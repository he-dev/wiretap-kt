package wiretap.util.buzz

import wiretap.util.Activity
import wiretap.util.LogEntry
import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName
import wiretap.util.TraceContext
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.status
import wiretap.util.wiretap

class CreateLogEntry private constructor(
    private val root: PropertyName,
    private val messagePartRegistrations: List<MessagePartRegistration>,
    private val arrangeMessageParts: MessageContext.() -> List<MessagePartMap.Entry>,
    private val joinMessageParts: List<MessagePartMap.Entry>.() -> String,
) {
    fun from(
        activities: List<Activity>,
        traceContext: TraceContext?,
    ): LogEntry {
        val activity = activities.first()
        val status = activity.status
        val logProperties = getLogProperties(
            root,
            activities,
            traceContext,
        )
        val activityRoot = root.activity
        val get = GetLogProperty { logProperties[it] }
        val messageParts = getMessageParts(activityRoot, get, activity, status)
        val messageContext = MessageContext(root, logProperties, messageParts)
        messagePartRegistrations.forEach { registration ->
            messageContext.registration()
        }
        val messagePartsArranged = messageContext.arrangeMessageParts()
        val message = messagePartsArranged.joinMessageParts()

        return LogEntry(
            level = status.level,
            message = message,
            properties = logProperties,
            exception = status.exception,
        )
    }

    class MessageContext internal constructor(
        val root: PropertyName,
        val properties: Map<String, Any?>,
        val parts: MessagePartMap,
    )

    class Builder {
        var root: PropertyName = PropertyName().wiretap

        private val messagePartRegistrations = defaultMessagePartRegistrations.toMutableList()

        private var arrangeMessageParts: MessageContext.() -> List<MessagePartMap.Entry> = {
            // core: Priority parts are popped first; remaining parts are appended by name.
            listOfNotNull(
                parts.pop(root.activity.name),
                parts.pop(root.activity.durationMs),
            ) + parts.entries.sortedBy { it.key.toString() }.map { it.value }
        }

        private var joinMessageParts: List<MessagePartMap.Entry>.() -> String = {
            joinToString("; ") { it.text }
        }

        fun arrangeMessageParts(
            arrange: MessageContext.() -> List<MessagePartMap.Entry>,
        ) {
            arrangeMessageParts = arrange
        }

        fun registerMessageParts(registration: MessagePartRegistration) {
            messagePartRegistrations += registration
        }

        fun joinMessageParts(
            join: List<MessagePartMap.Entry>.() -> String,
        ) {
            joinMessageParts = join
        }

        fun build(): CreateLogEntry = CreateLogEntry(
            root = root,
            messagePartRegistrations = messagePartRegistrations.toList(),
            arrangeMessageParts = arrangeMessageParts,
            joinMessageParts = joinMessageParts,
        )
    }

    private companion object {
        val defaultMessagePartRegistrations: List<MessagePartRegistration> = listOf(
            {
                val activity = root.activity
                parts.push(
                    activity.name,
                    "${properties[activity.name.toString()]}[${properties[activity.status.code.toString()]}]",
                )
            },
            {
                val duration = root.activity.durationMs
                parts.push(
                    duration,
                    properties[duration.toString()]?.let { "$it ms" } ?: "N/A",
                    MessagePartOptions(label = "Duration"),
                )
            },
        )
    }
}

typealias MessagePartRegistration = CreateLogEntry.MessageContext.() -> Unit

fun createLogEntryBy(
    configure: CreateLogEntry.Builder.() -> Unit = {},
): CreateLogEntry =
    CreateLogEntry.Builder().apply(configure).build()
