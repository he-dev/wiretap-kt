package wiretap.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource

class BulkMath : LogPropertySource {
    private val statusCounts = linkedMapOf<String, Int>()
    private var durationM2 = 0.0

    var itemCount: Int = 0
        private set

    var durationMs: Long = 0
        private set

    var durationMsMin: Long? = null
        private set

    var durationMsMax: Long? = null
        private set

    var durationMsMean: Double = 0.0
        private set

    val durationMsStDev: Double
        get() = if (itemCount > 1) sqrt(durationM2 / (itemCount - 1)) else 0.0

    val throughputMs: Double
        get() = if (durationMs > 0) itemCount / durationMs.toDouble() else 0.0

    fun count(code: String, durationMs: Long) {
        // util: Bulk math records item outcomes only after each item reaches its final status.
        itemCount += 1
        this.durationMs += durationMs
        durationMsMin = durationMsMin?.let { min(it, durationMs) } ?: durationMs
        durationMsMax = durationMsMax?.let { max(it, durationMs) } ?: durationMs

        val normalizedCode = code.lowercase(Locale.ROOT)
        statusCounts[normalizedCode] = statusCounts.getOrElse(normalizedCode) { 0 } + 1

        // util: Welford's algorithm tracks variance without retaining every item duration.
        val delta = durationMs.toDouble() - durationMsMean
        durationMsMean += delta / itemCount
        val delta2 = durationMs.toDouble() - durationMsMean
        durationM2 += delta * delta2
    }

    fun rateOf(code: String): Double =
        if (itemCount > 0) {
            statusCounts.getOrDefault(code.lowercase(Locale.ROOT), 0) / itemCount.toDouble()
        } else {
            0.0
        }

    override fun AddLogProperty.logProperties(root: PropertyName) {
        if (itemCount == 0) return

        val bulk = root.activity.state.bulk
        localOnly(bulk.append("item_count"), itemCount)
        localOnly(bulk.append("duration_s"), durationMs / 1_000.0)
        localOnly(bulk.append("throughput_s"), throughputMs * 1_000.0)

        for ((code, count) in statusCounts) {
            localOnly(bulk.append("${code}_count"), count)
            localOnly(bulk.append("${code}_rate"), rateOf(code))
        }

        localOnly(bulk.append("duration_s_mean"), durationMsMean / 1_000.0)
        localOnly(bulk.append("duration_s_min"), durationMsMin?.div(1_000.0))
        localOnly(bulk.append("duration_s_max"), durationMsMax?.div(1_000.0))
        localOnly(bulk.append("duration_s_std_dev"), durationMsStDev / 1_000.0)
    }
}
