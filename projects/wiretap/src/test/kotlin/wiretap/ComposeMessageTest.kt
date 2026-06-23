package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.util.PropertyName
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.status
import wiretap.util.buzz.activityDuration
import wiretap.util.buzz.activityHeader
import wiretap.util.buzz.composeMessage
import wiretap.util.wiretap

class ComposeMessageTest {
    @Test
    fun configuresMessageCompositionWithReceiverDsl() {
        val activity = TestActivity().apply {
            setStatus(TestActivity.Okay())
        }
        val composeMessage = composeMessage {
            include {
                activityHeader()
                activityDuration()
                discrete(PropertyName("custom"), this[PropertyName("value")]) { label = "Custom" }
            }
            arrange {
                positional(root.activity.name)
                positional(root.activity.durationMs)
                remaining()
            }
            join {
                joinToString("; ") { it.text }
            }
        }

        val message = composeMessage(
            root = PropertyName("wiretap"),
            properties = mapOf(
                "value" to "configured",
                "wiretap.activity.name" to "TestActivity",
                "wiretap.activity.status.code" to "Okay",
            ),
            activity = activity,
        )

        assertEquals("TestActivity[Okay]; Duration: N/A; Custom: configured", message)
    }

    @Test
    fun keepsRemainingPartsInInsertionOrder() {
        val activity = TestActivity().apply {
            setStatus(TestActivity.Okay())
        }
        val root = PropertyName().wiretap
        val composeMessage = composeMessage {
            include {
                activityHeader()
                activityDuration()
                discrete(PropertyName("second"), "Second")
                discrete(PropertyName("first"), "First")
            }
            arrange {
                positional(root.activity.name)
                positional(root.activity.durationMs)
                remaining()
            }
            join {
                joinToString("; ") { it.text }
            }
        }

        val message = composeMessage(
            root = root,
            properties = mapOf(
                root.activity.name.toString() to "TestActivity",
                root.activity.status.code.toString() to "Okay",
            ),
            activity = activity,
        )

        assertEquals("TestActivity[Okay]; Duration: N/A; Second; First", message)
    }

    private class TestActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<TestActivity>()
    }
}
