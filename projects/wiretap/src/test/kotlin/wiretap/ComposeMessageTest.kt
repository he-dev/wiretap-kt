package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName
import wiretap.util.buzz.composeMessageBy

class ComposeMessageTest {
    @Test
    fun configuresMessageCompositionWithReceiverDsl() {
        val composeMessage = composeMessageBy {
            messageParts {
                push(
                    PropertyName("custom"),
                    this[PropertyName("value")],
                    MessagePartOptions(label = "Custom"),
                )
            }
        }

        val message = composeMessage(
            root = PropertyName("wiretap"),
            properties = mapOf("value" to "configured"),
        )

        assertEquals("Custom: configured", message)
    }
}
