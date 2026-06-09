param(
    [string]$BackupPath,
    [string]$ComposeFile = "docker-compose.server-nginx.yml",
    [string]$PostgresService = "postgres",
    [string]$MainDatabaseName = "edificio_app",
    [string]$RestoreDatabaseName = "edificio_app_restore_check",
    [string]$BackupDir = "backups"
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

function Assert-SafeDatabaseName {
    param(
        [string]$Name,
        [string]$MainName
    )

    if ($Name -eq $MainName) {
        throw "Por seguridad, este script no restaura sobre la base principal '$MainName'. Usa una base de prueba."
    }

    if ($Name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw "Nombre de base de datos invalido: '$Name'. Usa solo letras, numeros y guion bajo."
    }
}

function Invoke-Postgres {
    param(
        [string]$Sql,
        [string]$Database = "postgres"
    )

    docker compose -f $ComposeFile exec -T `
        -e "PGPASSWORD=$($script:EnvVars["DB_PASSWORD"])" `
        $PostgresService `
        psql `
        -v ON_ERROR_STOP=1 `
        -U $script:EnvVars["DB_USERNAME"] `
        -d $Database `
        -c $Sql

    if ($LASTEXITCODE -ne 0) {
        throw "psql fallo con codigo $LASTEXITCODE."
    }
}

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$script:EnvVars = Read-EnvFile (Join-Path $root ".env")

if (-not $script:EnvVars["DB_USERNAME"] -or -not $script:EnvVars["DB_PASSWORD"]) {
    throw "Faltan DB_USERNAME o DB_PASSWORD en .env."
}

Assert-SafeDatabaseName -Name $RestoreDatabaseName -MainName $MainDatabaseName

if (-not $BackupPath) {
    $backupRoot = Join-Path $root $BackupDir
    $latestBackup = Get-ChildItem -Path $backupRoot -Filter "*.sql" -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $latestBackup) {
        throw "No se encontraron backups .sql en $backupRoot."
    }

    $BackupPath = $latestBackup.FullName
}

if (-not [System.IO.Path]::IsPathRooted($BackupPath)) {
    $BackupPath = Join-Path $root $BackupPath
}

if (-not (Test-Path $BackupPath)) {
    throw "No se encontro el backup $BackupPath."
}

Write-Host "Restaurando backup de prueba:"
Write-Host $BackupPath
Write-Host "Base destino: $RestoreDatabaseName"

Invoke-Postgres -Sql "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$RestoreDatabaseName';"
Invoke-Postgres -Sql "DROP DATABASE IF EXISTS $RestoreDatabaseName;"
Invoke-Postgres -Sql "CREATE DATABASE $RestoreDatabaseName;"

Get-Content -Path $BackupPath -Raw | docker compose -f $ComposeFile exec -T `
    -e "PGPASSWORD=$($script:EnvVars["DB_PASSWORD"])" `
    $PostgresService `
    psql `
    -v ON_ERROR_STOP=1 `
    -U $script:EnvVars["DB_USERNAME"] `
    -d $RestoreDatabaseName

if ($LASTEXITCODE -ne 0) {
    throw "La restauracion fallo con codigo $LASTEXITCODE."
}

$tableCount = docker compose -f $ComposeFile exec -T `
    -e "PGPASSWORD=$($script:EnvVars["DB_PASSWORD"])" `
    $PostgresService `
    psql `
    -U $script:EnvVars["DB_USERNAME"] `
    -d $RestoreDatabaseName `
    -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"

if ($LASTEXITCODE -ne 0) {
    throw "No se pudo verificar la restauracion."
}

Write-Host "Restauracion de prueba finalizada correctamente."
Write-Host "Tablas restauradas: $($tableCount.Trim())"
Write-Host "Base de prueba: $RestoreDatabaseName"
