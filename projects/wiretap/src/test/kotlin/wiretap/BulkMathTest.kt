package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.BulkMath
import wiretap.util.PropertyName
import wiretap.util.buzz.getLogProperties

class BulkMathTest {
    @Test
    fun calculatesDurationStatisticsInMilliseconds() {
        val math = BulkMath()

        math.count("Okay", 100)
        math.count("Fail", 200)
        math.count("Okay", 300)

        assertEquals(3, math.itemCount)
        assertEquals(600L, math.durationMs)
        assertEquals(100L, math.durationMsMin)
        assertEquals(300L, math.durationMsMax)
        assertEquals(200.0, math.durationMsMean)
        assertEquals(100.0, math.durationMsStDev)
        assertEquals(0.005, math.throughputMs)
        assertEquals(2.0 / 3.0, math.rateOf("Okay"))
        assertEquals(1.0 / 3.0, math.rateOf("Fail"))
    }

    @Test
    fun usesZeroForSingleItemDeviation() {
        val math = BulkMath()

        math.count("Okay", 25)

        assertEquals(0.0, math.durationMsStDev)
    }

    @Test
    fun omitsPropertiesUntilAnItemCompletes() {
        val properties = getLogProperties(
            PropertyName("wiretap"),
            BulkMath(),
        )

        assertEquals(emptyMap(), properties)
    }

    @Test
    fun publishesAllBulkProperties() {
        val math = BulkMath()
        math.count("Okay", 100)
        val properties = getLogProperties(
            PropertyName("wiretap"),
            math,
        )

        assertEquals(1, properties["wiretap.activity.state.bulk.item_count"])
        assertEquals(0.1, properties["wiretap.activity.state.bulk.duration_s"])
        assertEquals(10.0, properties["wiretap.activity.state.bulk.throughput_s"])
        assertEquals(1, properties["wiretap.activity.state.bulk.okay_count"])
        assertEquals(1.0, properties["wiretap.activity.state.bulk.okay_rate"])
        assertEquals(0.1, properties["wiretap.activity.state.bulk.duration_s_mean"])
        assertEquals(0.1, properties["wiretap.activity.state.bulk.duration_s_min"])
        assertEquals(0.1, properties["wiretap.activity.state.bulk.duration_s_max"])
        assertEquals(0.0, properties["wiretap.activity.state.bulk.duration_s_std_dev"])
    }
}
