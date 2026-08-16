# Sistema Delivery em Kotlin

Solução completa de delivery com interface de linha de comando (CLI), permitindo que restaurantes gerenciem cardápios e pedidos, enquanto clientes podem fazer e acompanhar pedidos.

## 📋 Requisitos

- **Java 21+** (ou qualquer versão recente)
- **Kotlin 2.3+** (compilador instalado)

Verifique com:
```bash
java -version
kotlinc -version
```

## 🚀 Como usar

### 1. Compilar o projeto

```bash
./build.sh
```

Isso gera dois arquivos JAR em `build/`:
- `restaurante.jar`
- `cliente.jar`

### 2. Executar os aplicativos

**Terminal 1 - App do Restaurante:**
```bash
java -jar build/restaurante.jar
```

**Terminal 2 - App do Cliente:**
```bash
java -jar build/cliente.jar
```

## 📁 Estrutura do Projeto

```
src/
├── common/
│   └── DeliveryCore.kt        # Modelos, JSON parsing, persistência
├── cliente/
│   └── ClienteApp.kt          # Aplicativo do cliente
└── restaurante/
    └── RestauranteApp.kt      # Aplicativo do restaurante

tests/
└── cli_e2e.ps1                # Testes end-to-end automatizados

build.sh                        # Script de compilação
```

## 💾 Dados Persistidos

Os dados são armazenados localmente na pasta `data/`:

- **restaurante_ID.json** — Cadastro, email único e cardápio de cada restaurante
- **clientes.json** — Dados dos clientes e telefone único
- **pedidos.csv** — Registro compartilhado de pedidos com filtros por email e telefone

## 🎮 Funcionalidades

### Restaurante
- ✅ Cadastro e login com email
- ✅ Gerenciar cardápio (ver, adicionar, remover itens)
- ✅ Visualizar pedidos por status
- ✅ Atualizar status do pedido (0 a 4)

### Cliente
- ✅ Cadastro e login com telefone
- ✅ Realizar novos pedidos
- ✅ Ver pedidos em andamento
- ✅ Ver pedidos finalizados

## 📊 Status dos Pedidos

- `0` — SOLICITADO
- `1` — EM PREPARAÇÃO
- `2` — AGUARDANDO ENTREGADOR
- `3` — EM TRÂNSITO
- `4` — ENTREGUE
