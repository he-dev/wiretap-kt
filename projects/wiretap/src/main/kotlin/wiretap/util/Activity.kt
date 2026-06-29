package wiretap.util

import wiretap.util.data.Detail
import wiretap.util.data.Remark
import kotlin.time.TimeMark
import kotlin.time.TimeSource

abstract class Activity protected constructor(
    val role: String,
) {
    internal lateinit var status: ActivityStatus<*>
        private set

    open val name: String
        get() = this::class.simpleName!!

    open val tags: Set<String> = emptySet()

    internal open fun setStatus(status: ActivityStatus<*>): Boolean {
        if (::status.isInitialized && this.status is Last) return false

        this.status = status
        return true
    }

    abstract class Buzz internal constructor(role: String) : Activity(role) {
        private lateinit var startedAt: TimeMark

        var durationMs: Long = 0
            private set

        protected constructor() : this("buzz")

        internal fun start() {
            startedAt = TimeSource.Monotonic.markNow()
        }

        internal override fun setStatus(status: ActivityStatus<*>): Boolean {
            if (!super.setStatus(status)) return false

            durationMs = startedAt.elapsedNow().inWholeMilliseconds
            return true
        }
    }

    abstract class Snap : Activity("snap")

    abstract class BulkItem protected constructor(
        val omitStatuses: Set<OmitStatus> = emptySet(),
    ) : Buzz("item") {
        protected constructor(vararg omitStatuses: OmitStatus) : this(omitStatuses.toSet())
    }

    abstract class Bulk<I : BulkItem> : Buzz("bulk") {
        // core: Bulk calculations live with their activity, so publication needs no scope-specific path.
        internal val math = BulkMath()

        @Detail("bulk.item_count")
        @Remark(label = "Item Count")
        val itemCount: Int
            get() = math.itemCount

        @Detail("bulk.duration_s")
        @Remark(label = "Item Duration", format = "%.3f s")
        val durationS: Double
            get() = math.durationMs / 1_000.0

        @Detail("bulk.throughput_s")
        @Remark(label = "Throughput", format = "%.1f/s")
        val throughputS: Double
            get() = math.throughputMs * 1_000.0
    }
}
