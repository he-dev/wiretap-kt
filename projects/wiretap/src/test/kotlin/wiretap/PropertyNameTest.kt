package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.PropertyName

class PropertyNameTest {
    @Test
    fun stringPartsAreParsedAsDottedPaths() {
        assertEquals(
            "wiretap.activity.state.bulk.item_count",
            PropertyName("wiretap.activity", "state", "bulk.item_count").toString(),
        )
    }

    @Test
    fun plusCombinesParsedPropertyNames() {
        assertEquals(
            "wiretap.activity.state.bulk.item_count",
            (PropertyName("wiretap.activity.state") + PropertyName.parse("bulk.item_count")).toString(),
        )
    }
}
