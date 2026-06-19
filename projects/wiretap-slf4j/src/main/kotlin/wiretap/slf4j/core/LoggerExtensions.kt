package wiretap.slf4j.core

import org.slf4j.Logger
import wiretap.core.beginBulk
import wiretap.core.beginBuzz
import wiretap.core.logSnap
import wiretap.slf4j.util.ActivityLogger
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkScope
import wiretap.util.BuzzScope
import wiretap.util.StatusLogOption
import wiretap.util.bothStatusLogOptions

fun <A : Activity.Buzz> Logger.beginBuzz(
    activity: A,
): BuzzScope<A> =
    ActivityLogger(this).beginBuzz(activity)

fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    block: BuzzScope<A>.() -> R,
): R =
    ActivityLogger(this).beginBuzz(activity, block)

fun <A : Activity.Buzz> Logger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
): BuzzScope<A> =
    ActivityLogger(this).beginBuzz(activity, parent, statusLogOptions)

fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    block: BuzzScope<A>.() -> R,
): R =
    ActivityLogger(this).beginBuzz(activity, parent, statusLogOptions, block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> Logger.beginBulk(
    activity: B,
): BulkScope<B, I> =
    ActivityLogger(this).beginBulk(activity)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> Logger.beginBulk(
    activity: B,
    block: BulkScope<B, I>.() -> R,
): R =
    ActivityLogger(this).beginBulk(activity, block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> Logger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
): BulkScope<B, I> =
    ActivityLogger(this).beginBulk(activity, parent, statusLogOptions)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> Logger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    block: BulkScope<B, I>.() -> R,
): R =
    ActivityLogger(this).beginBulk(activity, parent, statusLogOptions, block)

fun <A : Activity.Snap> Logger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
) {
    ActivityLogger(this).logSnap(activity, status)
}

fun <A : Activity.Snap> Logger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
    parent: ActivityScope<*>?,
) {
    ActivityLogger(this).logSnap(activity, status, parent)
}
