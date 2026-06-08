param(
    [string]$BaseUrl = "http://localhost",
    [string]$ComposeFile = "docker-compose.prod.yml"
)

$ErrorActionPreference = "Stop"

function Read-EnvFile {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "No se encontro el archivo $Path. Crea un .env basado en .env.example."
    }

    $values = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $name, $value = $line -split "=", 2
        $values[$name] = $value
    }

    return $values
}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Assert-StatusCode {
    param(
        [string]$Uri,
        [int]$ExpectedStatusCode
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing
        $actualStatusCode = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $actualStatusCode = [int]$_.Exception.Response.StatusCode
        } else {
            throw
        }
    }

    if ($actualStatusCode -ne $ExpectedStatusCode) {
        throw "Se esperaba HTTP $ExpectedStatusCode en $Uri, pero se recibio HTTP $actualStatusCode."
    }

    Write-Host "OK $Uri respondio HTTP $ExpectedStatusCode"
}

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envPath = Join-Path $root ".env"
$envVars = Read-EnvFile $envPath

Push-Location $root
try {
    Write-Step "Revisando contenedores"
    docker compose -f $ComposeFile ps

    Write-Step "Revisando healthcheck interno del backend"
    $health = docker exec edificio_app-backend-1 curl -fsS http://localhost:8080/actuator/health
    if ($health -notmatch '"status"\s*:\s*"UP"') {
        throw "El backend no reporto status UP. Respuesta: $health"
    }
    Write-Host "OK backend health: $health"

    Write-Step "Revisando frontend publico"
    Assert-StatusCode -Uri "$BaseUrl/" -ExpectedStatusCode 200

    Write-Step "Revisando proxy API sin token"
    Assert-StatusCode -Uri "$BaseUrl/api/buildings" -ExpectedStatusCode 401

    Write-Step "Probando login y endpoint protegido"
    $loginBody = @{
        username = $envVars["APP_ADMIN_USERNAME"]
        password = $envVars["APP_ADMIN_PASSWORD"]
    } | ConvertTo-Json

    $login = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
    if (-not $login.accessToken -or -not $login.refreshToken) {
        throw "El login no devolvio accessToken y refreshToken."
    }

    $headers = @{ Authorization = "Bearer $($login.accessToken)" }
    $buildings = Invoke-RestMethod -Uri "$BaseUrl/api/buildings" -Headers $headers
    Write-Host "OK login valido. BuildingsCount=$(@($buildings).Count)"

    Write-Step "Smoke test finalizado"
    Write-Host "Todo se ve bien para el stack local de produccion."
} finally {
    Pop-Location
}
