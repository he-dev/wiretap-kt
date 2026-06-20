package wiretap.core

import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkScope
import wiretap.util.BulkScopeProfile
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

fun <B : Activity.Bulk<I>, I : Activity.Buzz> ActivityLogger.beginBulk(
    activity: B,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
): BulkScope<B, I> =
    beginBulk(activity, ActivityScope.current(), profile, traceId)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> ActivityLogger.beginBulk(
    activity: B,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
    block: BulkScope<B, I>.() -> R,
): R =
    beginBulk(activity, profile, traceId).use(block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
): BulkScope<B, I> =
    BulkScope.push(this, activity, parent, profile, traceId)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
    block: BulkScope<B, I>.() -> R,
): R =
    beginBulk(activity, parent, profile, traceId).use(block)

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
