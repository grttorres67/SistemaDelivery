package delivery

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val DATA_DIR = File(System.getProperty("user.dir"), "data")
private const val CSV_HEADER = "id_pedido;data_hora;email_restaurante;nome_restaurante;telefone_cliente;nome_cliente;endereco_cliente;numero_item;quantidade;descricao_item;valor_unitario;valor_total_item;status"

fun ensureDataFiles() {
    DATA_DIR.mkdirs()

    val clientesFile = File(DATA_DIR, "clientes.json")
    if (!clientesFile.exists()) {
        clientesFile.writeText("[]")
    }

    val pedidosFile = File(DATA_DIR, "pedidos.csv")
    if (!pedidosFile.exists() || pedidosFile.readText().isBlank()) {
        pedidosFile.writeText("$CSV_HEADER\n")
    }
}

data class MenuItem(
    val numero_item: Int,
    val descricao: String,
    val preco: Double
)

data class Restaurant(
    val nome: String,
    val email: String,
    val endereco: String,
    val menu: MutableList<MenuItem>
) {
    fun toJsonObject(): Map<String, Any?> = linkedMapOf(
        "nome" to nome,
        "email" to email,
        "endereco" to endereco,
        "menu" to menu.map { item ->
            linkedMapOf(
                "numero_item" to item.numero_item,
                "descricao" to item.descricao,
                "preco" to item.preco
            )
        }
    )

    fun toJson(): String = jsonEncode(toJsonObject())

    companion object {
        fun fromJsonFile(file: File): Restaurant {
            val parsed = JsonParser(file.readText()).parse()
            val map = parsed as? Map<*, *> ?: error("Arquivo inválido: ${file.name}")
            val menuList = (map["menu"] as? List<*>) ?: emptyList<Any?>()
            val menu = menuList.mapNotNull { item ->
                val obj = item as? Map<*, *> ?: return@mapNotNull null
                val numero = (obj["numero_item"] as? Number)?.toInt() ?: 0
                val descricao = obj["descricao"] as? String ?: ""
                val preco = (obj["preco"] as? Number)?.toDouble() ?: 0.0
                MenuItem(numero, descricao, preco)
            }.toMutableList()

            return Restaurant(
                nome = map["nome"] as? String ?: "",
                email = map["email"] as? String ?: "",
                endereco = map["endereco"] as? String ?: "",
                menu = menu
            )
        }
    }
}

data class Client(
    val nome: String,
    val telefone: String,
    val endereco: String
) {
    fun toJsonObject(): Map<String, Any?> = linkedMapOf(
        "nome" to nome,
        "telefone" to telefone,
        "endereco" to endereco
    )

    fun toJson(): String = jsonEncode(toJsonObject())

    companion object {
        fun fromJsonObject(map: Map<*, *>): Client = Client(
            nome = map["nome"] as? String ?: "",
            telefone = map["telefone"] as? String ?: "",
            endereco = map["endereco"] as? String ?: ""
        )
    }
}

object DataFiles {
    fun restaurantesDir(): File = File(DATA_DIR, ".")

    fun restaurantFiles(): List<File> = DATA_DIR.listFiles { file ->
        file.isFile && file.name.startsWith("restaurante_") && file.name.endsWith(".json")
    }?.sortedBy { it.name } ?: emptyList()

    fun loadRestaurants(): List<Restaurant> = restaurantFiles().mapNotNull { file ->
        runCatching { Restaurant.fromJsonFile(file) }.getOrNull()
    }

    fun saveRestaurant(restaurant: Restaurant) {
        val file = File(DATA_DIR, "restaurante_${nextRestaurantId()}.json")
        if (restaurantFiles().isEmpty()) {
            file.writeText(restaurant.toJson())
        } else {
            val existing = loadRestaurants().firstOrNull { it.email.equals(restaurant.email, true) }
            val targetFile = if (existing != null) {
                restaurantFiles().first { fileName ->
                    val parsed = Restaurant.fromJsonFile(fileName)
                    parsed.email.equals(existing.email, true)
                }
            } else {
                File(DATA_DIR, "restaurante_${nextRestaurantId()}.json")
            }
            targetFile.writeText(restaurant.toJson())
        }
    }

    fun saveNewRestaurant(restaurant: Restaurant) {
        val nextId = nextRestaurantId()
        val file = File(DATA_DIR, "restaurante_${nextId}.json")
        file.writeText(restaurant.toJson())
    }

    fun nextRestaurantId(): Int {
        val existingIds = restaurantFiles().mapNotNull { file ->
            val name = file.nameWithoutExtension.removePrefix("restaurante_")
            name.toIntOrNull()
        }
        return if (existingIds.isEmpty()) 1 else (existingIds.maxOrNull() ?: 0) + 1
    }

    fun loadClients(): List<Client> {
        val file = File(DATA_DIR, "clientes.json")
        if (!file.exists() || file.readText().isBlank()) return emptyList()
        val parsed = JsonParser(file.readText()).parse()
        val list = parsed as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            Client.fromJsonObject(map)
        }
    }

    fun saveClients(clients: List<Client>) {
        val file = File(DATA_DIR, "clientes.json")
        file.writeText(jsonEncode(clients.map { it.toJsonObject() }))
    }

    fun readPedidoRows(): List<List<String>> {
        val file = File(DATA_DIR, "pedidos.csv")
        if (!file.exists() || file.readText().isBlank()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .drop(1)
            .map { it.split(";") }
    }

    fun appendPedidoRows(rows: List<List<String>>) {
        val file = File(DATA_DIR, "pedidos.csv")
        val builder = StringBuilder()
        if (!file.exists() || file.readText().isBlank()) {
            builder.append("$CSV_HEADER\n")
        }
        rows.forEach { row ->
            builder.append(row.joinToString(";")).append("\n")
        }
        file.appendText(builder.toString())
    }

    fun updatePedidoStatus(idPedido: String, novoStatus: Int) {
        val file = File(DATA_DIR, "pedidos.csv")
        if (!file.exists()) return
        val linhas = file.readLines()
        if (linhas.isEmpty()) return

        val novaLista = mutableListOf<String>()
        novaLista.add(linhas.first())

        for (i in 1 until linhas.size) {
            val linha = linhas[i]
            if (linha.isBlank()) continue
            val campos = linha.split(";")
            if (campos.isNotEmpty() && campos[0].trim() == idPedido.trim()) {
                val atualizado = campos.toMutableList()
                if (atualizado.size > 12) {
                    atualizado[12] = novoStatus.toString()
                    novaLista.add(atualizado.joinToString(";"))
                } else {
                    novaLista.add(linha)
                }
            } else {
                novaLista.add(linha)
            }
        }
        file.writeText(novaLista.joinToString("\n") + "\n")
    }
}

fun ask(prompt: String): String = print(prompt).let { readLine() ?: "" }.trim()

fun jsonEncode(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")}\""
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (key, entryValue) ->
            val jsonKey = jsonEncode(key.toString())
            val jsonValue = jsonEncode(entryValue)
            "$jsonKey:$jsonValue"
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { item -> jsonEncode(item) }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { item -> jsonEncode(item) }
        else -> "\"${value.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}

class JsonParser(private val text: String) {
    private var index = 0

    fun parse(): Any? {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        if (index != text.length) {
            throw IllegalArgumentException("JSON inválido: caractere inesperado em posição $index")
        }
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (index >= text.length) throw IllegalArgumentException("JSON incompleto")

        return when (val char = text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Token inválido: $char")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        skipWhitespace()
        val result = linkedMapOf<String, Any?>()
        if (peek('}')) {
            index++
            return result
        }

        while (true) {
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            result[key] = value
            skipWhitespace()
            if (peek(',')) {
                index++
                skipWhitespace()
                continue
            }
            if (peek('}')) {
                index++
                return result
            }
            throw IllegalArgumentException("Objeto JSON inválido")
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        skipWhitespace()
        val result = mutableListOf<Any?>()
        if (peek(']')) {
            index++
            return result
        }

        while (true) {
            result.add(parseValue())
            skipWhitespace()
            if (peek(',')) {
                index++
                skipWhitespace()
                continue
            }
            if (peek(']')) {
                index++
                return result
            }
            throw IllegalArgumentException("Array JSON inválido")
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (index < text.length) {
            val current = text[index++]
            if (current == '"') {
                return builder.toString()
            }
            if (current == '\\') {
                if (index >= text.length) throw IllegalArgumentException("String terminada de forma inválida")
                val escaped = text[index++]
                when (escaped) {
                    '"' -> builder.append('"')
                    '\\' -> builder.append('\\')
                    '/' -> builder.append('/')
                    'b' -> builder.append('\b')
                    'f' -> builder.append('\u000C')
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'u' -> {
                        if (index + 4 > text.length) throw IllegalArgumentException("Unicode inválido")
                        val hex = text.substring(index, index + 4)
                        builder.append(hex.toInt(16).toChar())
                        index += 4
                    }
                    else -> throw IllegalArgumentException("Escape inválido: \\$escaped")
                }
            } else {
                builder.append(current)
            }
        }
        throw IllegalArgumentException("String JSON sem fechamento")
    }

    private fun parseNumber(): Number {
        val start = index
        if (text[index] == '-') index++
        while (index < text.length && text[index].isDigit()) index++
        if (index < text.length && text[index] == '.') {
            index++
            while (index < text.length && text[index].isDigit()) index++
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            index++
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
            while (index < text.length && text[index].isDigit()) index++
        }
        val token = text.substring(start, index)
        return if (token.contains('.') || token.contains('e') || token.contains('E')) token.toDouble() else token.toLong()
    }

    private fun parseLiteral(expected: String, value: Any?): Any? {
        if (!text.startsWith(expected, index)) {
            throw IllegalArgumentException("Literal inválido: esperado $expected")
        }
        index += expected.length
        return value
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) {
            index++
        }
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (index >= text.length || text[index] != expected) {
            throw IllegalArgumentException("Esperado '$expected' em posição $index")
        }
        index++
    }

    private fun peek(expected: Char): Boolean {
        skipWhitespace()
        return index < text.length && text[index] == expected
    }
}

fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

fun nowString(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
