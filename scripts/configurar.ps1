$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '../.env'
if (Test-Path -LiteralPath $envPath) { throw '.env ya existe; no se sobrescribe.' }
function New-RandomSecret { [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48)) }
$dbSecret = New-RandomSecret
$jwtSecret = New-RandomSecret
$seedPassword = Read-Host 'Clave de los usuarios de prueba indicada en el examen'
@("DATABASE_PASSWORD=$dbSecret", "JWT_SECRET=$jwtSecret", 'SEED_ENABLED=true', "SEED_PASSWORD=$seedPassword") | Set-Content -LiteralPath $envPath -Encoding utf8
Write-Host '.env creado. Ejecuta docker compose up --build.'
