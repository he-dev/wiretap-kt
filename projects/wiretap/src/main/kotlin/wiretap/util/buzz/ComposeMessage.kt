package wiretap.util.buzz

import wiretap.util.PropertyName
import wiretap.util.activity

// TODO: Remove the overlapping message pipeline from CreateLogEntry when this draft is integrated.
class ComposeMessage internal constructor(
    private val registrations: List<MessagePartsDsl.() -> Unit>,
    private val arrange: ArrangePartsDsl.() -> Unit,
    private val join: JoinMessageDsl.() -> String,
) {
    operator fun invoke(
        root: PropertyName,
        properties: Map<String, Any?>,
        vararg sources: Any?,
    ): String {
        val get = GetLogProperty { properties[it] }
        val parts = getMessageParts(root.activity, get, *sources)
        val context = MessagePartsDsl(
            root = root,
            getLogProperty = get,
            add = AddMessagePart(parts::push),
        )

        registrations.forEach { registration ->
            context.registration()
        }

        val arranged = ArrangePartsDsl.arrange(root, parts, arrange)

        return JoinMessageDsl(arranged).join()
    }
}
