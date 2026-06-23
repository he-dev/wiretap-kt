package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.MessagePart
import wiretap.util.PropertyName
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.MessagePartSource
import wiretap.util.buzz.getMessageParts

class GetMessagePartsTest {
    @Test
    fun getsMessagePartsFromInterfacesAndAnnotations() {
        val parts = getMessageParts(
            root = PropertyName("activity"),
            properties = emptyMap(),
            Source("annotated"),
        )

        assertEquals("interface", parts[PropertyName("interface")]?.value)
        assertEquals("annotated", parts[PropertyName("annotated")]?.value)
    }

    class Source(
        @MessagePart
        val annotated: String,
    ) : MessagePartSource {
        override fun AddMessagePart.messageParts(root: PropertyName) {
            discrete(PropertyName("interface"), "interface")
        }
    }
}
