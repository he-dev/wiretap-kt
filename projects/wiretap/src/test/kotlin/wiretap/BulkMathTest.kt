package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.BulkMath

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

}
