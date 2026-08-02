<#
.SYNOPSIS
    Cập nhật checksum SHA-256 chuẩn hóa LF cho migration database duy nhất.

.DESCRIPTION
    Chuẩn hóa CRLF/CR thành LF trước khi băm, nên manifest giống nhau trên
    Windows, Linux và macOS. Chỉ chạy sau khi thay đổi schema có chủ đích.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$relativeMigration = 'src/main/resources/db/migration/V1__database.sql'
$migration = Join-Path $repoRoot ($relativeMigration -replace '/', [IO.Path]::DirectorySeparatorChar)
$manifest = Join-Path $repoRoot 'sql/migration-checksums.sha256'

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$sql = [IO.File]::ReadAllText($migration, $utf8NoBom)
$normalized = $sql.Replace("`r`n", "`n").Replace("`r", "`n")
$bytes = $utf8NoBom.GetBytes($normalized)
$sha = [Security.Cryptography.SHA256]::Create()
try {
    $hash = ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
} finally {
    $sha.Dispose()
}

$entry = "$hash  $relativeMigration"
[IO.File]::WriteAllText($manifest, "$entry`n", $utf8NoBom)
Write-Host "Da cap nhat $manifest"
Write-Host $entry
