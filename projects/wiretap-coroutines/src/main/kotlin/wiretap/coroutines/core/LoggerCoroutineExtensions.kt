package wiretap.coroutines.core

import kotlinx.coroutines.CoroutineScope
import org.slf4j.Logger
import wiretap.slf4j.util.LoggerAdapter
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.BulkScope
import wiretap.util.BuzzScope

suspend fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    traceId: String? = null,
    block: suspend CoroutineScope.(BuzzScope<A>) -> R,
): R =
    LoggerAdapter(this).beginBuzz(activity, traceId, block)

suspend fun <A : Activity.Buzz, R> Logger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: suspend CoroutineScope.(BuzzScope<A>) -> R,
): R =
    LoggerAdapter(this).beginBuzz(activity, parent, traceId, block)

suspend fun <B : Activity.Bulk<I>, I : Activity.BulkItem, R> Logger.beginBulk(
    activity: B,
    traceId: String? = null,
    block: suspend CoroutineScope.(BulkScope<B, I>) -> R,
): R =
    LoggerAdapter(this).beginBulk(activity, traceId, block)

suspend fun <B : Activity.Bulk<I>, I : Activity.BulkItem, R> Logger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: suspend CoroutineScope.(BulkScope<B, I>) -> R,
): R =
    LoggerAdapter(this).beginBulk(activity, parent, traceId, block)
