package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.util.DottedName
import wiretap.util.FeatureMap
import wiretap.util.SnippetMap
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.status
import wiretap.util.text
import wiretap.util.buzz.addActivityDuration
import wiretap.util.buzz.addActivity
import wiretap.util.buzz.composeMessage
import wiretap.util.wiretap

class ComposeMessageTest {
    @Test
    fun configuresMessageCompositionWithReceiverDsl() {
        val activity = TestActivity().apply {
            setStatus(TestActivity.Okay())
        }
        val composeMessage = composeMessage {
            remarks {
                addActivity()
                addActivityDuration()
                discrete(DottedName("custom"), this[DottedName("value")]) { label = "Custom" }
            }
            arrange {
                this.add(root.activity.name)
                this.add(root.activity.durationMs)
                addRemaining()
            }
            join {
                joinToString("; ") { it.text }
            }
        }

        val features = FeatureMap().apply {
            put(DottedName("value"), "configured")
            put(DottedName("wiretap.activity.name"), "TestActivity")
            put(DottedName("wiretap.activity.status.code"), "Okay")
        }
        val snippets = composeMessage(
            root = DottedName("wiretap"),
            features = features,
            snippets = SnippetMap(),
            activity = activity,
        )

        assertEquals("TestActivity[Okay]; Duration: N/A; Custom: configured", composeMessage.join(snippets))
    }

    @Test
    fun keepsRemainingPartsInInsertionOrder() {
        val activity = TestActivity().apply {
            setStatus(TestActivity.Okay())
        }
        val root = DottedName().wiretap
        val composeMessage = composeMessage {
            remarks {
                addActivity()
                addActivityDuration()
                discrete(DottedName("second"), "Second")
                discrete(DottedName("first"), "First")
            }
            arrange {
                this.add(root.activity.name)
                this.add(root.activity.durationMs)
                addRemaining()
            }
            join {
                joinToString("; ") { it.text }
            }
        }

        val features = FeatureMap().apply {
            put(root.activity.name, "TestActivity")
            put(root.activity.status.code, "Okay")
        }
        val snippets = composeMessage(
            root = root,
            features = features,
            snippets = SnippetMap(),
            activity = activity,
        )

        assertEquals("TestActivity[Okay]; Duration: N/A; Second; First", composeMessage.join(snippets))
    }

    private class TestActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<TestActivity>()
    }
}
