package wiretap.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BulkMath {
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

}
