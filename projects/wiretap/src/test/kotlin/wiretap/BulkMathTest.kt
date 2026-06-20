package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import wiretap.util.BulkStat
import wiretap.util.BulkMath
import wiretap.util.BulkUnit
import wiretap.util.PropertyName
import wiretap.util.buzz.AddLogProperty

class BulkMathTest {
    @Test
    fun calculatesDurationStatisticsWithWelfordsAlgorithm() {
        val math = BulkMath()

        math.count("Okay", 100)
        math.count("Fail", 200)
        math.count("Okay", 300)

        assertEquals(3, math.itemCount)
        assertEquals(600.0, math.duration)
        assertEquals(100.0, math.durationMinimum)
        assertEquals(300.0, math.durationMaximum)
        assertEquals(200.0, math.durationMean)
        assertEquals(100.0, math.durationStDev)
        assertEquals(5.0, math.throughput)
        assertEquals(2.0 / 3.0, math.rateOf("Okay"))
        assertEquals(1.0 / 3.0, math.rateOf("Fail"))
    }

    @Test
    fun usesZeroForSingleItemDeviation() {
        val math = BulkMath()

        math.count("Okay", 25)

        assertEquals(0.0, math.durationStDev)
    }

    @Test
    fun keepsDurationAndThroughputUnitsIndependent() {
        val math = BulkMath(
            durationUnit = BulkUnit.Seconds,
            throughputUnit = BulkUnit.Minutes,
        )

        math.count("Okay", 120_000)

        assertEquals(120.0, math.duration)
        assertEquals(0.5, math.throughput)
    }

    @Test
    fun omitsPropertiesUntilAnItemCompletes() {
        val properties = linkedMapOf<String, Any?>()

        BulkMath().logProperties(
            PropertyName("wiretap"),
            AddLogProperty(properties::put),
        )

        assertEquals(emptyMap(), properties)
    }

    @Test
    fun publishesBaselinePropertiesWithoutOptIns() {
        val properties = propertiesFrom()

        assertEquals(
            setOf(
                "wiretap.activity.state.bulk.item_count",
                "wiretap.activity.state.bulk.duration_ms",
                "wiretap.activity.state.bulk.throughput_s",
            ),
            properties.keys,
        )
    }

    @Test
    fun publishesStatusStatsIndependently() {
        val counts = propertiesFrom(BulkStat.CountByStatus)
        val rates = propertiesFrom(BulkStat.RateByStatus)

        assertEquals(1, counts["wiretap.activity.state.bulk.okay_count"])
        assertFalse(counts.containsKey("wiretap.activity.state.bulk.okay_rate"))
        assertEquals(1.0, rates["wiretap.activity.state.bulk.okay_rate"])
        assertFalse(rates.containsKey("wiretap.activity.state.bulk.okay_count"))
    }

    @Test
    fun publishesOptedInDurationStats() {
        val properties = propertiesFrom(
            BulkStat.DurationMean,
            BulkStat.DurationExtremes,
            BulkStat.DurationStDev,
        )

        assertEquals(100.0, properties["wiretap.activity.state.bulk.duration_ms_mean"])
        assertEquals(100.0, properties["wiretap.activity.state.bulk.duration_ms_minimum"])
        assertEquals(100.0, properties["wiretap.activity.state.bulk.duration_ms_maximum"])
        assertEquals(0.0, properties["wiretap.activity.state.bulk.duration_ms_std_dev"])
    }

    @Test
    fun publishesConfiguredUnitsInPropertyNames() {
        val properties = linkedMapOf<String, Any?>()
        val math = BulkMath(
            durationUnit = BulkUnit.Seconds,
            throughputUnit = BulkUnit.Minutes,
        )
        math.count("Okay", 120_000)

        math.logProperties(
            PropertyName("wiretap"),
            AddLogProperty(properties::put),
        )

        assertEquals(120.0, properties["wiretap.activity.state.bulk.duration_s"])
        assertEquals(0.5, properties["wiretap.activity.state.bulk.throughput_min"])
    }

    private fun propertiesFrom(
        vararg optedIn: BulkStat,
    ): Map<String, Any?> {
        val properties = linkedMapOf<String, Any?>()
        val math = BulkMath(optedIn.toSet())
        math.count("Okay", 100)
        math.logProperties(
            PropertyName("wiretap"),
            AddLogProperty(properties::put),
        )
        return properties
    }
}
