package wiretap.util

import wiretap.meta.ActivityScopeAmbient
import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.data.Detail
import wiretap.util.data.DetailBuilder
import wiretap.util.data.DetailMap
import wiretap.util.data.DetailSource
import wiretap.util.data.Remark
import wiretap.util.data.RemarkBuilder
import wiretap.util.data.RemarkMap
import wiretap.util.data.RemarkSource
import wiretap.util.logging.ActivityLogger

abstract class ActivityScope<A : Activity>(
    protected val logger: ActivityLogger,
    val activity: A,
    val parent: ActivityScope<*>?,
    traceId: String? = null,
) : AutoCloseable, Iterable<ActivityScope<*>> {
    private var ambient: AutoCloseable? = null
    private val configuration = Configuration.resolve(activity)

    val traceContext: TraceContext = TraceContext.create(parent?.traceContext, traceId)

    protected open fun push(): ActivityScope<A> {
        // meta: Ambient scope is restored by close, which is why callers must use the returned scope with use.
        ambient = ActivityScopeAmbient.push(this)
        return this
    }

    abstract fun setStatus(status: ActivityStatus<A>);

    protected fun log() {

        val root = configuration.root
        val activities = map { it.activity }

        // core: Initialize with default values.
        val details = DetailMap().apply {
            put(root.activity.name, activity.name)
            put(root.activity.status.code, activity.status.code)
            put(root.activity.status.role, (activity.status as? ActivityStatusRole)?.role)

            put(root.activity.role, activity.role)
            put(root.activity.depth, activities.lastIndex)
            put(root.activity.path, activities.asReversed().joinToString("/") { it.name })
            put(root.activity.tags, activity.tags.takeIf { it.isNotEmpty() })
            put(root.activity.durationMs, (activity as? Activity.Buzz)?.durationMs)

            // core: A null trace context means the resolved configuration omitted trace publication.
            put(root.traceId, traceContext.traceId)
            put(root.spanId, traceContext.spanId)
            put(root.parentSpanId, traceContext.parentSpanId)

        }

        val remarks = RemarkMap()

        // core: Scan for details first; Single pass.
        activities.forEachIndexed { level, source ->

            // note: Each level needs its own builder.
            val builder = DetailBuilder(root.activity.state, level, details)

            // core: Check for interfaces first as they have priority.
            when (source) {
                is DetailSource -> with(source) {
                    builder.details()
                }
            }

            // note: Scan for annotations second.
            findAnnotatedProperties<Detail>(source).forEach { property ->
                val annotation = property.annotation
                val name = annotation.name.ifEmpty { property.name }
                builder.add(DottedName(name), property.value(source)) {
                    cascade = annotation.cascade
                }
            }
        }


        // core: Scan for remarks in the same order as the details; Single pass.
        for (source in listOf(activity, activity.status)) {
            val builder = RemarkBuilder(root, details, remarks)
            when (source) {
                is RemarkSource -> with(source) {
                    builder.remarks()
                }
            }
            findAnnotatedProperties<Remark>(source)
                .forEach { property ->
                    //report(property.name, property.value(source), property.annotation)
                    builder.add(DottedName(property.name), property.value(source)) {
                        label = property.annotation.label
                        separator = property.annotation.separator
                        format = property.annotation.format
                    }
                }
        }

        val message = configuration.composeMessage(root, details, remarks)
        logger.log(
            activity.status.level,
            details.asSequence()
                .mapNotNull { (name, value) -> value?.let { name.toString() to it } }
                .toMap(),
            message,
            activity.status.exception,
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
