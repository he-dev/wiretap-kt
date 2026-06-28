package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.DottedName

class PropertyNameTest {
    @Test
    fun stringPartsAreParsedAsDottedPaths() {
        assertEquals(
            "wiretap.activity.state.bulk.item_count",
            DottedName("wiretap.activity", "state", "bulk.item_count").toString(),
        )
    }

    @Test
    fun plusCombinesParsedPropertyNames() {
        assertEquals(
            "wiretap.activity.state.bulk.item_count",
            (DottedName("wiretap.activity.state") + DottedName.parse("bulk.item_count")).toString(),
        )
    }
}
