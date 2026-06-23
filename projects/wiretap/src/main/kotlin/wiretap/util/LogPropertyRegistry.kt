package wiretap.util

class LogPropertyRegistry {
    private val items = mutableListOf<Item>()

    internal fun toList(): List<Item> =
        items.toList()

    fun register(
        name: PropertyName,
        value: Any?,
        configure: LogPropertyOptions.() -> Unit = {},
    ) {
        items += Item(name, value, LogPropertyOptions().apply(configure))
    }

    data class Item(
        val name: PropertyName,
        val value: Any?,
        val options: LogPropertyOptions,
    )
}


class LogPropertyOptions {
    var cascade: Boolean = false
}


interface LogPropertySource {
    fun LogPropertyRegistry.logProperties(root: PropertyName)
}

