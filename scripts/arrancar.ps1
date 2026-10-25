<#
  Arranca o BikeShop todo em Docker:
    1. infraestrutura (grupo bikeshop-infra) e espera que fique healthy;
    2. compila os JAR se faltarem (ou sempre, com -Compilar);
    3. constroi as imagens e arranca os 6 servicos (grupo bikeshop-apps);
    4. espera que a gateway responda e mostra o endereco.

    .\scripts\arrancar.ps1
    .\scripts\arrancar.ps1 -Compilar
#>
param([switch]$Compilar)
$ErrorActionPreference = "Continue"   # o docker e o mvnw escrevem progresso no stderr
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz
$servicos = "bikeshop-registo", "bikeshop-gateway", "bikeshop-catalogo", "bikeshop-stock", "bikeshop-pagamentos", "bikeshop-encomendas", "bikeshop-clientes", "bikeshop-envios"

cmd /c "docker info >nul 2>&1"
if ($LASTEXITCODE -ne 0) { Write-Host "O Docker nao responde. Abrir o Docker Desktop e esperar por 'Engine running'." -ForegroundColor Red; exit 1 }

Write-Host "== 1/4 Infraestrutura (bikeshop-infra)" -ForegroundColor Cyan
cmd /c "docker compose up -d --wait 2>&1" | Select-Object -Last 1
if ($LASTEXITCODE -ne 0) { Write-Host "A infraestrutura nao ficou healthy. Ver: docker compose ps" -ForegroundColor Red; exit 1 }

$faltam = $servicos | Where-Object { -not (Test-Path "$_\target\$_.jar") }
if ($Compilar -or $faltam) {
    Write-Host "== 2/4 Compilar os JAR" -ForegroundColor Cyan
    & .\mvnw.cmd -q -B package -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Host "Falhou a compilacao" -ForegroundColor Red; exit 1 }
} else {
    Write-Host "== 2/4 JAR ja compilados (usar -Compilar para recompilar)" -ForegroundColor Cyan
}

Write-Host "== 3/4 Imagens e servicos (bikeshop-apps)" -ForegroundColor Cyan
cmd /c "docker compose -f docker-compose.apps.yml up -d --build 2>&1" | Select-Object -Last 6

Write-Host "== 4/4 A esperar pelos servicos (1 a 3 minutos)" -ForegroundColor Cyan
$caminhos = "/api/pecas/landing", "/api/stock/landing", "/api/pagamentos/landing", "/api/encomendas/landing", "/api/clientes/landing", "/api/envios/landing"
$fim = (Get-Date).AddMinutes(5)
do {
    Start-Sleep -Seconds 5
    $ok = 0
    foreach ($c in $caminhos) {
        try { if ((Invoke-WebRequest "http://localhost:9080$c" -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200) { $ok++ } } catch { }
    }
    Write-Host ("  {0,3} s: {1}/{2} servicos de negocio a responder pela gateway" -f [int]((Get-Date) - $fim.AddMinutes(-5)).TotalSeconds, $ok, $caminhos.Count)
} until ($ok -eq $caminhos.Count -or (Get-Date) -gt $fim)

if ($ok -ne $caminhos.Count) { Write-Host "Nem todos responderam. Ver: docker compose -f docker-compose.apps.yml logs" -ForegroundColor Red; exit 1 }

# A encomenda chama o catalogo e os clientes pelo Eureka: espera que os 7 servicos estejam registados
# e que passe uma atualizacao da lista (5 s) para nao responder 503 no primeiro pedido.
do {
    $apps = 0
    try { $apps = @((Invoke-RestMethod "http://localhost:9761/eureka/apps" -Headers @{ Accept = "application/json" } -TimeoutSec 3).applications.application).Count } catch { }
    if ($apps -lt 7) { Start-Sleep -Seconds 3 }
} until ($apps -ge 7 -or (Get-Date) -gt $fim)
Write-Host "  $apps servicos registados no Eureka"
Start-Sleep -Seconds 10
Write-Host ""
Write-Host "BikeShop pronto: http://localhost:9080/   Teste: .\scripts\e2e-test.ps1" -ForegroundColor Green
