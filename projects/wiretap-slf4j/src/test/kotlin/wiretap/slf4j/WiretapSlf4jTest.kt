package wiretap.slf4j

import kotlin.test.Test
import kotlin.test.assertNotNull
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.core.beginBuzz
import org.slf4j.helpers.NOPLogger
import wiretap.slf4j.core.ActivityLogger

class WiretapSlf4jTest {
    @Test
    fun beginsBuzzWithSlf4jLogger() {
        val adapter = ActivityLogger(NOPLogger.NOP_LOGGER)

        adapter.beginBuzz(TestActivity()) {
            setStatus(TestActivity.Okay())
        }

        assertNotNull(adapter)
    }

    private class TestActivity : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<TestActivity>()
    }
}
