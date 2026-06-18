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
): BuzzScope<A> =
    beginBuzz(activity, ActivityScope.current())

fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    block: BuzzScope<A>.() -> R,
): R =
    beginBuzz(activity).use(block)

fun <A : Activity.Buzz> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
): BuzzScope<A> =
    BuzzScope.push(this, activity, parent, statusLogOptions)

fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    block: BuzzScope<A>.() -> R,
): R =
    beginBuzz(activity, parent, statusLogOptions).use(block)

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
