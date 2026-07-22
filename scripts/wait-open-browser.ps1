param(
    [Parameter(Mandatory = $true)]
    [string]$Url
)

$ErrorActionPreference = "SilentlyContinue"
$deadline = (Get-Date).AddSeconds(60)

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
            Start-Process $Url
            exit 0
        }
    } catch {
        Start-Sleep -Milliseconds 750
    }
}

exit 1
