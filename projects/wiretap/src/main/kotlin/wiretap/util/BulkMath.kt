package wiretap.util

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource

class BulkMath(
    private val optedIn: Set<BulkStat> = emptySet(),
    val durationUnit: BulkUnit = BulkUnit.Milliseconds,
    val throughputUnit: BulkUnit = BulkUnit.Seconds,
) : LogPropertySource {
    private val statusCounts = linkedMapOf<String, Int>()
    private var durationMeanValue = 0.0
    private var durationM2 = 0.0

    var itemCount: Int = 0
        private set

    var duration: Double = 0.0
        private set

    var durationMinimum: Double? = null
        private set

    var durationMaximum: Double? = null
        private set

    val durationMean: Double
        get() = durationMeanValue

    val durationStDev: Double
        get() = if (itemCount > 1) sqrt(durationM2 / (itemCount - 1)) else 0.0

    val throughput: Double
        get() {
            val durationForThroughput = durationUnit.convert(duration, throughputUnit)
            return if (durationForThroughput > 0) itemCount / durationForThroughput else 0.0
        }

    fun count(code: String, durationMs: Long) {
        // util: Bulk math records item outcomes only after each item reaches its final status.
        val measuredDuration = durationUnit.fromMilliseconds(durationMs.toDouble())
        itemCount += 1
        duration += measuredDuration
        durationMinimum = durationMinimum?.let { min(it, measuredDuration) } ?: measuredDuration
        durationMaximum = durationMaximum?.let { max(it, measuredDuration) } ?: measuredDuration

        val normalizedCode = code.lowercase(Locale.ROOT)
        statusCounts[normalizedCode] = statusCounts.getOrElse(normalizedCode) { 0 } + 1

        // util: Welford's algorithm tracks variance without retaining every item duration.
        val delta = measuredDuration - durationMeanValue
        durationMeanValue += delta / itemCount
        val delta2 = measuredDuration - durationMeanValue
        durationM2 += delta * delta2
    }

    fun rateOf(code: String): Double =
        if (itemCount > 0) {
            statusCounts.getOrDefault(code.lowercase(Locale.ROOT), 0) / itemCount.toDouble()
        } else {
            0.0
        }

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        if (itemCount == 0) return

        val bulk = root.activity.state.bulk
        val durationName = "duration_${durationUnit.suffix}"
        add(bulk.append("item_count"), itemCount)
        add(bulk.append(durationName), duration)
        add(bulk.append("throughput_${throughputUnit.suffix}"), throughput)

        for ((code, count) in statusCounts) {
            if (BulkStat.CountByStatus in optedIn) {
                add(bulk.append("${code}_count"), count)
            }

            if (BulkStat.RateByStatus in optedIn) {
                add(bulk.append("${code}_rate"), rateOf(code))
            }
        }

        if (BulkStat.DurationMean in optedIn) {
            add(bulk.append("${durationName}_mean"), durationMean)
        }

        if (BulkStat.DurationExtremes in optedIn) {
            add(bulk.append("${durationName}_minimum"), durationMinimum)
            add(bulk.append("${durationName}_maximum"), durationMaximum)
        }

        if (BulkStat.DurationStDev in optedIn) {
            add(bulk.append("${durationName}_std_dev"), durationStDev)
        }
    }
}
