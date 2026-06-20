package wiretap.core

import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkScope
import wiretap.util.BuzzScope
import wiretap.util.SnapScope
import wiretap.util.StatusLogOption
import wiretap.util.bothStatusLogOptions


fun <A : Activity.Buzz> ActivityLogger.beginBuzz(
    activity: A,
    traceId: String? = null,
): BuzzScope<A> =
    beginBuzz(activity, ActivityScope.current(), traceId = traceId)

fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    traceId: String? = null,
    block: BuzzScope<A>.() -> R,
): R =
    beginBuzz(activity, traceId).use(block)

fun <A : Activity.Buzz> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    traceId: String? = null,
): BuzzScope<A> =
    BuzzScope.push(this, activity, parent, statusLogOptions, traceId)

fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    traceId: String? = null,
    block: BuzzScope<A>.() -> R,
): R =
    beginBuzz(activity, parent, statusLogOptions, traceId).use(block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> ActivityLogger.beginBulk(
    activity: B,
): BulkScope<B, I> =
    beginBulk(activity, ActivityScope.current())

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> ActivityLogger.beginBulk(
    activity: B,
    block: BulkScope<B, I>.() -> R,
): R =
    beginBulk(activity).use(block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
): BulkScope<B, I> =
    BulkScope.push(this, activity, parent, statusLogOptions)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    block: BulkScope<B, I>.() -> R,
): R =
    beginBulk(activity, parent, statusLogOptions).use(block)

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
