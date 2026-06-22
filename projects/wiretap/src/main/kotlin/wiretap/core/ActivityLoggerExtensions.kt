package wiretap.core

import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkScope
import wiretap.util.BuzzScope
import wiretap.util.SnapScope


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
    traceId: String? = null,
): BuzzScope<A> =
    BuzzScope.push(this, activity, parent, traceId)

fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: BuzzScope<A>.() -> R,
): R =
    beginBuzz(activity, parent, traceId).use(block)

fun <B : Activity.Bulk<I>, I : Activity.Item> ActivityLogger.beginBulk(
    activity: B,
    traceId: String? = null,
): BulkScope<B, I> =
    beginBulk(activity, ActivityScope.current(), traceId)

fun <B : Activity.Bulk<I>, I : Activity.Item, R> ActivityLogger.beginBulk(
    activity: B,
    traceId: String? = null,
    block: (BulkScope<B, I>) -> R,
): R =
    beginBulk(activity, traceId).use(block)

fun <B : Activity.Bulk<I>, I : Activity.Item> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
): BulkScope<B, I> =
    BulkScope.push(this, activity, parent, traceId)

fun <B : Activity.Bulk<I>, I : Activity.Item, R> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: (BulkScope<B, I>) -> R,
): R =
    beginBulk(activity, parent, traceId).use(block)

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
