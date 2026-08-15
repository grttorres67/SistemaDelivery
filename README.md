# Sistema Delivery em Kotlin

Este projeto implementa uma solução de delivery com interface de linha de comando (CLI) em Kotlin, composta por:

- Aplicativo de restaurante
- Aplicativo de cliente

Os dados são persistidos em arquivos locais em JSON e CSV na pasta `data`.

## Estrutura

- `src/common/DeliveryCore.kt`: modelos, serialização JSON, manipulação de arquivos, utilitários gerais
- `src/restaurante/RestauranteApp.kt`: fluxo do restaurante
- `src/cliente/ClienteApp.kt`: fluxo do cliente

## Compilar

```bash
./build.sh
```

## Executar

```bash
java -jar build/restaurante.jar
java -jar build/cliente.jar
```

## Arquivos gerados

- `data/restaurante_1.json`
- `data/restaurante_2.json`
- `data/clientes.json`
- `data/pedidos.csv`
