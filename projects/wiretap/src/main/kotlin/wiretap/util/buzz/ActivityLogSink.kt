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
            val composeMessage = ComposeMessageByAppending()

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

            val message = composeMessage(
                state,
                *messageFeeds(scope, status).toTypedArray(),
            )

            return ActivityLogRecord(
                scope = scope,
                status = status,
                level = status.level,
                stateItems = state,
                message = message,
            )
        }

        private fun messageFeeds(
            scope: ActivityScope<*>,
            status: ActivityStatus<*>,
        ): List<MessagePartFeed> =
            buildList {
                add(scope)

                if (scope.activity is MessagePartFeed) {
                    add(scope.activity as MessagePartFeed)
                }

                add(status)
            }
    }
}
