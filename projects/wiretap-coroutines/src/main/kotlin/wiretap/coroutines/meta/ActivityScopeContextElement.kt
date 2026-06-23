package wiretap.coroutines.meta

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import wiretap.meta.ActivityScopeAmbient
import wiretap.util.ActivityScope

/**
 * Carries the current activity scope through coroutine suspension and dispatcher hops.
 *
 * meta: This is infrastructure for coroutine adapters, not an API users should have to install manually.
 */
internal class ActivityScopeContextElement(
    private val captured: ActivityScope<*>?,
) : ThreadContextElement<ActivityScope<*>?> {

    companion object Key : CoroutineContext.Key<ActivityScopeContextElement> {
        fun captureCurrent(): ActivityScopeContextElement =
            ActivityScopeContextElement(ActivityScopeAmbient.current())
    }

    override val key: CoroutineContext.Key<ActivityScopeContextElement>
        get() = Key

    override fun updateThreadContext(context: CoroutineContext): ActivityScope<*>? =
        ActivityScopeAmbient.install(captured)

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: ActivityScope<*>?,
    ) {
        ActivityScopeAmbient.restorePrevious(oldState)
    }
}
