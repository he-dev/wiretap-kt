package wiretap.util

class MessagePartRegistry internal constructor(
    private val get: (PropertyName) -> Any?,
) {
    private val items = mutableListOf<Item>()

    internal fun toList(): List<Item> =
        items.toList()

    fun property(
        name: PropertyName,
        configure: MessagePartOptions.() -> Unit = {},
    ) = discrete(name, get(name), configure)

    fun discrete(
        name: PropertyName,
        value: Any?,
        configure: MessagePartOptions.() -> Unit = {},
    ) {
        items += Item(name, value, MessagePartOptions().apply(configure))
    }

    operator fun invoke(
        name: PropertyName,
        configure: MessagePartOptions.() -> Unit = {},
    ) = property(name, configure)

    data class Item(
        val name: PropertyName,
        val value: Any?,
        val options: MessagePartOptions,
    )
}

interface MessagePartSource {
    fun MessagePartRegistry.messageParts(root: PropertyName)
}
