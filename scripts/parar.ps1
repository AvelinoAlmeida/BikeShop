<#
  Para o BikeShop: primeiro os servicos (bikeshop-apps), depois a infraestrutura (bikeshop-infra).
    .\scripts\parar.ps1               # mantem os dados
    .\scripts\parar.ps1 -ApagarDados  # apaga tambem as bases e as filas (voltam os dados de exemplo)
#>
param([switch]$ApagarDados)
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz

cmd /c "docker compose -f docker-compose.apps.yml down 2>&1" | Select-Object -Last 1
if ($ApagarDados) { cmd /c "docker compose down -v 2>&1" | Select-Object -Last 1 }
else { cmd /c "docker compose down 2>&1" | Select-Object -Last 1 }
Write-Host "BikeShop parado." -ForegroundColor Green
