package wiretap.util.buzz

fun interface ComposeMessage {
    operator fun invoke(
        stateItems: Map<String, Any?>,
        vararg feeds: MessagePartFeed,
    ): String
}

class ComposeMessageByAppending(
    private val separator: String = "; ",
) : ComposeMessage {
    override fun invoke(
        stateItems: Map<String, Any?>,
        vararg feeds: MessagePartFeed,
    ): String {
        val root = PropertyName()
        val get = GetStateItem { key -> stateItems[key] }
        val parts = mutableListOf<String>()

        val push = PushMessagePart { message, args ->
            if (message != null) {
                parts += render(message, args)
            }
        }

        for (feed in feeds) {
            feed.messageParts(root, get, push)
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
