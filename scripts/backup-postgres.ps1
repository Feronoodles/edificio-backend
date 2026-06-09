param(
    [string]$ComposeFile = "docker-compose.server-nginx.yml",
    [string]$PostgresService = "postgres",
    [string]$DatabaseName = "edificio_app",
    [string]$OutputDir = "backups"
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

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envVars = Read-EnvFile (Join-Path $root ".env")
$backupDir = Join-Path $root $OutputDir

if (-not $envVars["DB_USERNAME"] -or -not $envVars["DB_PASSWORD"]) {
    throw "Faltan DB_USERNAME o DB_PASSWORD en .env."
}

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupPath = Join-Path $backupDir "$DatabaseName-$timestamp.sql"

Write-Host "Creando backup de $DatabaseName en $backupPath"

$dump = docker compose -f $ComposeFile exec -T `
    -e "PGPASSWORD=$($envVars["DB_PASSWORD"])" `
    $PostgresService `
    pg_dump `
    -U $envVars["DB_USERNAME"] `
    -d $DatabaseName `
    --clean `
    --if-exists `
    --no-owner `
    --no-privileges

if ($LASTEXITCODE -ne 0) {
    throw "pg_dump fallo con codigo $LASTEXITCODE."
}

$dump | Set-Content -Path $backupPath -Encoding UTF8

Write-Host "Backup creado correctamente."
Write-Host $backupPath
