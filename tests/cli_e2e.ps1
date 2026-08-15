$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$workDir = Join-Path $root 'test-run'
$restauranteJar = Join-Path $root 'build\restaurante.jar'
$clienteJar = Join-Path $root 'build\cliente.jar'

if (-not (Test-Path $restauranteJar)) { throw "Arquivo não encontrado: $restauranteJar" }
if (-not (Test-Path $clienteJar)) { throw "Arquivo não encontrado: $clienteJar" }

if (Test-Path $workDir) {
    Remove-Item $workDir -Recurse -Force
}
New-Item -ItemType Directory -Path $workDir | Out-Null

function Invoke-AppWithInput {
    param(
        [string]$JarPath,
        [string[]]$InputLines
    )

    Push-Location $workDir
    try {
        $output = $InputLines | java -jar $JarPath 2>&1
        return $output
    }
    finally {
        Pop-Location
    }
}

Write-Host '== Cadastro do restaurante ==' 
$restaurantInput = @(
    '2',
    'Pizzaria do Bairro',
    'contato@pizzariadobairro.com',
    'Rua das Flores, 123',
    '1',
    'Pizza Calabresa',
    '45.00',
    '2',
    'Refrigerante 2L',
    '10.00',
    '',
    '0'
)
$restaurantOutput = Invoke-AppWithInput -JarPath $restauranteJar -InputLines $restaurantInput
$restaurantOutput | Out-String | Write-Host

$restaurantJson = Join-Path $workDir 'data\restaurante_1.json'
if (-not (Test-Path $restaurantJson)) { throw 'Arquivo do restaurante não foi gerado.' }

Write-Host '== Cadastro do cliente ==' 
$clientInput = @(
    '2',
    'Joao Silva',
    '62999998888',
    'Av. Central, 500',
    '0'
)
$clientOutput = Invoke-AppWithInput -JarPath $clienteJar -InputLines $clientInput
$clientOutput | Out-String | Write-Host

$clientesJson = Join-Path $workDir 'data\clientes.json'
if (-not (Test-Path $clientesJson)) { throw 'Arquivo de clientes não foi gerado.' }

Write-Host '== Realização do pedido ==' 
$pedidoInput = @(
    '1',
    '62999998888',
    '1',
    '1',
    '1',
    '2',
    '2',
    '1',
    '',
    'S',
    '0',
    '0'
)
$pedidoOutput = Invoke-AppWithInput -JarPath $clienteJar -InputLines $pedidoInput
$pedidoOutput | Out-String | Write-Host

$pedidosCsv = Join-Path $workDir 'data\pedidos.csv'
if (-not (Test-Path $pedidosCsv)) { throw 'Arquivo de pedidos não foi gerado.' }

$pedidoLines = Get-Content $pedidosCsv
if ($pedidoLines.Count -lt 2) { throw 'Não houve linhas de pedido no CSV.' }
$lastPedido = ($pedidoLines | Select-Object -Last 1).Split(';')
if ($lastPedido[0] -notmatch '^PED-') { throw "ID de pedido inválido: $($lastPedido[0])" }
if ($lastPedido[12] -ne '0') { throw "Status inicial do pedido deveria ser 0. Valor encontrado: $($lastPedido[12])" }

$pedidoId = $lastPedido[0]
Write-Host "Pedido gerado: $pedidoId"

Write-Host '== Atualização do status pelo restaurante ==' 
$statusInput = @(
    '1',
    'contato@pizzariadobairro.com',
    '3',
    $pedidoId,
    '4',
    '0'
)
$statusOutput = Invoke-AppWithInput -JarPath $restauranteJar -InputLines $statusInput
$statusOutput | Out-String | Write-Host

$updatedLines = Get-Content $pedidosCsv
$updatedStatus = ($updatedLines | Select-Object -Last 1).Split(';')[12]
if ($updatedStatus -ne '4') { throw "Status final esperado 4, mas ficou em $updatedStatus" }

Write-Host '== Validação do cliente com pedidos finalizados ==' 
$finalizadosInput = @(
    '1',
    '62999998888',
    '3',
    '0'
)
$finalizadosOutput = Invoke-AppWithInput -JarPath $clienteJar -InputLines $finalizadosInput
$finalizadosOutput | Out-String | Write-Host

if ($finalizadosOutput -notmatch 'Pedido') {
    throw 'Cliente não exibiu pedido finalizado.'
}

Write-Host '== TESTE E2E CONCLUÍDO COM SUCESSO =='
