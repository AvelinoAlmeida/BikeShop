<#
  Publica as imagens do BikeShop no Docker Hub (slide 06: docker tag e docker push), com versao.
  As imagens locais bikeshop-<servico>:1.0 sao construidas pelo arrancar.ps1 ou pelo docker compose.

    docker login                                           # uma vez, com a conta do Docker Hub
    .\scripts\publicar-dockerhub.ps1 -Utilizador conta     # publica conta/bikeshop-<servico>:1.0
    .\scripts\publicar-dockerhub.ps1 -Utilizador conta -Versao 1.1

  Depois de publicar, os manifestos em k8s/ podem usar image: conta/bikeshop-<servico>:1.0
  com imagePullPolicy: IfNotPresent, em vez de carregar as imagens com minikube image load.
#>
param(
    [Parameter(Mandatory = $true)][string]$Utilizador,
    [string]$Versao = "1.0"
)
$ErrorActionPreference = "Continue"
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz
$servicos = "bikeshop-registo", "bikeshop-gateway", "bikeshop-catalogo", "bikeshop-stock", "bikeshop-pagamentos", "bikeshop-encomendas"

cmd /c "docker info >nul 2>&1"
if ($LASTEXITCODE -ne 0) { Write-Host "O Docker nao responde. Abrir o Docker Desktop." -ForegroundColor Red; exit 1 }

# O docker login guarda as credenciais; sem elas o push falha com "denied".
$conta = (docker info --format "{{.Username}}" 2>$null)
if (-not $conta) { Write-Host "Aviso: nao foi possivel confirmar o login. Se o push falhar, correr: docker login" -ForegroundColor Yellow }

$falhas = 0
foreach ($s in $servicos) {
    $local = "${s}:1.0"
    $remota = "$Utilizador/${s}:$Versao"
    cmd /c "docker image inspect $local >nul 2>&1"
    if ($LASTEXITCODE -ne 0) { Write-Host "FALTA $local (correr antes .\scripts\arrancar.ps1)" -ForegroundColor Red; $falhas++; continue }
    docker tag $local $remota
    cmd /c "docker push $remota 2>&1" | Select-Object -Last 1
    if ($LASTEXITCODE -eq 0) { Write-Host "OK    $remota" -ForegroundColor Green } else { Write-Host "FALHA $remota" -ForegroundColor Red; $falhas++ }
}

if ($falhas) { Write-Host "$falhas imagem(ns) por publicar" -ForegroundColor Red; exit 1 }
Write-Host "Publicadas em https://hub.docker.com/u/$Utilizador" -ForegroundColor Green
