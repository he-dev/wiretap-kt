package wiretap.util.buzz

import wiretap.util.Activity
import wiretap.util.ActivityStatusRole
import wiretap.util.Configuration
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
): Map<String, Any?> =
    buildMap {
        val activity = activities.first()
        val status = activity.status
        // core: A null value claims its key without entering the result map.
        val nullValueKeys = mutableSetOf<String>()

        // core: Sources are processed strongest-first, so the first claim owns a key.
        fun putFirst(key: PropertyName, value: Any?) {
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

        // core: The current activity may publish both local and cascading properties.
        val current = object : AddLogProperty {
            override fun localOnly(key: PropertyName, value: Any?) = putFirst(key, value)
            override fun cascading(key: PropertyName, value: Any?) = putFirst(key, value)
        }

        // core: Ancestors may contribute only properties explicitly marked as cascading.
        val inherited = object : AddLogProperty {
            override fun localOnly(key: PropertyName, value: Any?) = Unit
            override fun cascading(key: PropertyName, value: Any?) = putFirst(key, value)
        }

        // core: Interface values are collected before annotations because first claims win.
        fun collectInterface(source: Any?, add: AddLogProperty, isCurrent: Boolean) {
            val propertySource = source as? LogPropertySource ?: return
            if (!isCurrent) {
                with(propertySource) { add.logProperties(root) }
                return
            }

            // note: Duplicate detection is local to one current-interface invocation.
            val seenKeys = mutableSetOf<String>()
            val checked = object : AddLogProperty {
                fun push(key: PropertyName, value: Any?) {
                    if (!seenKeys.add(key.toString())) {
                        Configuration.diagnosticLogger.warnAboutDuplicateLogProperty(source::class, key)
                        return
                    }

                    // core: For the current scope it does not matter whether the value is cascading or local-only.
                    add.localOnly(key, value)
                }

                override fun localOnly(key: PropertyName, value: Any?) = push(key, value)
                override fun cascading(key: PropertyName, value: Any?) = push(key, value)
            }

            with(propertySource) { checked.logProperties(root) }
        }

        fun collect(source: Any?, add: AddLogProperty, isCurrent: Boolean) {
            source ?: return
            collectInterface(source, add, isCurrent)
            addAnnotatedLogProperties(root.activity.state, source, add)
        }

        // core: Canonical framework properties claim their keys before customizable sources.
        current.localOnly(root.activity.role, activity.role)
        current.localOnly(root.activity.depth, activities.lastIndex)
        current.localOnly(root.activity.path, activities.asReversed().joinToString("/") { it.name })
        current.localOnly(root.activity.name, activity.name)
        current.localOnly(root.activity.tags, activity.tags.takeIf { it.isNotEmpty() })
        current.localOnly(root.activity.durationMs, (activity as? Activity.Buzz)?.durationMs)
        current.localOnly(root.activity.status.code, status.code)
        current.localOnly(root.activity.status.role, (status as? ActivityStatusRole)?.role)

        // core: A null trace context means the resolved configuration omitted trace publication.
        if (traceContext != null) {
            current.localOnly(root.traceId, traceContext.traceId)
            current.localOnly(root.spanId, traceContext.spanId)
            current.localOnly(root.parentSpanId, traceContext.parentSpanId)
        }

        // core: Status values are more specific than activity values.
        collect(status, current, true)

        // core: Natural scope iteration runs from the current activity toward the root.
        activities.withIndex().forEach { (index, item) ->
            val add = if (index == 0) current else inherited
            collect(item, add, isCurrent = index == 0)
        }
    }

internal fun addAnnotatedLogProperties(
    prefix: PropertyName,
    source: Any,
    add: AddLogProperty,
) {
    // core: StateItem properties are relative to the activity-state namespace.
    annotatedProperties<StateItem>(source)
        .forEach { property ->
            // meta: An omitted annotation name falls back to the Kotlin property name.
            val name = property.annotation.name.nullIfUnset() ?: property.name
            val value = property.value(source)

            val key = prefix + PropertyName.parse(name)

            // core: The annotation chooses whether this value may cross a scope boundary.
            if (property.annotation.cascade) {
                add.cascading(key, value)
            } else {
                add.localOnly(key, value)
            }
        }
}
