param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ComposeFile = "docker-compose.prod.yml"
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$smokeScript = Join-Path $root "scripts\smoke-prod-local.ps1"

Push-Location $root
try {
    Write-Step "Ejecutando tests del backend"
    mvn test

    Write-Step "Validando docker compose"
    docker compose -f $ComposeFile config | Out-Null
    Write-Host "OK docker compose config"

    Write-Step "Construyendo y levantando stack local de produccion"
    docker compose -f $ComposeFile up --build -d

    Write-Step "Ejecutando smoke test"
    & $smokeScript -BaseUrl $BaseUrl -ComposeFile $ComposeFile

    Write-Step "Verificacion completa finalizada"
    Write-Host "Todo se ve bien para produccion local."
} finally {
    Pop-Location
}
