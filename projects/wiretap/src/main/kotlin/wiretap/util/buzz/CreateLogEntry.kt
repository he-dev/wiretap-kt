package wiretap.util.buzz

import wiretap.util.LogEntry
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.AnnotatedStateItems
import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.state
import wiretap.util.status
import wiretap.util.wiretap
import java.util.Locale

class CreateLogEntry private constructor(
    private val root: PropertyName,
    private val arrangeMessageParts: MessageContext.() -> List<MessagePartMap.Entry>,
    private val joinMessageParts: List<MessagePartMap.Entry>.() -> String,
) {
    fun from(scope: ActivityScope<*>, status: ActivityStatus<*>): LogEntry {
        val logProperties = collectLogProperties(root, scope, status)
        val messageParts = collectMessageParts(root.activity, logProperties, scope, status)
        val messageContext = MessageContext(root, logProperties, messageParts)
        val messagePartsArranged = messageContext.arrangeMessageParts()
        val message = messagePartsArranged.joinMessageParts()

        return LogEntry(
            level = status.level,
            message = message,
            properties = logProperties,
            exception = status.exception,
        )
    }

    private fun collectLogProperties(
        root: PropertyName,
        scope: ActivityScope<*>,
        status: ActivityStatus<*>,
    ): Map<String, Any?> {
        val properties = linkedMapOf<String, Any?>()
        val addLogProperty = AddLogProperty { key, value ->
            value?.let { properties[key] = it }
        }

        AnnotatedStateItems.pushFromAncestors(
            root.activity.state,
            addLogProperty,
            // core: Cascade root-first so nearer activities overwrite their ancestors.
            scope.reversed().dropLast(1).asSequence().map { it.activity },
        )

        // core: Framework scopes and optional user sources share one stable property feed.
        for (source in sequenceOf(scope, scope.activity, status).mapNotNull { it as? LogPropertySource }) {
            source.logProperties(root, addLogProperty)
        }

        AnnotatedStateItems.pushFromSelf(root.activity.state, addLogProperty, scope.activity)
        AnnotatedStateItems.pushFromSelf(root.activity.state, addLogProperty, status)

        return properties
    }

    private fun collectMessageParts(
        root: PropertyName,
        properties: Map<String, Any?>,
        scope: ActivityScope<*>,
        status: ActivityStatus<*>,
    ): MessagePartMap {
        val messageParts = MessagePartMap()
        val getLogProperty = GetLogProperty { properties[it] }
        val addMessagePart = AddMessagePart { name, value, options ->
            messageParts.push(name, value, options)
        }

        addMessagePart(
            root.name,
            "${getLogProperty(root.name)}[${getLogProperty(root.status.code)}]",
        )

        for (source in listOf(scope.activity, status)) {
            (source as? MessagePartSource)?.messageParts(root, getLogProperty, addMessagePart)
            FindAnnotatedMessageParts.on(source, addMessagePart)
        }
        addMessagePart(
            root.durationMs,
            scope.durationMs?.let { "$it ms" } ?: "N/A",
            MessagePartOptions(label = "Duration"),
        )

        return messageParts
    }

    class MessagePartMap internal constructor(
        private val entriesByName: MutableMap<PropertyName, Entry> = linkedMapOf(),
    ) : MutableMap<PropertyName, MessagePartMap.Entry> by entriesByName {
        data class Entry(
            val name: PropertyName,
            val value: Any?,
            val options: MessagePartOptions,
        ) {
            val text: String
                get() {
                    val label = options.label?.ifEmpty { name.toString() }
                    val valueFormatted = options.format
                        ?.let { String.format(Locale.ROOT, it, value) }
                        ?: value.toString()

                    return label?.let { "$it${options.separator}$valueFormatted" } ?: valueFormatted
                }
        }

        fun push(
            name: PropertyName,
            value: Any?,
            options: MessagePartOptions = MessagePartOptions(),
        ) {
            this[name] = Entry(name, value, options)
        }

        fun pop(name: PropertyName): Entry? =
            remove(name)
    }

    class MessageContext internal constructor(
        val root: PropertyName,
        val properties: Map<String, Any?>,
        val parts: MessagePartMap,
    )

    class Builder {
        var root: PropertyName = PropertyName().wiretap

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

        fun joinMessageParts(
            join: List<MessagePartMap.Entry>.() -> String,
        ) {
            joinMessageParts = join
        }

        fun build(): CreateLogEntry = CreateLogEntry(
            root = root,
            arrangeMessageParts = arrangeMessageParts,
            joinMessageParts = joinMessageParts,
        )
    }
}

fun createLogEntryBy(
    configure: CreateLogEntry.Builder.() -> Unit = {},
): CreateLogEntry =
    CreateLogEntry.Builder().apply(configure).build()
