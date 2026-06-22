package wiretap.util.buzz

import wiretap.util.Activity
import wiretap.util.LogEntry
import wiretap.util.PropertyName
import wiretap.util.TraceContext

class CreateLogEntry(
    private val root: PropertyName,
    private val composeMessage: ComposeMessage,
) {
    fun from(
        activities: List<Activity>,
        traceContext: TraceContext?,
    ): LogEntry {
        val activity = activities.first()
        val status = activity.status
        val properties = getLogProperties(root, activities, traceContext)

        return LogEntry(
            level = status.level,
            message = composeMessage(root, properties, activity),
            properties = properties,
            exception = status.exception,
        )
    }
}
