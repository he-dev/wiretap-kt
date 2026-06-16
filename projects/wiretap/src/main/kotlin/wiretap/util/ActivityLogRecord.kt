package wiretap.util

import wiretap.util.buzz.ComposeMessageByAppending
import wiretap.util.buzz.MessagePartFeed
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushLogProperty
import wiretap.util.buzz.LogPropertyFeed
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

            val pushState = PushLogProperty { key, value ->
                if (value != null) {
                    state[key] = value
                }
            }

            scope.logProperties(root, pushState)
            status.logProperties(root, pushState)

            if (scope.activity is LogPropertyFeed) {
                scope.activity.logProperties(root, pushState)
            }

            AnnotatedStateItems.pushFrom(root.state, pushState, scope.activity, status)

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
