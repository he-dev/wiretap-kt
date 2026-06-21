package wiretap.util.buzz

import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName
import wiretap.util.activity
import wiretap.util.durationMs
import wiretap.util.name

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class ComposeMessageDslMarker

@ComposeMessageDslMarker
class ComposeMessageDsl internal constructor() {
    private val registrations = mutableListOf<MessagePartsDsl.() -> Unit>()

    private var arrange: ArrangePartsDsl.() -> Unit = {
        positional(root.activity.name)
        positional(root.activity.durationMs)
        remaining()
    }

    private var join: JoinMessageDsl.() -> String = {
        joinToString("; ") { it.text }
    }

    // TODO: Revisit this registration vocabulary and its replacement semantics before integration.
    fun messageParts(contribute: MessagePartsDsl.() -> Unit) {
        registrations += contribute
    }

    fun arrange(arrange: ArrangePartsDsl.() -> Unit) {
        this.arrange = arrange
    }

    fun join(join: JoinMessageDsl.() -> String) {
        this.join = join
    }

    internal fun build(): ComposeMessage = ComposeMessage(
        registrations = registrations.toList(),
        arrange = arrange,
        join = join,
    )
}

@ComposeMessageDslMarker
class MessagePartsDsl internal constructor(
    val root: PropertyName,
    private val getLogProperty: GetLogProperty,
    private val add: AddMessagePart,
) {
    operator fun get(name: PropertyName): Any? =
        getLogProperty(name)

    fun push(
        name: PropertyName,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) {
        add(name, value, options)
    }
}

@ComposeMessageDslMarker
class ArrangePartsDsl private constructor(
    val root: PropertyName,
    private val parts: MessagePartMap,
) {
    private val arranged = mutableListOf<MessagePartMap.Entry>()

    fun positional(name: PropertyName) {
        parts.pop(name)?.let(arranged::add)
    }

    fun remaining() {
        arranged += parts.values
        parts.clear()
    }

    internal companion object {
        fun arrange(
            root: PropertyName,
            parts: MessagePartMap,
            configure: ArrangePartsDsl.() -> Unit,
        ): List<MessagePartMap.Entry> =
            ArrangePartsDsl(root, parts).apply(configure).arranged
    }
}

@ComposeMessageDslMarker
class JoinMessageDsl internal constructor(
    entries: List<MessagePartMap.Entry>,
) : List<MessagePartMap.Entry> by entries

fun composeMessageBy(
    configure: ComposeMessageDsl.() -> Unit = {},
): ComposeMessage =
    ComposeMessageDsl().apply(configure).build()
