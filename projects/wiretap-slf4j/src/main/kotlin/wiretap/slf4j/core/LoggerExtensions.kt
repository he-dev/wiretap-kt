package wiretap.slf4j.core

import org.slf4j.Logger
import wiretap.core.beginBulk
import wiretap.core.beginBuzz
import wiretap.core.logSnap
import wiretap.slf4j.util.LoggerAdapter
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.BulkScope
import wiretap.util.BulkScopeProfile
import wiretap.util.BuzzScope

fun <A : Activity.Buzz> Logger.beginBuzz(
    activity: A,
    traceId: String? = null,
): BuzzScope<A> =
    LoggerAdapter(this).beginBuzz(activity, traceId)

fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    traceId: String? = null,
    block: BuzzScope<A>.() -> R,
): R =
    LoggerAdapter(this).beginBuzz(activity, traceId, block)

fun <A : Activity.Buzz> Logger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    traceId: String? = null,
): BuzzScope<A> =
    LoggerAdapter(this).beginBuzz(activity, parent, traceId)

fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: BuzzScope<A>.() -> R,
): R =
    LoggerAdapter(this).beginBuzz(activity, parent, traceId, block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> Logger.beginBulk(
    activity: B,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
): BulkScope<B, I> =
    LoggerAdapter(this).beginBulk(activity, profile, traceId)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> Logger.beginBulk(
    activity: B,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
    block: BulkScope<B, I>.() -> R,
): R =
    LoggerAdapter(this).beginBulk(activity, profile, traceId, block)

fun <B : Activity.Bulk<I>, I : Activity.Buzz> Logger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
): BulkScope<B, I> =
    LoggerAdapter(this).beginBulk(activity, parent, profile, traceId)

fun <B : Activity.Bulk<I>, I : Activity.Buzz, R> Logger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    profile: BulkScopeProfile = BulkScopeProfile.from(activity::class),
    traceId: String? = null,
    block: BulkScope<B, I>.() -> R,
): R =
    LoggerAdapter(this).beginBulk(activity, parent, profile, traceId, block)

fun <A : Activity.Snap> Logger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
) {
    LoggerAdapter(this).logSnap(activity, status)
}

fun <A : Activity.Snap> Logger.logSnap(
    activity: A,
    status: ActivityStatus<A>,
    parent: ActivityScope<*>?,
) {
    LoggerAdapter(this).logSnap(activity, status, parent)
}
