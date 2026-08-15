package delivery

fun main() {
    ensureDataFiles()

    while (true) {
        println("\n=== APP CLIENTE ===")
        println("[1] Entrar")
        println("[2] Novo Cadastro")
        println("[0] Sair")

        when (ask("Escolha uma opção: ")) {
            "1" -> loginCliente()
            "2" -> novoCadastroCliente()
            "0" -> {
                println("Encerrando o app cliente...")
                return
            }
            else -> println("Opção inválida.")
        }
    }
}

private fun loginCliente() {
    val telefone = ask("Digite o telefone do cliente: ")
    if (telefone.isBlank()) {
        println("Telefone obrigatório.")
        return
    }

    val cliente = DataFiles.loadClients().firstOrNull { it.telefone == telefone }
    if (cliente == null) {
        println("Cliente não encontrado.")
        return
    }

    menuCliente(cliente)
}

private fun novoCadastroCliente() {
    val nome = ask("Nome: ")
    val telefone = ask("Telefone: ")
    val endereco = ask("Endereço: ")

    if (nome.isBlank() || telefone.isBlank() || endereco.isBlank()) {
        println("Todos os campos são obrigatórios.")
        return
    }

    if (DataFiles.loadClients().any { it.telefone == telefone }) {
        println("Telefone já cadastrado.")
        return
    }

    val cliente = Client(nome, telefone, endereco)
    val clientes = DataFiles.loadClients().toMutableList()
    clientes.add(cliente)
    DataFiles.saveClients(clientes)
    println("Cliente cadastrado com sucesso.")
}

private fun menuCliente(cliente: Client) {
    while (true) {
        println("\n=== MENU CLIENTE ===")
        println("[1] Realizar Novo Pedido")
        println("[2] Ver Pedidos em Andamento")
        println("[3] Ver Pedidos Finalizados")
        println("[0] Voltar")

        when (ask("Escolha uma opção: ")) {
            "1" -> realizarPedido(cliente)
            "2" -> verPedidos(cliente, emAndamento = true)
            "3" -> verPedidos(cliente, emAndamento = false)
            "0" -> return
            else -> println("Opção inválida.")
        }
    }
}

private fun realizarPedido(cliente: Client) {
    val restaurantes = DataFiles.loadRestaurants()
    if (restaurantes.isEmpty()) {
        println("Nenhum restaurante cadastrado.")
        return
    }

    println("\nRestaurantes disponíveis:")
    restaurantes.forEachIndexed { index, restaurante ->
        println("[${index + 1}] ${restaurante.nome} - ${restaurante.email}")
    }

    val escolha = ask("Selecione o restaurante: ")
    val restauranteSelecionado = escolha.toIntOrNull()?.let { idx ->
        restaurantes.getOrNull(idx - 1)
    }

    if (restauranteSelecionado == null) {
        println("Restaurante inválido.")
        return
    }

    println("\nCardápio de ${restauranteSelecionado.nome}:")
    if (restauranteSelecionado.menu.isEmpty()) {
        println("Cardápio vazio.")
        return
    }

    restauranteSelecionado.menu.forEach { item ->
        println("${item.numero_item} - ${item.descricao} - R$ ${formatMoney(item.preco)}")
    }

    val itensPedido = linkedMapOf<Int, Int>()
    println("\nInforme os itens. Deixe o número do item em branco para finalizar.")

    while (true) {
        val numeroTexto = ask("Número do item: ")
        if (numeroTexto.isBlank()) break

        val numero = numeroTexto.toIntOrNull() ?: run {
            println("Número inválido.")
            continue
        }

        val item = restauranteSelecionado.menu.firstOrNull { it.numero_item == numero }
        if (item == null) {
            println("Item não encontrado.")
            continue
        }

        val quantidadeTexto = ask("Quantidade: ")
        val quantidade = quantidadeTexto.toIntOrNull() ?: run {
            println("Quantidade inválida.")
            continue
        }

        if (quantidade <= 0) {
            println("Quantidade deve ser maior que zero.")
            continue
        }

        itensPedido[numero] = quantidade
    }

    if (itensPedido.isEmpty()) {
        println("Pedido vazio. Nenhuma compra foi registrada.")
        return
    }

    val itensResumo = itensPedido.mapNotNull { (numero, quantidade) ->
        val item = restauranteSelecionado.menu.firstOrNull { it.numero_item == numero }
        item?.let { it to quantidade }
    }

    println("\nResumo do pedido:")
    var total = 0.0
    itensResumo.forEach { (item, quantidade) ->
        val valor = item.preco * quantidade
        total += valor
        println("- ${item.descricao} (${quantidade}x) = R$ ${formatMoney(valor)}")
    }
    println("Total: R$ ${formatMoney(total)}")

    val confirmacao = ask("Confirmar pedido? [S/N]: ").uppercase()
    if (confirmacao != "S") {
        println("Pedido cancelado.")
        return
    }

    val idPedido = "PED-${System.currentTimeMillis()}"
    val dataHora = nowString()
    val linhas = itensResumo.map { (item, quantidade) ->
        val valorTotalItem = item.preco * quantidade
        listOf(
            idPedido,
            dataHora,
            restauranteSelecionado.email,
            restauranteSelecionado.nome,
            cliente.telefone,
            cliente.nome,
            cliente.endereco,
            item.numero_item.toString(),
            quantidade.toString(),
            item.descricao,
            formatMoney(item.preco),
            formatMoney(valorTotalItem),
            "0"
        )
    }

    DataFiles.appendPedidoRows(linhas)
    println("Pedido registrado com sucesso. ID: $idPedido")
}

private fun verPedidos(cliente: Client, emAndamento: Boolean) {
    val linhas = DataFiles.readPedidoRows()
    val filtradas = linhas.filter { linha ->
        linha.size > 12 && linha[4].trim() == cliente.telefone && ((emAndamento && linha[12].trim() != "4") || (!emAndamento && linha[12].trim() == "4"))
    }

    if (filtradas.isEmpty()) {
        println("Nenhum pedido encontrado.")
        return
    }

    println("\nPedidos do cliente ${cliente.nome}:")
    filtradas.forEach { linha ->
        val status = linha[12].trim()
        println("Pedido ${linha[0]} | Restaurante: ${linha[3]} | Status: $status | Total: R$ ${linha[11]} | Item: ${linha[9]}")
    }
}
