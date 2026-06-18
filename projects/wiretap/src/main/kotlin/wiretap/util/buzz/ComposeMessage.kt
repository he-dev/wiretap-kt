package wiretap.util.buzz

import java.util.Locale

fun interface ComposeMessage {
    operator fun invoke(
        logProperties: Map<String, Any?>,
        vararg sources: Any?,
    ): String
}

class ComposeMessageByAppending(
    private val separator: String = "; ",
) : ComposeMessage {
    override fun invoke(
        logProperties: Map<String, Any?>,
        vararg sources: Any?,
    ): String {
        val root = PropertyName().wiretap.activity
        val getLogProperty = GetLogProperty { key -> logProperties[key] }
        val messageParts = mutableListOf<String>()

        val addMessagePart = AddMessagePart { name, value, options ->
            value?.let {
                messageParts += render(name, it, options)
            }
        }

        for (source in sources) {
            // core: Explicit message feeds run before annotated parts so custom text can lead the message.
            if (source is MessagePartSource) {
                source.messageParts(root, getLogProperty, addMessagePart)
            }
            FindAnnotatedMessageParts.on(source, addMessagePart)
        }

        return messageParts.joinToString(separator)
    }

    private fun render(name: String, value: Any, options: MessagePartOptions): String {
        val label = options.label?.ifEmpty { name }
        val formattedValue = options.format
            ?.let { String.format(Locale.ROOT, it, value) }
            ?: value.toString()
        return label?.let { "$it${options.separator}$formattedValue" } ?: formattedValue
    }
}
