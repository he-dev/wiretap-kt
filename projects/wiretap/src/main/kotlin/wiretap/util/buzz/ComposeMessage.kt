package wiretap.util.buzz

import wiretap.util.Activity
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
        properties: Map<String, Any>,
        activity: Activity,
    ): String {
        val parts = getMessageParts(root.activity, properties, activity, activity.status)
        val context = MessagePartsDsl(
            root = root,
            read = { properties[it.toString()] },
            registry = parts,
        )

        registrations.forEach { registration ->
            context.registration()
        }

        val arranged = ArrangePartsDsl.arrange(root, parts, arrange)

        return JoinMessageDsl(arranged).join()
    }
}
