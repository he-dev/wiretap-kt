package wiretap.slf4j

import kotlin.test.Test
import kotlin.test.assertNotNull
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import org.slf4j.helpers.NOPLogger
import wiretap.slf4j.core.beginBuzz

class WiretapSlf4jTest {
    @Test
    fun beginsBuzzWithSlf4jLogger() {
        val logger = NOPLogger.NOP_LOGGER

        logger.beginBuzz(TestActivity()) {
            setStatus(TestActivity.Okay())
        }

        assertNotNull(logger)
    }

    private class TestActivity : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<TestActivity>()
    }
}
