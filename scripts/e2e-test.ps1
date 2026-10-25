<#
  Teste ponta a ponta da SAGA, pela gateway:
    1. encomenda paga e enviada: cliente registado -> reservado -> pagamento aprovado -> PAGA e stock em baixo
       -> envio criado -> expedido -> ENVIADA com o codigo do envio;
    2. compensacao: pagamento recusado -> CANCELADA e stock libertado;
    3. stock insuficiente -> CANCELADA sem pagamento.
  Uso: .\scripts\e2e-test.ps1 [-Gateway http://localhost:9080]
#>
param([string]$Gateway = "http://localhost:9080")
$ErrorActionPreference = "Stop"
$api = "$Gateway/api"
$falhas = 0

function Verificar([string]$nome, [bool]$ok, [string]$detalhe) {
    if ($ok) { Write-Host "OK    $nome  [$detalhe]" -ForegroundColor Green }
    else { Write-Host "FALHA $nome  [$detalhe]" -ForegroundColor Red; $script:falhas++ }
}

function Pedido([string]$metodo, [string]$caminho, $corpo) {
    $p = @{ Method = $metodo; Uri = "$api$caminho"; ContentType = "application/json; charset=utf-8" }
    if ($null -ne $corpo) { $p.Body = [System.Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 5)) }
    return Invoke-RestMethod @p
}

function EsperarEstado([string]$id, [string[]]$estados) {
    for ($i = 0; $i -lt 40; $i++) {
        $e = (Pedido GET "/encomendas/$id" $null).estado
        if ($estados -contains $e) { return $e }
        Start-Sleep -Milliseconds 500
    }
    return $e
}

# Espera que um recurso criado por mensagem exista (o pagamento ou o envio de uma encomenda)
function EsperarRecurso([string]$caminho) {
    for ($i = 0; $i -lt 40; $i++) {
        try { return Pedido GET $caminho $null } catch { Start-Sleep -Milliseconds 500 }
    }
    return $null
}

function NovaEncomenda([string]$referencia, [int]$quantidade) {
    return Pedido POST "/encomendas" @{ clienteNome = "Teste E2E"; clienteEmail = "e2e@bikeshop.pt"; linhas = @(@{ referencia = $referencia; quantidade = $quantidade }) }
}

# Garante stock para o teste sem mexer nas reservas
$ref = "TRV-PAS"
$s = Pedido GET "/stock/$ref" $null
if ($s.livre -lt 4) { Pedido PUT "/stock/$ref" @{ quantidade = $s.reservada + 40 } | Out-Null }

# 1. Encomenda paga
$antes = Pedido GET "/stock/$ref" $null
$enc = NovaEncomenda $ref 2
Verificar "Encomenda criada PENDENTE" ($enc.estado -eq "PENDENTE") "$($enc.id) $($enc.total) EUR"
$estado = EsperarEstado $enc.id @("RESERVADA", "CANCELADA")
Verificar "Stock reservado" ($estado -eq "RESERVADA") $estado
$cli = Pedido GET "/clientes/email/e2e@bikeshop.pt" $null
Verificar "Cliente registado pela encomenda (Feign)" ($cli.id -eq $enc.clienteId) "cliente $($cli.id)"
$pag = EsperarRecurso "/pagamentos/encomenda/$($enc.id)"
Verificar "Pagamento PENDENTE criado por mensagem" ($pag.estado -eq "PENDENTE") "$($pag.valor) EUR"
Pedido POST "/pagamentos/encomenda/$($enc.id)/aprovar" $null | Out-Null
$estado = EsperarEstado $enc.id @("PAGA", "CANCELADA")
Verificar "Encomenda PAGA" ($estado -eq "PAGA") $estado
Start-Sleep -Seconds 1
$depois = Pedido GET "/stock/$ref" $null
Verificar "Stock em baixo (2 unidades saem do armazem)" ($depois.quantidade -eq $antes.quantidade - 2 -and $depois.reservada -eq $antes.reservada) "$($antes.quantidade) -> $($depois.quantidade)"
$envio = EsperarRecurso "/envios/encomenda/$($enc.id)"
Verificar "Envio EM_PREPARACAO criado por mensagem" ($envio.estado -eq "EM_PREPARACAO" -and $envio.artigos -eq 2) "$($envio.codigo), $($envio.artigos) artigos"
Pedido POST "/envios/encomenda/$($enc.id)/expedir" $null | Out-Null
$estado = EsperarEstado $enc.id @("ENVIADA")
$codigo = (Pedido GET "/encomendas/$($enc.id)" $null).codigoEnvio
Verificar "Encomenda ENVIADA com o codigo do envio" ($estado -eq "ENVIADA" -and $codigo -eq $envio.codigo) "$estado $codigo"

# 2. Compensacao: pagamento recusado
$enc = NovaEncomenda $ref 1
EsperarEstado $enc.id @("RESERVADA") | Out-Null
$reservado = (Pedido GET "/stock/$ref" $null).reservada
EsperarRecurso "/pagamentos/encomenda/$($enc.id)" | Out-Null
Pedido POST "/pagamentos/encomenda/$($enc.id)/recusar" $null | Out-Null
$estado = EsperarEstado $enc.id @("CANCELADA")
Verificar "Pagamento recusado: encomenda CANCELADA" ($estado -eq "CANCELADA") $estado
Start-Sleep -Seconds 1
$livre = (Pedido GET "/stock/$ref" $null).reservada
Verificar "Compensacao: reserva libertada" ($livre -eq $reservado - 1) "reservada $reservado -> $livre"

# 3. Stock insuficiente
$enc = NovaEncomenda "MOT-CX5" 99
$estado = EsperarEstado $enc.id @("CANCELADA", "RESERVADA")
$motivo = (Pedido GET "/encomendas/$($enc.id)" $null).motivo
Verificar "Sem stock: encomenda CANCELADA" ($estado -eq "CANCELADA") $motivo

Write-Host ""
if ($falhas -eq 0) { Write-Host "Todas as verificacoes OK" -ForegroundColor Green }
else { Write-Host "$falhas verificacao(oes) falharam" -ForegroundColor Red; exit 1 }
