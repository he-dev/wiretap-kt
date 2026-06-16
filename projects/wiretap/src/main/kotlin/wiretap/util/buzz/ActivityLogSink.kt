package wiretap.util.buzz

import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel

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
            val root = PropertyName()

            val pushState = PushStateItem { key, value ->
                if (value != null) {
                    state[key] = value
                }
            }

            scope.stateItems(root, pushState)
            status.stateItems(root, pushState)

            if (scope.activity is StateItemFeed) {
                scope.activity.stateItems(root, pushState)
            }

            val message = composeMessage(scope, status)

            return ActivityLogRecord(
                scope = scope,
                status = status,
                level = status.level,
                stateItems = state,
                message = message,
            )
        }

        private fun composeMessage(
            scope: ActivityScope<*>,
            status: ActivityStatus<*>,
        ): String {
            val parts = mutableListOf("${scope.activity.name}[${status.code}]")

            if (scope.durationMs != null) {
                parts += "Duration: ${scope.durationMs} ms"
            } else {
                parts += "Duration: N/A"
            }

            val exception = status.exception
            if (exception?.message != null) {
                parts += exception.message.toString()
            }

            return parts.joinToString("; ")
        }
    }
}
