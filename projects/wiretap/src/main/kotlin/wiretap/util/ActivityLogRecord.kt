package wiretap.util

import wiretap.util.buzz.ComposeMessageByAppending
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.activity
import wiretap.util.buzz.state
import wiretap.util.buzz.wiretap

data class ActivityLogRecord(
    val scope: ActivityScope<*>,
    val status: ActivityStatus<*>,
    val level: ActivityStatusLevel,
    val stateItems: Map<String, Any?>,
    val message: String,
) {
    companion object {
        fun from(scope: ActivityScope<*>, status: ActivityStatus<*>): ActivityLogRecord {
            val state = linkedMapOf<String, Any?>()
            val root = PropertyName().wiretap.activity
            val composeMessage = ComposeMessageByAppending()

            val pushState = AddLogProperty { key, value ->
                value?.let { 
                    state[key] = it
                }
            }

            AnnotatedStateItems.pushFromAncestors(
                root.state,
                pushState,
                scope.toSequence().filter { it !== scope }.map { it.activity },
            )

            // core: Scope/status metadata is written before activity annotations so explicit activity state can win.
            scope.logProperties(root, pushState)
            status.logProperties(root, pushState)

            scope.activity.logProperties(root, pushState)

            AnnotatedStateItems.pushFromSelf(root.state, pushState, scope.activity)
            AnnotatedStateItems.pushFromSelf(root.state, pushState, status)

            val message = composeMessage(state, scope, scope.activity, status)

            return ActivityLogRecord(
                scope = scope,
                status = status,
                level = status.level,
                stateItems = state,
                message = message,
            )
        }
    }
}
