package wiretap.util

import wiretap.meta.ActivityScopeAmbient

abstract class ActivityScope<A : Activity>(
    protected val logger: ActivityLogger,
    val activity: A,
    val parent: ActivityScope<*>?,
    traceId: String? = null,
) : AutoCloseable, Iterable<ActivityScope<*>> {
    private var ambient: AutoCloseable? = null
    private val variant = Configuration.resolve(activity)

    val traceContext: TraceContext = TraceContext.create(parent?.traceContext, traceId)

    protected open fun push(): ActivityScope<A> {
        // meta: Ambient scope is restored by close, which is why callers must use the returned scope with use.
        ambient = ActivityScopeAmbient.push(this)
        return this
    }

    abstract fun setStatus(status: ActivityStatus<A>);

    protected fun log() {
        logger.log(
            variant.createLogEntry.from(
                activities = map { it.activity },
                traceContext = traceContext.takeIf { variant.attachTraceContext },
            ),
        )
    }

    override fun close() {
        ambient?.close()
        ambient = null
    }

    override fun iterator(): Iterator<ActivityScope<*>> =
        // core: Scope iteration follows the parent chain from the current scope to the root.
        generateSequence(this as ActivityScope<*>?) { it.parent }.iterator()

    companion object {
        fun current(): ActivityScope<*>? =
            ActivityScopeAmbient.current()
    }
}

open class BuzzScope<A : Activity.Buzz>(
    logger: ActivityLogger,
    activity: A,
    parent: ActivityScope<*>?,
    private val onLastStatus: ((String, Long) -> Unit)? = null,
    traceId: String? = null,
) : ActivityScope<A>(logger, activity, parent, traceId) {
    override fun setStatus(status: ActivityStatus<A>) {
        if (!activity.setStatus(status)) {
            Configuration.diagnosticLogger.warnAboutLastStatusOverwrite(activity, status)
        }
    }

    override fun push(): BuzzScope<A> {
        activity.start()
        super.push()
        activity.setStatus(ActivityStatus.Ready<A>())
        if (!omits(OmitStatus.First)) {
            log()
        }

        return this
    }

    override fun close() {
        // core: A buzz without an explicit final status is still logged, so open scopes cannot disappear silently.
        if (activity.status !is Last) {
            activity.setStatus(ActivityStatus.Void<A>())
        }

        if (!omits(OmitStatus.Last)) {
            log()
        }

        onLastStatus?.invoke(activity.status.code, activity.durationMs)
        super.close()
    }

    protected open fun omits(status: OmitStatus): Boolean =
        false

    companion object {
        fun <A : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: A,
            parent: ActivityScope<*>? = ActivityScope.current(),
            traceId: String? = null,
        ): BuzzScope<A> =
            BuzzScope(logger, activity, parent, traceId = traceId).push()
    }
}

class BulkScope<B : Activity.Bulk<I>, I : Activity.Item>(
    logger: ActivityLogger,
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
) : BuzzScope<B>(logger, activity, parent, traceId = traceId) {
    override fun push(): BulkScope<B, I> {
        super.push()
        return this
    }

    fun beginItem(activity: I): ItemScope<I> =
        // core: Items report their final status into the parent bulk math instead of owning a separate summary.
        ItemScope.push(logger, activity, parent = this, this.activity.math)

    fun <R> beginItem(
        activity: I,
        block: (ItemScope<I>) -> R,
    ): R =
        beginItem(activity).use(block)

    companion object {
        fun <B : Activity.Bulk<I>, I : Activity.Item> push(
            logger: ActivityLogger,
            activity: B,
            parent: ActivityScope<*>? = ActivityScope.current(),
            traceId: String? = null,
        ): BulkScope<B, I> =
            BulkScope(logger, activity, parent, traceId).push()
    }
}

class ItemScope<I : Activity.Item>(
    logger: ActivityLogger,
    activity: I,
    parent: ActivityScope<*>?,
    private val math: BulkMath,
) : BuzzScope<I>(
    logger = logger,
    activity = activity,
    parent = parent,
    onLastStatus = math::count,
) {
    override fun push(): ItemScope<I> {
        super.push()
        return this
    }

    override fun omits(status: OmitStatus): Boolean =
        status in activity.resolvedOmitStatuses

    companion object {
        fun <I : Activity.Item> push(
            logger: ActivityLogger,
            activity: I,
            parent: ActivityScope<*>?,
            math: BulkMath,
        ): ItemScope<I> =
            ItemScope(logger, activity, parent, math).push()
    }
}

class SnapScope<A : Activity.Snap>(
    logger: ActivityLogger,
    activity: A,
    parent: ActivityScope<*>?,
) : ActivityScope<A>(logger, activity, parent) {
    override fun push(): SnapScope<A> {
        super.push()
        return this
    }

    override fun setStatus(status: ActivityStatus<A>) {
        if (activity.setStatus(status)) {
            log()
        } else {
            Configuration.diagnosticLogger.warnAboutLastStatusOverwrite(activity, status)
        }
    }

    companion object {
        fun <A : Activity.Snap> push(
            logger: ActivityLogger,
            activity: A,
            parent: ActivityScope<*>? = ActivityScope.current(),
        ): SnapScope<A> =
            SnapScope(logger, activity, parent).push()
    }
}
