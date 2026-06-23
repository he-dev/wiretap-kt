package wiretap.util.buzz

import wiretap.util.MessagePartMap
import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName

interface AddLogProperty {
    fun localOnly(key: PropertyName, value: Any?)
    fun cascading(key: PropertyName, value: Any?)
}

class AddMessagePart internal constructor(
    private val parts: MessagePartMap,
    private val get: (PropertyName) -> Any?,
) {
    fun property(
        name: PropertyName,
        configure: MessagePartOptionsBuilder.() -> Unit = {},
    ) = discrete(name, get(name), configure)

    fun discrete(
        name: PropertyName,
        value: Any?,
        configure: MessagePartOptionsBuilder.() -> Unit = {},
    ) {
        parts.push(name, value, MessagePartOptionsBuilder().apply(configure).build())
    }

    operator fun invoke(
        name: PropertyName,
        configure: MessagePartOptionsBuilder.() -> Unit = {},
    ) = property(name, configure)
}

class MessagePartOptionsBuilder {
    private val defaults = MessagePartOptions()

    var label: String? = null
    var style: String = defaults.style
    var separator: String = defaults.separator
    var format: String? = null

    internal fun build(): MessagePartOptions =
        MessagePartOptions(
            label = label,
            style = style,
            separator = separator,
            format = format,
        )
}

interface LogPropertySource {
    fun AddLogProperty.logProperties(root: PropertyName)
}

interface MessagePartSource {
    fun AddMessagePart.messageParts(root: PropertyName)
}
