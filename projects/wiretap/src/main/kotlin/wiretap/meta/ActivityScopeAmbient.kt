package wiretap.meta

import wiretap.util.ActivityScope

internal object ActivityScopeAmbient {
    private val current = ThreadLocal<ActivityScope<*>?>()

    fun current(): ActivityScope<*>? =
        current.get()

    fun push(scope: ActivityScope<*>): AutoCloseable {
        val previous = current.get()
        current.set(scope)

        // meta: Closing restores the previous scope so nested use blocks unwind in stack order.
        return AutoCloseable {
            current.set(previous)
        }
    }
}
