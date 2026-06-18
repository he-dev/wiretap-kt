package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.GetLogProperty
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.MessagePartOptions
import wiretap.util.buzz.MessagePartSource
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.activity
import wiretap.util.buzz.state
import wiretap.util.buzz.wiretap

class ActivityLogEntryFactory private constructor(
    private val root: PropertyName,
    private val arrangeMessageParts: MessageContext.() -> List<MessagePartMap.Entry>,
    private val joinMessageParts: List<MessagePartMap.Entry>.() -> String,
) {
    fun create(scope: ActivityScope<*>, status: ActivityStatus<*>): ActivityLogEntry {
        val properties = collectLogProperties(root, scope, status)
        val standardMessageParts = collectMessageParts(root, properties, scope, status)
        val messageContext = MessageContext(root, properties, standardMessageParts)
        val arrangedMessageParts = messageContext.arrangeMessageParts()

        return ActivityLogEntry(
            level = status.level,
            message = arrangedMessageParts.joinMessageParts(),
            properties = properties,
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

        // core: Cascaded properties are collected root-first so nearer scopes can override them.
        scope.ancestors()
            .toList()
            .asReversed()
            .forEach { ancestor ->
                AnnotatedStateItems.pushFrom(
                    root.state,
                    addLogProperty,
                    ancestor.activity,
                    cascadingOnly = true,
                )
            }

        // core: Framework scopes and optional user sources share one stable property feed.
        addLogPropertiesFrom(scope, root, addLogProperty)
        addLogPropertiesFrom(status, root, addLogProperty)
        addLogPropertiesFrom(scope.activity, root, addLogProperty)

        AnnotatedStateItems.pushFrom(
            root.state,
            addLogProperty,
            scope.activity,
            status,
        )

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

        for (source in listOf(scope.activity, status)) {
            (source as? MessagePartSource)?.messageParts(root, getLogProperty, addMessagePart)
            AnnotatedMessageParts.addFrom(addMessagePart, source)
        }
        addMessagePart(
            "duration",
            scope.durationMs?.let { "$it ms" } ?: "N/A",
            MessagePartOptions(label = "Duration"),
        )

        return messageParts
    }

    class MessagePartMap internal constructor(
        private val entriesByName: MutableMap<String, Entry> = linkedMapOf(),
    ) : MutableMap<String, MessagePartMap.Entry> by entriesByName {
        data class Entry(
            val name: String,
            val value: Any?,
            val options: MessagePartOptions,
        )

        fun push(
            name: String,
            value: Any?,
            options: MessagePartOptions = MessagePartOptions(),
        ) {
            this[name] = Entry(name, value, options)
        }

        fun pop(name: String): Entry? =
            remove(name)
    }

    class MessageContext internal constructor(
        val root: PropertyName,
        val properties: Map<String, Any?>,
        val parts: MessagePartMap,
    )

    class Builder {
        var root: PropertyName = PropertyName().wiretap.activity

        private var arrangeMessageParts: MessageContext.() -> List<MessagePartMap.Entry> = {
            // core: Priority parts are popped first; remaining parts are appended by name.
            listOfNotNull(
                parts.pop("activity"), parts.pop("duration"),
            ) + parts.entries.sortedBy { it.key }.map { it.value }
        }

        private var joinMessageParts: List<MessagePartMap.Entry>.() -> String = {
            joinToString("; ") { entry ->
                val label = entry.options.label?.ifEmpty { entry.name }
                label
                    ?.let { "$it${entry.options.separator}${entry.value}" }
                    ?: entry.value.toString()
            }
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

        fun build(): ActivityLogEntryFactory = ActivityLogEntryFactory(
            root = root,
            arrangeMessageParts = arrangeMessageParts,
            joinMessageParts = joinMessageParts,
        )
    }
}

fun activityLogEntryFactory(
    configure: ActivityLogEntryFactory.Builder.() -> Unit = {},
): ActivityLogEntryFactory =
    ActivityLogEntryFactory.Builder().apply(configure).build()

private fun addLogPropertiesFrom(
    source: Any?,
    root: PropertyName,
    addLogProperty: AddLogProperty,
) {
    (source as? LogPropertySource)?.logProperties(root, addLogProperty)
}

private fun ActivityScope<*>.ancestors(): Sequence<ActivityScope<*>> =
    generateSequence(parent) { it.parent }
