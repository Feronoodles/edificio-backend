param(
    [string]$BaseUrl = "http://localhost",
    [string]$ComposeFile = "docker-compose.prod.yml",
    [switch]$SkipFrontendBuild
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$frontendPath = Join-Path $root "frontend"
$smokeScript = Join-Path $root "scripts\smoke-prod-local.ps1"

Push-Location $root
try {
    Write-Step "Ejecutando tests del backend"
    mvn test

    if (-not $SkipFrontendBuild -and (Test-Path $frontendPath)) {
        Write-Step "Compilando frontend"
        Push-Location $frontendPath
        try {
            npm.cmd run build
        } finally {
            Pop-Location
        }
    } elseif (-not $SkipFrontendBuild) {
        Write-Step "Frontend no encontrado"
        Write-Host "Se omite build frontend porque este repo puede ser solo backend."
    }

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
