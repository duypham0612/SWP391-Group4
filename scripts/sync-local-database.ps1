$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $projectRoot "src\main\resources\db.properties"
$migrationPath = Join-Path $projectRoot "sql\migration_local_schema_sync.sql"

if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw "Database properties were not found at $propertiesPath"
}
if (-not (Test-Path -LiteralPath $migrationPath)) {
    throw "Database migration was not found at $migrationPath"
}
if ($null -eq (Get-Command "SQLCMD.EXE" -ErrorAction SilentlyContinue)) {
    throw "sqlcmd is not installed or is not available on PATH."
}

$properties = @{}
foreach ($line in Get-Content -LiteralPath $propertiesPath) {
    if ($line -match '^\s*([^#!][^=]*)=(.*)$') {
        $properties[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$jdbcUrl = if ($env:CAFE_DB_URL) { $env:CAFE_DB_URL } else { $properties['db.url'] }
$username = if ($env:CAFE_DB_USERNAME) { $env:CAFE_DB_USERNAME } else { $properties['db.username'] }
$password = if ($env:CAFE_DB_PASSWORD) { $env:CAFE_DB_PASSWORD } else { $properties['db.password'] }

if ($jdbcUrl -notmatch '^jdbc:sqlserver://([^:;]+)(?::([0-9]+))?;.*databaseName=([^;]+)') {
    throw "Unsupported SQL Server JDBC URL in db.properties."
}

$server = $matches[1]
if ($matches[2]) {
    $server = "$server,$($matches[2])"
}
$database = $matches[3]

Write-Host "[vscode] Checking additive database schema for $database..."
& SQLCMD.EXE -S $server -d $database -U $username -P $password -C -b -V 16 -h -1 -W -i $migrationPath
if ($LASTEXITCODE -ne 0) {
    throw "Database schema sync failed with exit code $LASTEXITCODE."
}

Write-Host "[vscode] Database schema is ready."
