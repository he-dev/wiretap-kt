package wiretap.util

import wiretap.meta.ActivityScopeAmbient
import wiretap.util.buzz.*
import kotlin.time.TimeSource

abstract class ActivityScope<A : Activity>(
    protected val logger: ActivityLogger,
    val activity: A,
    val parent: ActivityScope<*>?,
    traceId: String? = null,
) : AutoCloseable, LogPropertySource, Iterable<ActivityScope<*>> {
    private var ambient: AutoCloseable? = null
    private val variant = Configuration.resolve(activity)

    val traceContext: TraceContext = TraceContext.create(parent?.traceContext, traceId)

    val depth: Int
        get() = parent?.depth?.plus(1) ?: 0

    val ancestors: Sequence<ActivityScope<*>>
        get() = asSequence().drop(1)

    val path: String
        get() = reversed().joinToString("/") { it.activity.name }

    protected abstract val role: String

    open val durationMs: Long? = null

    protected open fun push(): ActivityScope<A> {
        // meta: Ambient scope is restored by close, which is why callers must use the returned scope with use.
        ambient = ActivityScopeAmbient.push(this)
        return this
    }

    abstract fun setStatus(status: ActivityStatus<A>);

    protected fun log(status: ActivityStatus<A>) {
        logger.log(variant.createLogEntryBy.from(this, status))
    }

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        // core: Cascade root-first so nearer activities overwrite their ancestors.
        reversed().dropLast(1).forEach { ancestor ->
            addAnnotatedLogProperties(
                root.activity.state,
                ancestor.activity,
                add,
                cascadingOnly = true,
            )
        }

        add(root.activity.role, role)
        add(root.activity.depth, depth)
        add(root.activity.path, path)

        if (variant.attachTraceContext) {
            traceContext.logProperties(root, add)
        }
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
    private val onLastStatus: ((ActivityStatus<A>, Long) -> Unit)? = null,
    traceId: String? = null,
) : ActivityScope<A>(logger, activity, parent, traceId) {
    private val startedAt = TimeSource.Monotonic.markNow()
    private var lastStatus: ActivityStatus<A>? = null

    override val role: String = "buzz"

    override val durationMs: Long
        get() = startedAt.elapsedNow().inWholeMilliseconds

    override fun setStatus(status: ActivityStatus<A>) {
        lastStatus = status
    }

    override fun push(): BuzzScope<A> {
        super.push()
        if (!omits(OmitStatus.First)) {
            log(ActivityStatus.Ready())
        }

        return this
    }

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        super.logProperties(root, add)
        add(root.activity.durationMs, durationMs)
    }

    override fun close() {
        // core: A buzz without an explicit final status is still logged, so open scopes cannot disappear silently.
        val status = lastStatus ?: ActivityStatus.Void<A>().also {
            lastStatus = it
        }

        if (!omits(OmitStatus.Last)) {
            log(status)
        }

        onLastStatus?.invoke(status, durationMs)
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

class BulkScope<B : Activity.Bulk<I>, I : Activity.Buzz>(
    logger: ActivityLogger,
    activity: B,
    parent: ActivityScope<*>?,
    traceId: String? = null,
) : BuzzScope<B>(logger, activity, parent, traceId = traceId) {
    private val math = BulkMath()

    override val role: String = "bulk"

    override fun push(): BulkScope<B, I> {
        super.push()
        return this
    }

    fun beginItem(
        activity: I,
        omitStatuses: Set<OmitStatus> = bulkItemStatusOmissions(activity.javaClass),
    ): ItemScope<I> =
        // core: Items report their final status into the parent bulk math instead of owning a separate summary.
        ItemScope.push(logger, activity, parent = this, math, omitStatuses)

    fun <R> beginItem(
        activity: I,
        omitStatuses: Set<OmitStatus> = bulkItemStatusOmissions(activity.javaClass),
        block: ItemScope<I>.() -> R,
    ): R =
        beginItem(activity, omitStatuses).use(block)

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        super.logProperties(root, add)
        math.logProperties(root, add)
    }

    companion object {
        fun <B : Activity.Bulk<I>, I : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: B,
            parent: ActivityScope<*>? = ActivityScope.current(),
            traceId: String? = null,
        ): BulkScope<B, I> =
            BulkScope(logger, activity, parent, traceId).push()
    }
}

class ItemScope<I : Activity.Buzz>(
    logger: ActivityLogger,
    activity: I,
    parent: ActivityScope<*>?,
    private val math: BulkMath,
    private val omitStatuses: Set<OmitStatus>,
) : BuzzScope<I>(
    logger = logger,
    activity = activity,
    parent = parent,
    onLastStatus = { status, durationMs -> math.count(status.code, durationMs) },
) {
    override val role: String = "item"

    override fun push(): ItemScope<I> {
        super.push()
        return this
    }

    override fun omits(status: OmitStatus): Boolean =
        status in omitStatuses

    companion object {
        fun <I : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: I,
            parent: ActivityScope<*>?,
            math: BulkMath,
            omitStatuses: Set<OmitStatus>,
        ): ItemScope<I> =
            ItemScope(logger, activity, parent, math, omitStatuses).push()
    }
}

class SnapScope<A : Activity.Snap>(
    logger: ActivityLogger,
    activity: A,
    parent: ActivityScope<*>?,
) : ActivityScope<A>(logger, activity, parent) {
    override val role: String = "snap"

    override fun push(): SnapScope<A> {
        super.push()
        return this
    }

    override fun setStatus(status: ActivityStatus<A>) {
        log(status)
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
