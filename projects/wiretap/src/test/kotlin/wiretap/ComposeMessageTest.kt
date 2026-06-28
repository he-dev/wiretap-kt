package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.DottedName
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.status
import wiretap.util.buzz.addActivityDuration
import wiretap.util.buzz.addActivity
import wiretap.util.buzz.composeMessage
import wiretap.util.wiretap

class ComposeMessageTest {
    @Test
    fun configuresMessageCompositionWithReceiverDsl() {
        val composeMessage = composeMessage {
            remarks {
                addActivity()
                addActivityDuration()
                add(DottedName("custom"), details[DottedName("value")]) { label = "Custom" }
            }
            arrange {
                this.add(root.activity.name)
                this.add(root.activity.durationMs)
                addRemaining()
            }
            join {
                joinToString("; ") { it }
            }
        }

        val details = mutableMapOf<DottedName, Any?>().apply {
            put(DottedName("value"), "configured")
            put(DottedName("wiretap.activity.name"), "TestActivity")
            put(DottedName("wiretap.activity.status.code"), "Okay")
        }
        val message = composeMessage(
            root = DottedName("wiretap"),
            details = details,
            remarks = mutableMapOf(),
        )

        assertEquals("name: TestActivity[Okay]; Duration: N/A; Custom: configured", message)
    }

    @Test
    fun keepsRemainingPartsInInsertionOrder() {
        val root = DottedName().wiretap
        val composeMessage = composeMessage {
            remarks {
                addActivity()
                addActivityDuration()
                add(DottedName("second"), "Second") { label = null }
                add(DottedName("first"), "First") { label = null }
            }
            arrange {
                this.add(root.activity.name)
                this.add(root.activity.durationMs)
                addRemaining()
            }
            join {
                joinToString("; ") { it }
            }
        }

        val details = mutableMapOf<DottedName, Any?>().apply {
            put(root.activity.name, "TestActivity")
            put(root.activity.status.code, "Okay")
        }
        val message = composeMessage(
            root = root,
            details = details,
            remarks = mutableMapOf(),
        )

        assertEquals("name: TestActivity[Okay]; Duration: N/A; second: Second; first: First", message)
    }
}
