package wiretap.util

import wiretap.util.buzz.ActivityLogRecord

interface ActivityLogger {
    fun log(record: ActivityLogRecord)
}

fun <A : Activity.Buzz> ActivityLogger.beginBuzz(
    activity: A,
): BuzzScope<A> =
    beginBuzz(activity, ActivityScope.current())

fun <A : Activity.Buzz> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
): BuzzScope<A> =
    BuzzScope.push(this, activity, parent, statusLogOptions)

fun <A : Activity.Snap> ActivityLogger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
) {
    logSnap(activity, status, ActivityScope.current())
}

fun <A : Activity.Snap> ActivityLogger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
    parent: ActivityScope<*>?,
) {
    SnapScope.push(this, activity, parent).use { scope ->
        scope.setStatus(status)
    }
}
