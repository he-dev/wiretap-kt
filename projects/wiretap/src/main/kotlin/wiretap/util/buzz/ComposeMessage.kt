package wiretap.util.buzz

import wiretap.util.AnnotatedMessageParts

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

        val addMessagePart = AddMessagePart { message, args ->
            message?.let {
                messageParts += render(it, args)
            }
        }

        for (source in sources) {
            // core: Explicit message feeds run before annotated parts so custom text can lead the message.
            if (source is MessagePartSource) {
                source.messageParts(root, getLogProperty, addMessagePart)
            }
            AnnotatedMessageParts.addFrom(addMessagePart, source)
        }

        return messageParts.joinToString(separator)
    }

    private fun render(message: String, args: Array<out Any?>): String =
        if (args.isEmpty()) {
            message
        } else {
            message.format(*args)
        }
}
