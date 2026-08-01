<#
.SYNOPSIS
    Dựng database CafeChain từ đầu bằng Flyway (schema 25 bảng).

.DESCRIPTION
    Schema hiện tại KHÔNG nâng cấp được từ database cũ: số bảng và quan hệ đã đổi
    hoàn toàn (49 -> 25 bảng). Vì vậy script luôn tạo database MỚI rồi migrate,
    không cố sửa database đang có.

    Các bước script thực hiện:
      1. Kiểm tra kết nối SQL Server.
      2. Tạo database mới (hoặc drop rồi tạo lại nếu truyền -Force).
      3. Tạo SQL login + user cho ứng dụng, cấp quyền db_owner trên database đó.
      4. Chạy Flyway migrate (tạo bảng, index, FK, CHECK, trigger và seed demo).
      5. In lại biến môi trường cần set cho Tomcat.

.PARAMETER ServerInstance
    Instance SQL Server. Ví dụ: 'localhost\SQLEXPRESS' hoặc 'localhost'.

.PARAMETER DatabaseName
    Tên database sẽ tạo. Mặc định CafeChain_v2 để không đụng database cũ.

.PARAMETER AppLogin
    SQL login cho ứng dụng. Script tạo nếu chưa có.

.PARAMETER AppPassword
    Mật khẩu cho SQL login.

.PARAMETER Force
    Drop database nếu đã tồn tại. MẤT TOÀN BỘ DỮ LIỆU trong database đó.

.EXAMPLE
    .\tools\setup-database.ps1 -ServerInstance 'localhost\HOANGANH'

.EXAMPLE
    .\tools\setup-database.ps1 -ServerInstance 'localhost\HOANGANH' -DatabaseName CafeChain_v2 -Force
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ServerInstance,

    [string]$DatabaseName = 'CafeChain_v2',
    [string]$AppLogin     = 'cafechain_app',
    [string]$AppPassword  = 'CafeChain@2026Dev',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Sql {
    param([string]$Database, [string]$Query)
    $cs = "Server=$ServerInstance;Database=$Database;Integrated Security=true;TrustServerCertificate=true"
    $conn = New-Object System.Data.SqlClient.SqlConnection $cs
    $conn.Open()
    try {
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = $Query
        $cmd.CommandTimeout = 300
        $reader = $cmd.ExecuteReader()
        $rows = @()
        while ($reader.Read()) {
            $row = [ordered]@{}
            for ($i = 0; $i -lt $reader.FieldCount; $i++) { $row[$reader.GetName($i)] = $reader.GetValue($i) }
            $rows += [pscustomobject]$row
        }
        $reader.Close()
        return $rows
    } finally { $conn.Close() }
}

Write-Host "==> Kiem tra ket noi $ServerInstance" -ForegroundColor Cyan
$ver = Invoke-Sql -Database 'master' -Query "SELECT @@VERSION AS V"
Write-Host ("    " + ($ver[0].V -split "`n")[0].Trim())

$exists = Invoke-Sql -Database 'master' -Query "SELECT DB_ID(N'$DatabaseName') AS Id"
if ($null -ne $exists[0].Id -and $exists[0].Id -isnot [DBNull]) {
    if (-not $Force) {
        throw "Database [$DatabaseName] da ton tai. Dung -Force de drop va tao lai (MAT TOAN BO DU LIEU), hoac chon -DatabaseName khac."
    }
    Write-Host "==> Drop database [$DatabaseName] (da truyen -Force)" -ForegroundColor Yellow
    Invoke-Sql -Database 'master' -Query "ALTER DATABASE [$DatabaseName] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [$DatabaseName];" | Out-Null
}

Write-Host "==> Tao database [$DatabaseName]" -ForegroundColor Cyan
Invoke-Sql -Database 'master' -Query "CREATE DATABASE [$DatabaseName];" | Out-Null
Invoke-Sql -Database 'master' -Query "ALTER DATABASE [$DatabaseName] SET MULTI_USER;" | Out-Null

Write-Host "==> Tao login/user [$AppLogin]" -ForegroundColor Cyan
Invoke-Sql -Database 'master' -Query @"
IF NOT EXISTS(SELECT 1 FROM sys.server_principals WHERE name = N'$AppLogin')
    CREATE LOGIN [$AppLogin] WITH PASSWORD = '$AppPassword', CHECK_POLICY = OFF;
"@ | Out-Null
Invoke-Sql -Database $DatabaseName -Query @"
IF NOT EXISTS(SELECT 1 FROM sys.database_principals WHERE name = N'$AppLogin')
    CREATE USER [$AppLogin] FOR LOGIN [$AppLogin];
ALTER ROLE db_owner ADD MEMBER [$AppLogin];
"@ | Out-Null

# Named instance khong phai luc nao cung resolve duoc qua JDBC, nen uu tien port TCP that.
$ports = Invoke-Sql -Database 'master' -Query "SELECT DISTINCT port FROM sys.dm_tcp_listener_states WHERE type = 0 AND state = 0"
$port = 1433
if ($ports.Count -gt 0 -and ($ports | Where-Object { $_.port -eq 1433 })) { $port = 1433 }
elseif ($ports.Count -gt 0) { $port = $ports[0].port }
$host_ = ($ServerInstance -split '\\')[0]
if ([string]::IsNullOrWhiteSpace($host_)) { $host_ = 'localhost' }
$jdbcUrl = "jdbc:sqlserver://${host_}:${port};databaseName=$DatabaseName;encrypt=true;trustServerCertificate=true"

Write-Host "==> Chay Flyway migrate" -ForegroundColor Cyan
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -ne $mvn) { $mvnCmd = $mvn.Source }
else {
    $bundled = Get-ChildItem 'C:\Program Files\JetBrains' -Filter 'mvn.cmd' -Recurse -ErrorAction SilentlyContinue |
               Where-Object { $_.FullName -like '*maven*bin*' } | Select-Object -First 1
    if ($null -eq $bundled) { throw "Khong tim thay Maven. Cai Maven hoac chay lenh flyway:migrate thu cong (xem docs/DATABASE-SETUP.md)." }
    $mvnCmd = $bundled.FullName
}

Push-Location $repoRoot
try {
    & $mvnCmd -q -Pdb-migrate flyway:migrate `
        "-Dflyway.url=$jdbcUrl" `
        "-Dflyway.user=$AppLogin" `
        "-Dflyway.password=$AppPassword"
    if ($LASTEXITCODE -ne 0) { throw "Flyway migrate that bai (exit $LASTEXITCODE)." }
} finally { Pop-Location }

Write-Host "==> Kiem tra ket qua" -ForegroundColor Cyan
$check = Invoke-Sql -Database $DatabaseName -Query @"
SELECT
  (SELECT COUNT(*) FROM sys.tables WHERE name <> 'flyway_schema_history') AS BangNghiepVu,
  (SELECT COUNT(*) FROM sys.triggers WHERE is_ms_shipped = 0)             AS Trigger_,
  (SELECT COUNT(*) FROM sys.foreign_keys)                                 AS FK,
  (SELECT COUNT(*) FROM iam.UserAccount)                                  AS TaiKhoan,
  (SELECT TOP 1 version FROM ops.flyway_schema_history
    WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank DESC) AS FlywayVersion
"@
$c = $check[0]
Write-Host ("    Bang nghiep vu : {0} (ky vong 25)" -f $c.BangNghiepVu)
Write-Host ("    Trigger        : {0}" -f $c.Trigger_)
Write-Host ("    Foreign key    : {0}" -f $c.FK)
Write-Host ("    Tai khoan demo : {0} (ky vong 4)" -f $c.TaiKhoan)
Write-Host ("    Flyway version : {0}" -f $c.FlywayVersion)

if ($c.BangNghiepVu -ne 25) { throw "So bang khong dung: $($c.BangNghiepVu), ky vong 25." }

Write-Host ""
Write-Host "HOAN TAT. Set bien moi truong cho Tomcat:" -ForegroundColor Green
Write-Host "    DB_URL=$jdbcUrl"
Write-Host "    DB_USERNAME=$AppLogin"
Write-Host "    DB_PASSWORD=$AppPassword"
Write-Host ""
Write-Host "Tai khoan demo (mat khau 123456): admin, manager1, cashier1, barista1"
