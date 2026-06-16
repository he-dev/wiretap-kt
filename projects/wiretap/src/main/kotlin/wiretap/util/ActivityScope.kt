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
    private val logger: ActivityLogger?,
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

class BuzzScope<A : Activity.Buzz>(
    logger: ActivityLogger? = null,
    activity: A,
    parent: ActivityScope<*>?,
    private val statusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
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

    override fun close() {
        val status = lastStatus ?: ActivityStatus.Void<A>().also {
            lastStatus = it
        }

        if (StatusLogOption.Last in statusLogOptions) {
            record(status)
        }

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

    override fun stateItems(name: PropertyName, push: PushStateItem) {
        super.stateItems(name, push)
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
