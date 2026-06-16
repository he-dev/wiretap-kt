package wiretap.util

import kotlin.time.TimeSource
import wiretap.meta.ActivityScopeAmbient
import wiretap.util.buzz.GetStateItem
import wiretap.util.buzz.ActivityLogRecord
import wiretap.util.buzz.MessagePartFeed
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushMessagePart
import wiretap.util.buzz.PushStateItem
import wiretap.util.buzz.StateItemFeed
import wiretap.util.buzz.activity
import wiretap.util.buzz.code
import wiretap.util.buzz.depth
import wiretap.util.buzz.durationMs
import wiretap.util.buzz.name
import wiretap.util.buzz.path
import wiretap.util.buzz.role
import wiretap.util.buzz.status
import wiretap.util.buzz.tags

abstract class ActivityScope<A : Activity>(
    protected val logger: ActivityLogger?,
    val activity: A,
    val parent: ActivityScope<*>?,
) : AutoCloseable, StateItemFeed, MessagePartFeed {
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

    protected fun record(status: ActivityStatus<A>) {
        logger?.log(ActivityLogRecord.from(this, status))
    }

    override fun stateItems(name: PropertyName, push: PushStateItem) {
        push(name.activity.name, activity.name)
        push(name.activity.role, role)
        push(name.activity.depth, depth)
        push(name.activity.path, path)

        if (activity.tags.isNotEmpty()) {
            push(name.activity.tags, activity.tags)
        }
    }

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        push("${activity.name}[${get(root.activity.status.code)}]")
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
    logger: ActivityLogger? = null,
    activity: A,
    parent: ActivityScope<*>?,
    private val statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
    private val onLastStatus: ((ActivityStatus<A>, Long) -> Unit)? = null,
) : ActivityScope<A>(logger, activity, parent) {
    private val startedAt = TimeSource.Monotonic.markNow()
    private var lastStatus: ActivityStatus<A>? = null

    override val role: String = "buzz"

    override val durationMs: Long
        get() = startedAt.elapsedNow().inWholeMilliseconds

    val status: ActivityStatus<A>?
        get() = lastStatus

    val logsReadyStatus: Boolean
        get() = StatusLogOption.First in statusLogOptions

    val logsLastStatus: Boolean
        get() = StatusLogOption.Last in statusLogOptions

    fun setStatus(status: ActivityStatus<A>): BuzzScope<A> {
        lastStatus = status
        return this
    }

    override fun push(): BuzzScope<A> {
        super.push()

        if (StatusLogOption.First in statusLogOptions) {
            record(ActivityStatus.Ready())
        }

        return this
    }

    override fun stateItems(name: PropertyName, push: PushStateItem) {
        super.stateItems(name, push)
        push(name.activity.durationMs, durationMs)
    }

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        super.messageParts(root, get, push)
        push("Duration: %d ms", get(root.activity.durationMs))
    }

    override fun close() {
        val status = lastStatus ?: ActivityStatus.Void<A>().also {
            lastStatus = it
        }

        if (StatusLogOption.Last in statusLogOptions) {
            record(status)
        }

        onLastStatus?.invoke(status, durationMs)
        super.close()
    }

    companion object {
        fun <A : Activity.Buzz> push(
            logger: ActivityLogger? = null,
            activity: A,
            parent: ActivityScope<*>? = ActivityScope.current(),
            statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
        ): BuzzScope<A> =
            BuzzScope(logger, activity, parent, statusLogOptions).push()
    }
}

class BulkScope<B : Activity.Bulk<I>, I : Activity.Buzz>(
    logger: ActivityLogger? = null,
    activity: B,
    parent: ActivityScope<*>?,
    statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
) : BuzzScope<B>(logger, activity, parent, statusLogOptions) {
    private val math = BulkMath()

    override val role: String = "bulk"

    override fun push(): BulkScope<B, I> {
        super.push()
        return this
    }

    fun beginItem(activity: I): ItemScope<I> =
        ItemScope.push(logger, activity, parent = this, math, this.activity.itemStatusLogOptions)

    override fun stateItems(name: PropertyName, push: PushStateItem) {
        super.stateItems(name, push)
        math.stateItems(name, push)
    }

    companion object {
        fun <B : Activity.Bulk<I>, I : Activity.Buzz> push(
            logger: ActivityLogger? = null,
            activity: B,
            parent: ActivityScope<*>? = ActivityScope.current(),
            statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
        ): BulkScope<B, I> =
            BulkScope(logger, activity, parent, statusLogOptions).push()
    }
}

class ItemScope<I : Activity.Buzz>(
    logger: ActivityLogger? = null,
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
            logger: ActivityLogger? = null,
            activity: I,
            parent: ActivityScope<*>?,
            math: BulkMath,
            statusLogOptions: Set<StatusLogOption>,
        ): ItemScope<I> =
            ItemScope(logger, activity, parent, math, statusLogOptions).push()
    }
}

class SnapScope<A : Activity.Snap>(
    logger: ActivityLogger? = null,
    activity: A,
    parent: ActivityScope<*>?,
) : ActivityScope<A>(logger, activity, parent) {
    override val role: String = "snap"

    override fun push(): SnapScope<A> {
        super.push()
        return this
    }

    fun setStatus(status: ActivityStatus<A>): SnapScope<A> {
        record(status)
        return this
    }

    override fun messageParts(root: PropertyName, get: GetStateItem, push: PushMessagePart) {
        super.messageParts(root, get, push)
        push("Duration: N/A")
    }

    companion object {
        fun <A : Activity.Snap> push(
            logger: ActivityLogger? = null,
            activity: A,
            parent: ActivityScope<*>? = ActivityScope.current(),
        ): SnapScope<A> =
            SnapScope(logger, activity, parent).push()
    }
}
