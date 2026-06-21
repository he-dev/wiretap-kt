package wiretap.util.buzz

import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusRole
import wiretap.util.BulkScope
import wiretap.util.Configuration
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.StatusSnapshot
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

fun getLogProperties(
    root: PropertyName,
    scope: ActivityScope<*>,
    status: ActivityStatus<*>,
    vararg propertySources: Any?,
): Map<String, Any?> =
    buildMap {
        val add = object : AddLogProperty {
            override fun localOnly(key: PropertyName, value: Any?) {
                value?.let { put(key.toString(), it) }
            }

            override fun cascading(key: PropertyName, value: Any?) {
                localOnly(key, value)
            }
        }
        val cascading = add.cascadingOnly()

        fun collect(source: Any?, target: AddLogProperty = add) {
            source ?: return

            (source as? LogPropertySource)?.let {
                with(it) { target.logProperties(root) }
            }
            addAnnotatedLogProperties(root.activity.state, source, target)
        }

        // core: Ancestors cascade root-first so nearer values overwrite earlier ones.
        scope.reversed().dropLast(1).forEach { ancestor ->
            collect(ancestor, cascading)
            collect(ancestor.activity, cascading)
        }

        add.localOnly(root.activity.role, scope.role)
        add.localOnly(root.activity.depth, scope.depth)
        add.localOnly(root.activity.path, scope.path)

        if (Configuration.resolve(scope.activity).attachTraceContext) {
            val trace = scope.traceContext
            add.localOnly(root.traceId, trace.traceId)
            add.localOnly(root.spanId, trace.spanId)
            trace.parentSpanId?.let { add.localOnly(root.parentSpanId, it) }
        }

        collect(scope)
        (scope as? BulkScope<*, *>)?.math?.let(::collect)
        propertySources.forEach(::collect)
        collect(scope.activity)
        collect(status)

        add.localOnly(root.activity.name, scope.activity.name)
        if (scope.activity.tags.isNotEmpty()) {
            add.localOnly(root.activity.tags, scope.activity.tags)
        }
        add.localOnly(root.activity.status.code, status.code)
        add.localOnly(root.activity.status.role, (status as? ActivityStatusRole)?.role)

        propertySources
            .filterIsInstance<StatusSnapshot<*>>()
            .lastOrNull()
            ?.let { add.localOnly(root.activity.durationMs, it.durationMs) }
    }

internal fun addAnnotatedLogProperties(
    prefix: PropertyName,
    source: Any,
    add: AddLogProperty,
) {
    annotatedProperties<StateItem>(source)
        .forEach { property ->
            val name = property.annotation.name.nullIfUnset() ?: property.name
            val value = property.value(source)

            if (property.annotation.cascade) {
                add.cascading(prefix.append(name), value)
            } else {
                add.localOnly(prefix.append(name), value)
            }
        }
}

internal fun AddLogProperty.cascadingOnly(): AddLogProperty =
    object : AddLogProperty {
        override fun localOnly(key: PropertyName, value: Any?) = Unit

        override fun cascading(key: PropertyName, value: Any?) {
            this@cascadingOnly.cascading(key, value)
        }
    }
