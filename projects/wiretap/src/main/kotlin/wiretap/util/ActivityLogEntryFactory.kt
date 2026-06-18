package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.ComposeMessage
import wiretap.util.buzz.ComposeMessageByAppending
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.activity
import wiretap.util.buzz.state
import wiretap.util.buzz.wiretap

fun interface ActivityLogEntryFactory {
    fun create(scope: ActivityScope<*>, status: ActivityStatus<*>): ActivityLogEntry

    companion object {
        fun default(
            composeMessage: ComposeMessage = ComposeMessageByAppending(),
        ): ActivityLogEntryFactory = ActivityLogEntryFactory { scope, status ->
            val logProperties = linkedMapOf<String, Any?>()
            val activityRoot = PropertyName().wiretap.activity
            val addLogProperty = AddLogProperty { key, value ->
                value?.let { logProperties[key] = it }
            }

            // core: Cascaded properties are collected root-first so nearer scopes can override them.
            scope.parents()
                .asReversed()
                .forEach { parent ->
                    AnnotatedStateItems.pushFrom(
                        activityRoot.state,
                        addLogProperty,
                        parent.activity,
                        cascadingOnly = true,
                    )
                }

            // core: Framework scopes and optional user sources share one stable property feed.
            addFrom(scope, activityRoot, addLogProperty)
            addFrom(scope.activity, activityRoot, addLogProperty)
            addFrom(status, activityRoot, addLogProperty)

            AnnotatedStateItems.pushFrom(
                activityRoot.state,
                addLogProperty,
                scope.activity,
                status,
            )

            ActivityLogEntry(
                level = status.level,
                message = composeMessage(logProperties, scope, scope.activity, status),
                properties = logProperties.toMap(),
                exception = status.exception,
            )
        }
    }
}

private fun addFrom(
    source: Any?,
    root: PropertyName,
    addLogProperty: AddLogProperty,
) {
    (source as? LogPropertySource)?.logProperties(root, addLogProperty)
}

private fun ActivityScope<*>.parents(): List<ActivityScope<*>> =
    generateSequence(parent) { it.parent }.toList()
