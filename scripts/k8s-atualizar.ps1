<#
  Poe em producao no Minikube uma nova versao de um servico, a partir do Docker Hub (exercicio do slide 06),
  e acompanha a recriacao dos pods. Os pods novos so substituem os antigos quando estao prontos (rolling update).

    .\scripts\publicar-dockerhub.ps1 -Utilizador conta -Versao 2.0        # primeiro, publicar a versao
    .\scripts\k8s-atualizar.ps1 -Utilizador conta -Servico catalogo -Versao 2.0
    kubectl rollout undo deployment/bikeshop-catalogo -n bikeshop          # voltar a versao anterior

  O k8s-deploy.ps1 volta a por as imagens locais (1.0) dos manifestos em k8s/.
#>
param(
    [Parameter(Mandatory = $true)][string]$Utilizador,
    [Parameter(Mandatory = $true)][ValidateSet("registo", "gateway", "catalogo", "stock", "pagamentos", "encomendas", "clientes", "envios")][string]$Servico,
    [string]$Versao = "2.0"
)
$ErrorActionPreference = "Continue"
$nome = "bikeshop-$Servico"
$imagem = "$Utilizador/${nome}:$Versao"
kubectl config use-context bikeshop-minikube | Out-Null

Write-Host "== 1/3 Antes" -ForegroundColor Cyan
kubectl get pods -n bikeshop -l app=$nome -o wide

# A imagem e a pull policy mudam juntas: os manifestos usam Never (imagens carregadas no cluster),
# e uma imagem do Docker Hub tem de poder ser descarregada.
Write-Host "== 2/3 $nome -> $imagem" -ForegroundColor Cyan
$patch = '[{"op":"replace","path":"/spec/template/spec/containers/0/image","value":"' + $imagem + '"},' +
         '{"op":"replace","path":"/spec/template/spec/containers/0/imagePullPolicy","value":"IfNotPresent"}]'
$patch | Set-Content -Encoding ascii "$env:TEMP\bikeshop-patch.json"
kubectl patch deployment $nome -n bikeshop --type=json --patch-file "$env:TEMP\bikeshop-patch.json"
kubectl annotate deployment $nome -n bikeshop kubernetes.io/change-cause="$imagem" --overwrite | Out-Null
kubectl rollout status deployment/$nome -n bikeshop --timeout=600s
if ($LASTEXITCODE -ne 0) { Write-Host "O rollout nao terminou: kubectl describe pod -n bikeshop -l app=$nome" -ForegroundColor Red; exit 1 }

Write-Host "== 3/3 Depois" -ForegroundColor Cyan
kubectl get pods -n bikeshop -l app=$nome -o wide
kubectl rollout history deployment/$nome -n bikeshop
Write-Host "$nome esta na versao $Versao ($imagem)" -ForegroundColor Green
