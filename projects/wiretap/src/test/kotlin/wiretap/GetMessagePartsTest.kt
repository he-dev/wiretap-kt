package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.FeatureMap
import wiretap.util.LogEntryBuilder
import wiretap.util.LogEntryFactory
import wiretap.util.Remark
import wiretap.util.DottedName
import wiretap.util.buzz.getMessageParts

class GetMessagePartsTest {
    @Test
    fun getsMessagePartsFromInterfacesAndAnnotations() {
        val parts = getMessageParts(
            root = DottedName("activity"),
            features = FeatureMap(),
            Source("annotated"),
        )

        assertEquals("interface", parts.pop(DottedName("interface"))?.value)
        assertEquals("annotated", parts.pop(DottedName("activity.activity.state.annotated"))?.value)
    }

    class Source(
        @Remark
        val annotated: String,
    ) : LogEntryFactory {
        override fun LogEntryBuilder.create() {
            snippets {
                add(DottedName("interface"), "interface")
            }
        }
    }
}
