package wiretap.meta

import wiretap.util.ActivityScope

object ActivityScopeAmbient {
    private val current = ThreadLocal<ActivityScope<*>?>()

    fun current(): ActivityScope<*>? =
        current.get()

    fun push(scope: ActivityScope<*>): AutoCloseable {
        val previous = install(scope)

        // meta: Closing restores the previous scope so nested use blocks unwind in stack order.
        return AutoCloseable {
            restorePrevious(previous)
        }
    }

    // meta: Coroutine context elements install a captured scope on whichever thread resumes the coroutine.
    fun install(scope: ActivityScope<*>?): ActivityScope<*>? {
        val previous = current.get()
        set(scope)
        return previous
    }

    // meta: Restoring null is intentional; it clears threads that had no ambient scope before this coroutine ran.
    fun restorePrevious(scope: ActivityScope<*>?) {
        set(scope)
    }

    private fun set(scope: ActivityScope<*>?) {
        if (scope == null) {
            current.remove()
        } else {
            current.set(scope)
        }
    }
}
