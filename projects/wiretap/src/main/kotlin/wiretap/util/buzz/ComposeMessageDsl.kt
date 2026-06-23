package wiretap.util.buzz

import wiretap.util.MessagePartMap
import wiretap.util.PropertyName
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.status

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class ComposeMessageDslMarker

@ComposeMessageDslMarker
class ComposeMessageDsl internal constructor() {
    private val registrations = mutableListOf<MessagePartsDsl.() -> Unit>()

    private var arrange: (ArrangePartsDsl.() -> Unit)? = null

    private var join: (JoinMessageDsl.() -> String)? = null

    fun include(contribute: MessagePartsDsl.() -> Unit) {
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
        arrange = checkNotNull(arrange) { "Message arrangement is not configured." },
        join = checkNotNull(join) { "Message joining is not configured." },
    )
}

@ComposeMessageDslMarker
class MessagePartsDsl internal constructor(
    val root: PropertyName,
    private val read: (PropertyName) -> Any?,
    private val add: AddMessagePart,
) {
    operator fun get(name: PropertyName): Any? =
        read(name)

    fun property(
        name: PropertyName,
        configure: MessagePartOptionsBuilder.() -> Unit = {},
    ) = add.property(name, configure)

    fun discrete(
        name: PropertyName,
        value: Any?,
        configure: MessagePartOptionsBuilder.() -> Unit = {},
    ) = add.discrete(name, value, configure)
}

fun MessagePartsDsl.activityHeader() {
    val activity = root.activity
    discrete(activity.name, "${this[activity.name]}[${this[activity.status.code]}]")
}

fun MessagePartsDsl.activityDuration() {
    val duration = root.activity.durationMs
    discrete(duration, this[duration]?.let { "$it ms" } ?: "N/A") { label = "Duration" }
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

fun composeMessage(
    configure: ComposeMessageDsl.() -> Unit = {},
): ComposeMessage =
    ComposeMessageDsl().apply(configure).build()
