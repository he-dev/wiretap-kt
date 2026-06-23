package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.MessagePart
import wiretap.util.PropertyName
import wiretap.util.MessagePartRegistry
import wiretap.util.MessagePartSource
import wiretap.util.buzz.getMessageParts

class GetMessagePartsTest {
    @Test
    fun getsMessagePartsFromInterfacesAndAnnotations() {
        val parts = getMessageParts(
            root = PropertyName("activity"),
            properties = emptyMap(),
            Source("annotated"),
        )

        assertEquals("interface", parts.pop(PropertyName("interface"))?.value)
        assertEquals("annotated", parts.pop(PropertyName("annotated"))?.value)
    }

    class Source(
        @MessagePart
        val annotated: String,
    ) : MessagePartSource {
        override fun MessagePartRegistry.messageParts(root: PropertyName) {
            discrete(PropertyName("interface"), "interface")
        }
    }
}
