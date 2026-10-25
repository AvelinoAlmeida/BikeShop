<#
  Coloca o BikeShop no Minikube (slide 06), num perfil proprio: bikeshop-minikube.
  A infraestrutura continua no Docker Compose (grupo bikeshop-infra); os pods chegam-lhe por host.minikube.internal.

    .\scripts\k8s-deploy.ps1             # compila, constroi as imagens, carrega-as e aplica k8s/
    .\scripts\k8s-deploy.ps1 -SemBuild   # so aplica os manifestos (imagens ja carregadas)

  Interface: kubectl port-forward -n bikeshop svc/bikeshop-gateway 19080:80   e abrir http://localhost:19080/
#>
param([switch]$SemBuild)
$ErrorActionPreference = "Continue"
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz
$perfil = "bikeshop-minikube"
$servicos = "bikeshop-registo", "bikeshop-gateway", "bikeshop-catalogo", "bikeshop-stock", "bikeshop-pagamentos", "bikeshop-encomendas", "bikeshop-clientes", "bikeshop-envios"

Write-Host "== 1/4 Infraestrutura (bikeshop-infra) e cluster ($perfil)" -ForegroundColor Cyan
cmd /c "docker compose up -d --wait 2>&1" | Out-Null
cmd /c "minikube -p $perfil status >NUL 2>&1"
if ($LASTEXITCODE -ne 0) { cmd /c "minikube start -p $perfil --driver=docker --memory=4096 --cpus=4 2>&1" | Select-Object -Last 2 }
kubectl config use-context $perfil | Out-Null

if (-not $SemBuild) {
    Write-Host "== 2/4 JAR e imagens <servico>:1.0" -ForegroundColor Cyan
    & .\mvnw.cmd -q -B package -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Host "Falhou a compilacao" -ForegroundColor Red; exit 1 }
    foreach ($s in $servicos) {
        cmd /c "docker build -q -f $s/Dockerfile -t ${s}:1.0 . 2>&1" | Out-Null
        Write-Host "  ${s}:1.0 construida; a carregar no cluster..."
        cmd /c "minikube -p $perfil image load ${s}:1.0 2>&1" | Out-Null
    }
}

Write-Host "== 3/4 kubectl apply -f k8s/" -ForegroundColor Cyan
kubectl apply -f k8s/
# As imagens novas tem a mesma etiqueta (1.0): sem reiniciar, os pods que ja existiam ficavam com as antigas.
if (-not $SemBuild) { kubectl rollout restart deployment -n bikeshop }

Write-Host "== 4/4 Rollout (o registo primeiro; o Spring demora)" -ForegroundColor Cyan
foreach ($s in $servicos) { kubectl rollout status deployment/$s -n bikeshop --timeout=600s }
kubectl get pods -n bikeshop
Write-Host ""
Write-Host "Interface: kubectl port-forward -n bikeshop svc/bikeshop-gateway 19080:80  e abrir http://localhost:19080/" -ForegroundColor Green
