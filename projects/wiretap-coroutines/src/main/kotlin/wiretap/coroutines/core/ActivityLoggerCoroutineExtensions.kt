package wiretap.coroutines.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import wiretap.core.beginBulk as beginBulkScope
import wiretap.core.beginBuzz as beginBuzzScope
import wiretap.coroutines.meta.ActivityScopeContextElement
import wiretap.util.Activity
import wiretap.util.logging.ActivityLogger
import wiretap.util.ActivityScope
import wiretap.util.BulkScope
import wiretap.util.BuzzScope
import wiretap.util.ItemScope

suspend fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    traceId: String? = null,
    block: suspend CoroutineScope.(BuzzScope<A>) -> R,
): R =
    this.beginBuzzScope(activity, traceId).runInActivityContext(block)

suspend fun <A : Activity.Buzz, R> ActivityLogger.beginBuzz(
    activity: A,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: suspend CoroutineScope.(BuzzScope<A>) -> R,
): R =
    this.beginBuzzScope(activity, parent, traceId).runInActivityContext(block)

suspend fun <B : Activity.Bulk<I>, I : Activity.BulkItem, R> ActivityLogger.beginBulk(
    activity: B,
    traceId: String? = null,
    block: suspend CoroutineScope.(BulkScope<B, I>) -> R,
): R =
    this.beginBulkScope(activity, traceId).runInActivityContext(block)

suspend fun <B : Activity.Bulk<I>, I : Activity.BulkItem, R> ActivityLogger.beginBulk(
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
    block: suspend CoroutineScope.(BulkScope<B, I>) -> R,
): R =
    this.beginBulkScope(activity, parent, traceId).runInActivityContext(block)

suspend fun <B : Activity.Bulk<I>, I : Activity.BulkItem, R> BulkScope<B, I>.beginItem(
    activity: I,
    block: suspend CoroutineScope.(ItemScope<I>) -> R,
): R =
    beginItem(activity).runInActivityContext(block)

private suspend fun <S : ActivityScope<*>, R> S.runInActivityContext(
    block: suspend CoroutineScope.(S) -> R,
): R {
    try {
        return withContext(ActivityScopeContextElement(this)) {
            coroutineScope {
                block(this@runInActivityContext)
            }
        }
    } finally {
        close()
    }
}
