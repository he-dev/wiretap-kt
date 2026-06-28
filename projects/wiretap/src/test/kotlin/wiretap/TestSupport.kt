package wiretap

import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel

data class CapturedLog(
    val level: ActivityStatusLevel,
    val details: Map<String, Any?>,
    val message: String,
    val exception: Throwable?,
) {
    operator fun get(name: String): Any? =
        details[name]
}

class CapturingActivityLogger(
    private val logs: MutableList<CapturedLog> = mutableListOf(),
) : ActivityLogger {
    val entries: List<CapturedLog>
        get() = logs

    override fun log(
        level: ActivityStatusLevel,
        details: Map<String, Any?>,
        message: String,
        exception: Throwable?,
    ) {
        logs += CapturedLog(level, details, message, exception)
    }
}
