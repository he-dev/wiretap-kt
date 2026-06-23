package wiretap.util.buzz

import wiretap.util.Activity
import wiretap.util.ActivityStatusRole
import wiretap.util.Configuration
import wiretap.util.LogPropertyRegistry
import wiretap.util.LogPropertySource
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.TraceContext
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.depth
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.nullIfUnset
import wiretap.util.parentSpanId
import wiretap.util.path
import wiretap.util.role
import wiretap.util.spanId
import wiretap.util.state
import wiretap.util.status
import wiretap.util.tags
import wiretap.util.traceId
import wiretap.util.warnAboutDuplicateLogProperty

fun getLogProperties(
    root: PropertyName,
    activities: List<Activity>,
    traceContext: TraceContext?,
): Map<String, Any> =
    buildMap<String, Any> {
        val activity = activities.first()
        val status = activity.status
        // core: A null value claims its key without entering the result map.
        val nullValueKeys = mutableSetOf<String>()

        // core: Sources are processed strongest-first, so the first claim owns a key.
        fun putOnce(key: PropertyName, value: Any?) {
            val name = key.toString()

            // core: A null value claims its key without entering the result map.
            if (name in nullValueKeys) return

            if (value == null) {
                // core: Record null-value key.
                if (!containsKey(name)) nullValueKeys += name
            } else {
                putIfAbsent(name, value)
            }
        }

        // core: Interface values are collected before annotations because first claims win.
        fun registerCustom(source: Any?, registry: LogPropertyRegistry, isCurrent: Boolean) {
            val propertySource = source as? LogPropertySource ?: return

            if (isCurrent) {
                // note: Duplicate detection is local to one current-interface invocation.
                val seenKeys = mutableSetOf<String>()
                val checked = LogPropertyRegistry()
                with(propertySource) { checked.logProperties(root) }
                checked.toList().forEach { item ->
                    if (seenKeys.add(item.name.toString())) {
                        registry.register(item.name, item.value) { cascade = item.options.cascade }
                    } else {
                        Configuration.diagnosticLogger.warnAboutDuplicateLogProperty(source::class, item.name)
                    }
                }
            } else {
                with(propertySource) { registry.logProperties(root) }
            }
        }

        fun collect(source: Any?, isCurrent: Boolean) {
            source ?: return
            val registry = LogPropertyRegistry()
            registerCustom(source, registry, isCurrent)
            registerStateItems(root.activity.state, source, registry)

            registry.toList()
                .filter { isCurrent || it.options.cascade }
                .forEach { putOnce(it.name, it.value) }
        }

        // core: Canonical framework properties claim their keys before customizable sources.
        putOnce(root.activity.role, activity.role)
        putOnce(root.activity.depth, activities.lastIndex)
        putOnce(root.activity.path, activities.asReversed().joinToString("/") { it.name })
        putOnce(root.activity.name, activity.name)
        putOnce(root.activity.tags, activity.tags.takeIf { it.isNotEmpty() })
        putOnce(root.activity.durationMs, (activity as? Activity.Buzz)?.durationMs)
        putOnce(root.activity.status.code, status.code)
        putOnce(root.activity.status.role, (status as? ActivityStatusRole)?.role)

        // core: A null trace context means the resolved configuration omitted trace publication.
        if (traceContext != null) {
            putOnce(root.traceId, traceContext.traceId)
            putOnce(root.spanId, traceContext.spanId)
            putOnce(root.parentSpanId, traceContext.parentSpanId)
        }

        // core: Status values are more specific than activity values.
        collect(status, isCurrent = true)

        // core: Natural scope iteration runs from the current activity toward the root.
        activities.withIndex().forEach { (index, item) ->
            collect(item, isCurrent = index == 0)
        }
    }

internal fun registerStateItems(
    prefix: PropertyName,
    source: Any,
    registry: LogPropertyRegistry,
) {
    // core: StateItem properties are relative to the activity-state namespace.
    findAnnotatedProperties<StateItem>(source)
        .forEach { property ->
            // core: An omitted annotation name falls back to the Kotlin property name.
            val name = prefix + (property.annotation.name.nullIfUnset() ?: property.name).let(PropertyName::parse)
            val value = property.value(source)

            // core: The annotation chooses whether this value may cross a scope boundary.
            registry.register(name, value) { cascade = property.annotation.cascade }
        }
}
