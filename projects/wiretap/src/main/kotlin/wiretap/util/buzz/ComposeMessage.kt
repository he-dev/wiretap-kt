package wiretap.util.buzz

import wiretap.util.DottedName
import wiretap.util.RemarkBuilder
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
class ComposeMessage internal constructor() {

    private var remarks: (RemarkBuilder.() -> Unit) = {}
    private var arrange: (ArrangeRemarks.() -> Unit) = { addRemaining() }
    private var join: (JoinRemarks.() -> String) = { joinToString("; ") { it } }

    fun remarks(block: RemarkBuilder.() -> Unit) {
        remarks = block
    }

    fun arrange(block: ArrangeRemarks.() -> Unit) {
        arrange = block
    }

    fun join(block: JoinRemarks.() -> String) {
        join = block
    }

    internal operator fun invoke(
        root: DottedName,
        details: MutableMap<DottedName, Any?>,
        remarks: MutableMap<DottedName, String>
    ): String {
        remarks(RemarkBuilder(root, details, remarks))
        val arranged = emptyList<String>().toMutableList()
        arrange(ArrangeRemarks(root, remarks, arranged))
        return join(JoinRemarks(arranged))
    }
}

fun RemarkBuilder.addActivity() {
    val activity = root.activity
    add(activity.name, render = { "${details[it]}[${details[activity.status.code]}]" })
}

fun RemarkBuilder.addActivityDuration() {
    val duration = root.activity.durationMs
    add(duration, render = { name -> details[name]?.let { "$it ms" } ?: "N/A" }) { label = "Duration" }
}

@ComposeMessageDslMarker
class ArrangeRemarks internal constructor(
    val root: DottedName,
    private val remarks: MutableMap<DottedName, String>,
    private val arranged: MutableList<String>
) {
    fun add(name: DottedName) {
        remarks.remove(name)?.let { arranged.add(it) }
    }

    fun addRemaining() {
        remarks.forEach { arranged.add(it.value) }
        remarks.clear()
    }
}

@ComposeMessageDslMarker
class JoinRemarks internal constructor(
    remarks: List<String>,
) : Iterable<String> by remarks

fun composeMessage(
    block: ComposeMessage.() -> Unit = {},
): ComposeMessage {
    val compose = ComposeMessage()
    block(compose)
    return compose
}


