param(
    [ValidateSet("run", "stop")]
    [string]$Action = "run",
    [switch]$DebugServer,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$tomcatHome = "C:\apache-tomcat-10.1.55"
$javaHome = "C:\Program Files\Java\jdk-21.0.11"
$tomcatBase = Join-Path $projectRoot ".tomcat-run"
$appUrl = "http://localhost:8080/cafe-shop/"

if (-not (Test-Path -LiteralPath (Join-Path $tomcatHome "bin\catalina.bat"))) {
    throw "Tomcat was not found at $tomcatHome"
}
if (-not (Test-Path -LiteralPath (Join-Path $javaHome "bin\java.exe"))) {
    throw "JDK was not found at $javaHome"
}

$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatBase
$env:JAVA_HOME = $javaHome

if ($Action -eq "stop") {
    if (-not (Test-Path -LiteralPath (Join-Path $tomcatBase "conf\server.xml"))) {
        Write-Host "[vscode] Tomcat runtime has not been created yet."
        exit 0
    }
    Write-Host "[vscode] Stopping Tomcat..."
    & (Join-Path $tomcatHome "bin\catalina.bat") stop
    exit $LASTEXITCODE
}

$occupiedListeners = @()
foreach ($port in @(8080, 5005)) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -ne $listener) {
        $occupiedListeners += $listener
    }
}

if ($occupiedListeners.Count -gt 0) {
    foreach ($listener in $occupiedListeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
        if ($null -eq $process -or $process.CommandLine -notlike "*$tomcatBase*") {
            throw "Port $($listener.LocalPort) is being used by another application (PID $($listener.OwningProcess))."
        }
    }

    Write-Host "[vscode] Restarting the existing workspace Tomcat..."
    & (Join-Path $tomcatHome "bin\catalina.bat") stop
    if ($LASTEXITCODE -ne 0) {
        throw "The existing workspace Tomcat could not be stopped."
    }

    $shutdownDeadline = (Get-Date).AddSeconds(15)
    do {
        Start-Sleep -Milliseconds 300
        $stillListening = Get-NetTCPConnection -State Listen -LocalPort 8080, 5005 -ErrorAction SilentlyContinue
    } while ($stillListening -and (Get-Date) -lt $shutdownDeadline)

    if ($stillListening) {
        throw "The existing workspace Tomcat did not release its ports in time."
    }
}

$runtimeDirectories = @("conf", "logs", "temp", "webapps", "work")
foreach ($directory in $runtimeDirectories) {
    $path = Join-Path $tomcatBase $directory
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
    }
}

$sourceConf = Join-Path $tomcatHome "conf\*"
$targetConf = Join-Path $tomcatBase "conf"
Copy-Item -Path $sourceConf -Destination $targetConf -Recurse -Force

$sourceWar = Join-Path $projectRoot "target\cafe-shop.war"
if (-not (Test-Path -LiteralPath $sourceWar)) {
    throw "WAR file is missing. Run 'mvn package -DskipTests' first."
}
Copy-Item -LiteralPath $sourceWar -Destination (Join-Path $tomcatBase "webapps\cafe-shop.war") -Force

$baseOptions = "-Dfile.encoding=UTF-8 -Djava.awt.headless=true"
if ($DebugServer) {
    $env:CATALINA_OPTS = "$baseOptions -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:5005"
} else {
    $env:CATALINA_OPTS = $baseOptions
}

if (-not $NoBrowser) {
    $browserHelper = Join-Path $PSScriptRoot "wait-open-browser.ps1"
    Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "`"$browserHelper`"", "-Url", "`"$appUrl`"") `
        -WindowStyle Hidden | Out-Null
}

Write-Host "[vscode] Starting Tomcat at $appUrl"
& (Join-Path $tomcatHome "bin\catalina.bat") run
exit $LASTEXITCODE
