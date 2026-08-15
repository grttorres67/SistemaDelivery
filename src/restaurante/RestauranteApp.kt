package delivery

fun main() {
    ensureDataFiles()

    while (true) {
        println("\n=== APP RESTAURANTE ===")
        println("[1] Entrar como Restaurante Existente")
        println("[2] Novo Cadastro")
        println("[0] Sair")

        when (ask("Escolha uma opção: ")) {
            "1" -> loginRestaurante()
            "2" -> novoCadastroRestaurante()
            "0" -> {
                println("Encerrando o app restaurante...")
                return
            }
            else -> println("Opção inválida.")
        }
    }
}

private fun loginRestaurante() {
    val email = ask("Digite o e-mail do restaurante: ")
    if (email.isBlank()) {
        println("E-mail obrigatório.")
        return
    }

    val restaurante = DataFiles.loadRestaurants().firstOrNull { it.email.equals(email, ignoreCase = true) }
    if (restaurante == null) {
        println("Restaurante não encontrado.")
        return
    }

    menuRestaurante(restaurante)
}

private fun novoCadastroRestaurante() {
    val nome = ask("Nome do restaurante: ")
    val email = ask("E-mail: ")
    val endereco = ask("Endereço: ")

    if (nome.isBlank() || email.isBlank() || endereco.isBlank()) {
        println("Todos os campos são obrigatórios.")
        return
    }

    if (DataFiles.loadRestaurants().any { it.email.equals(email, ignoreCase = true) }) {
        println("E-mail já cadastrado. Escolha outro.")
        return
    }

    val menu = mutableListOf<MenuItem>()
    println("Cadastro do cardápio. Deixe o número do item em branco para encerrar.")

    while (true) {
        val numeroTexto = ask("Número do item: ")
        if (numeroTexto.isBlank()) break

        val numero = numeroTexto.toIntOrNull()
        if (numero == null) {
            println("Número do item inválido.")
            continue
        }

        val descricao = ask("Descrição do item: ")
        val precoTexto = ask("Preço do item: ")
        val preco = precoTexto.toDoubleOrNull()

        if (descricao.isBlank() || preco == null) {
            println("Descrição e preço são obrigatórios.")
            continue
        }

        menu.add(MenuItem(numero, descricao, preco))
    }

    val restaurante = Restaurant(nome, email, endereco, menu)
    DataFiles.saveNewRestaurant(restaurante)
    println("Restaurante cadastrado com sucesso.")
}

private fun menuRestaurante(restaurante: Restaurant) {
    while (true) {
        println("\n=== MENU RESTAURANTE ===")
        println("[1] Gerenciar Cardápio")
        println("[2] Visualizar Pedidos por Status")
        println("[3] Alterar Status do Pedido")
        println("[0] Voltar")

        when (ask("Escolha uma opção: ")) {
            "1" -> gerenciarCardapio(restaurante)
            "2" -> visualizarPedidos(restaurante)
            "3" -> alterarStatusPedido(restaurante)
            "0" -> return
            else -> println("Opção inválida.")
        }
    }
}

private fun gerenciarCardapio(restaurante: Restaurant) {
    while (true) {
        println("\nCardápio de ${restaurante.nome}")
        println("[A] Ver Cardápio")
        println("[B] Adicionar Item")
        println("[C] Remover Item")
        println("[0] Voltar")

        when (ask("Opção: ").uppercase()) {
            "A" -> {
                if (restaurante.menu.isEmpty()) {
                    println("Cardápio vazio.")
                } else {
                    println("Itens do cardápio:")
                    restaurante.menu.forEach { item ->
                        println("${item.numero_item} - ${item.descricao} - R$ ${formatMoney(item.preco)}")
                    }
                }
            }
            "B" -> {
                val numeroTexto = ask("Número do item: ")
                if (numeroTexto.isBlank()) {
                    println("Número não informado.")
                    continue
                }

                val numero = numeroTexto.toIntOrNull() ?: run {
                    println("Número inválido.")
                    continue
                }

                val descricao = ask("Descrição: ")
                val precoTexto = ask("Preço: ")
                val preco = precoTexto.toDoubleOrNull() ?: run {
                    println("Preço inválido.")
                    continue
                }

                val novoItem = MenuItem(numero, descricao, preco)
                val atualizados = restaurante.menu.toMutableList()
                atualizados.removeAll { it.numero_item == numero }
                atualizados.add(novoItem)
                val atualizado = RestauranteAtualizado(restaurante, atualizados)
                salvarRestauranteAtualizado(atualizado)
                println("Item adicionado com sucesso.")
            }
            "C" -> {
                val numeroTexto = ask("Número do item para remover: ")
                if (numeroTexto.isBlank()) {
                    println("Número não informado.")
                    continue
                }

                val numero = numeroTexto.toIntOrNull() ?: run {
                    println("Número inválido.")
                    continue
                }

                val atualizados = restaurante.menu.filterNot { it.numero_item == numero }.toMutableList()
                if (atualizados.size == restaurante.menu.size) {
                    println("Item não encontrado.")
                    continue
                }

                val atualizado = RestauranteAtualizado(restaurante, atualizados)
                salvarRestauranteAtualizado(atualizado)
                println("Item removido com sucesso.")
            }
            "0" -> return
            else -> println("Opção inválida.")
        }
    }
}

private data class RestauranteAtualizado(
    val restaurante: Restaurant,
    val menuAtualizado: MutableList<MenuItem>
)

private fun salvarRestauranteAtualizado(restauranteAtualizado: RestauranteAtualizado) {
    val restaurante = Restaurant(
        nome = restauranteAtualizado.restaurante.nome,
        email = restauranteAtualizado.restaurante.email,
        endereco = restauranteAtualizado.restaurante.endereco,
        menu = restauranteAtualizado.menuAtualizado
    )

    val existingFile = DataFiles.restaurantFiles().firstOrNull { file ->
        val saved = runCatching { Restaurant.fromJsonFile(file) }.getOrNull()
        saved?.email.equals(restaurante.email, ignoreCase = true) == true
    }

    if (existingFile != null) {
        existingFile.writeText(restaurante.toJson())
    } else {
        DataFiles.saveNewRestaurant(restaurante)
    }
}

private fun visualizarPedidos(restaurante: Restaurant) {
    println("\nPedidos do restaurante ${restaurante.email}")
    val linhas = DataFiles.readPedidoRows()
    val filtrados = linhas.filter { linha ->
        linha.size > 2 && linha[2].equals(restaurante.email, ignoreCase = true)
    }

    if (filtrados.isEmpty()) {
        println("Nenhum pedido encontrado.")
        return
    }

    for (status in 0..4) {
        val pedidos = filtrados.filter { it.size > 12 && it[12].trim() == status.toString() }
        if (pedidos.isEmpty()) {
            println("\nStatus $status: nenhum pedido")
            continue
        }

        println("\nStatus $status:")
        pedidos.forEach { pedido ->
            println("Pedido ${pedido[0]} | Cliente: ${pedido[5]} | Item: ${pedido[9]} | qtd: ${pedido[8]} | total: R$ ${pedido[11]}")
        }
    }
}

private fun alterarStatusPedido(restaurante: Restaurant) {
    val linhas = DataFiles.readPedidoRows()
    val idPedido = ask("ID do pedido: ")

    val pedidoEncontrado = linhas.firstOrNull { it.size > 12 && it[0].trim() == idPedido && it[2].equals(restaurante.email, ignoreCase = true) }
    if (pedidoEncontrado == null) {
        println("Pedido não encontrado para este restaurante.")
        return
    }

    val novoStatusTexto = ask("Novo status (0 a 4): ")
    val novoStatus = novoStatusTexto.toIntOrNull() ?: run {
        println("Status inválido.")
        return
    }

    if (novoStatus !in 0..4) {
        println("Status deve estar entre 0 e 4.")
        return
    }

    DataFiles.updatePedidoStatus(idPedido, novoStatus)
    println("Status atualizado com sucesso.")
}
