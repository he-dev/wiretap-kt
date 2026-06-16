package wiretap.util

import kotlin.time.TimeSource
import wiretap.meta.ActivityScopeAmbient
import wiretap.util.buzz.GetStateItem
import wiretap.util.buzz.LogPropertyFeed
import wiretap.util.buzz.MessagePartFeed
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushLogProperty
import wiretap.util.buzz.PushMessagePart
import wiretap.util.buzz.code
import wiretap.util.buzz.depth
import wiretap.util.buzz.durationMs
import wiretap.util.buzz.name
import wiretap.util.buzz.path
import wiretap.util.buzz.role
import wiretap.util.buzz.status
import wiretap.util.buzz.tags

interface StackItem {
    fun <A : Activity.Buzz> push(
        logger: ActivityLogger,
        activity: A,
        parent: ActivityScope<*>? = ActivityScope.current(),
        statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions
    ): ActivityScope<A>
}

abstract class ActivityScope<A : Activity>(
    protected val logger: ActivityLogger,
    val activity: A,
    val parent: ActivityScope<*>?,
) : AutoCloseable, LogPropertyFeed, MessagePartFeed {
    private var ambient: AutoCloseable? = null

    val depth: Int
        get() = parent?.depth?.plus(1) ?: 0

    val path: String
        get() = generateSequence(this as ActivityScope<*>?) { it.parent }
            .toList()
            .asReversed()
            .joinToString("/") { it.activity.name }

    protected abstract val role: String

    open val durationMs: Long? = null

    protected open fun push(): ActivityScope<A> {
        ambient = ActivityScopeAmbient.push(this)
        return this
    }

    abstract fun setStatus(status: ActivityStatus<A>);

    protected fun log(status: ActivityStatus<A>) {
        logger.log(ActivityLogRecord.from(this, status))
    }

    override fun logProperties(root: PropertyName, push: PushLogProperty) {
        push(root.name, activity.name)
        push(root.role, role)
        push(root.depth, depth)
        push(root.path, path)

        if (activity.tags.isNotEmpty()) {
            push(root.tags, activity.tags)
        }
    }

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        push("${activity.name}[${get(root.status.code)}]")
    }

    override fun close() {
        ambient?.close()
        ambient = null
    }

    companion object {
        fun current(): ActivityScope<*>? =
            ActivityScopeAmbient.current()
    }
}

open class BuzzScope<A : Activity.Buzz>(
    logger: ActivityLogger,
    activity: A,
    parent: ActivityScope<*>?,
    private val statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    private val onLastStatus: ((ActivityStatus<A>, Long) -> Unit)? = null,
) : ActivityScope<A>(logger, activity, parent) {
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

        if (StatusLogOption.First in statusLogOptions) {
            log(ActivityStatus.Ready())
        }

        return this
    }

    override fun logProperties(root: PropertyName, push: PushLogProperty) {
        super.logProperties(root, push)
        push(root.durationMs, durationMs)
    }

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        super.messageParts(root, get, push)
        push("Duration: %d ms", get(root.durationMs))
    }

    override fun close() {
        val status = lastStatus ?: ActivityStatus.Void<A>().also {
            lastStatus = it
        }

        if (StatusLogOption.Last in statusLogOptions) {
            log(status)
        }

        onLastStatus?.invoke(status, durationMs)
        super.close()
    }

    companion object {
        fun <A : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: A,
            parent: ActivityScope<*>? = ActivityScope.current(),
            statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
        ): BuzzScope<A> =
            BuzzScope(logger, activity, parent, statusLogOptions).push()
    }
}

class BulkScope<B : Activity.Bulk<I>, I : Activity.Buzz>(
    logger: ActivityLogger,
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
) : BuzzScope<B>(logger, activity, parent, statusLogOptions) {
    private val math = BulkMath()

    override val role: String = "bulk"

    override fun push(): BulkScope<B, I> {
        super.push()
        return this
    }

    fun beginItem(activity: I): ItemScope<I> =
        ItemScope.push(logger, activity, parent = this, math, this.activity.itemStatusLogOptions)

    override fun logProperties(root: PropertyName, push: PushLogProperty) {
        super.logProperties(root, push)
        math.logProperties(root, push)
    }

    companion object {
        fun <B : Activity.Bulk<I>, I : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: B,
            parent: ActivityScope<*>? = ActivityScope.current(),
            statusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
        ): BulkScope<B, I> =
            BulkScope(logger, activity, parent, statusLogOptions).push()
    }
}

class ItemScope<I : Activity.Buzz>(
    logger: ActivityLogger,
    activity: I,
    parent: ActivityScope<*>?,
    private val math: BulkMath,
    statusLogOptions: Set<StatusLogOption>,
) : BuzzScope<I>(
    logger = logger,
    activity = activity,
    parent = parent,
    statusLogOptions = statusLogOptions,
    onLastStatus = { status, durationMs -> math.count(status, durationMs) },
) {
    override val role: String = "item"

    override fun push(): ItemScope<I> {
        super.push()
        return this
    }

    companion object {
        fun <I : Activity.Buzz> push(
            logger: ActivityLogger,
            activity: I,
            parent: ActivityScope<*>?,
            math: BulkMath,
            statusLogOptions: Set<StatusLogOption>,
        ): ItemScope<I> =
            ItemScope(logger, activity, parent, math, statusLogOptions).push()
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

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        super.messageParts(root, get, push)
        push("Duration: N/A")
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
