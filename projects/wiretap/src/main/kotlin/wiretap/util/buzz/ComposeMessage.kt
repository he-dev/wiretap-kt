package wiretap.util.buzz

import wiretap.util.AnnotatedMessageParts

fun interface ComposeMessage {
    operator fun invoke(
        stateItems: Map<String, Any?>,
        vararg feeds: Any?,
    ): String
}

class ComposeMessageByAppending(
    private val separator: String = "; ",
) : ComposeMessage {
    override fun invoke(
        stateItems: Map<String, Any?>,
        vararg feeds: Any?,
    ): String {
        val root = PropertyName().wiretap.activity
        val get = GetStateItem { key -> stateItems[key] }
        val parts = mutableListOf<String>()

        val push = PushMessagePart { message, args ->
            if (message != null) {
                parts += render(message, args)
            }
        }

        for (feed in feeds) {
            // core: Explicit message feeds run before annotated parts so custom text can lead the message.
            if (feed is MessagePartFeed) {
                feed.messageParts(root, get, push)
            }
            AnnotatedMessageParts.pushFrom(root, push, feed)
        }

        return parts.joinToString(separator)
    }

    private fun render(message: String, args: Array<out Any?>): String =
        if (args.isEmpty()) {
            message
        } else {
            message.format(*args)
        }
}
