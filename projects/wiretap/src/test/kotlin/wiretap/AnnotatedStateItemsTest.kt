package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.AnnotatedStateItems
import wiretap.util.StateItem
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.PropertyName

class AnnotatedStateItemsTest {
    @Test
    fun pushesEveryStateItemFromSelf() {
        val properties = linkedMapOf<String, Any?>()

        AnnotatedStateItems.pushFromSelf(
            PropertyName("state"),
            AddLogProperty(properties::put),
            Source(local = "local", cascading = "shared"),
        )

        assertEquals(
            mapOf<String, Any?>(
                "state.local" to "local",
                "state.cascading" to "shared",
            ),
            properties,
        )
    }

    @Test
    fun pushesOnlyCascadingStateItemsFromAncestors() {
        val properties = linkedMapOf<String, Any?>()

        AnnotatedStateItems.pushFromAncestors(
            PropertyName("state"),
            AddLogProperty(properties::put),
            sequenceOf(Source(local = "local", cascading = "shared")),
        )

        assertEquals(
            mapOf<String, Any?>("state.cascading" to "shared"),
            properties,
        )
    }

    class Source(
        @StateItem
        val local: String,

        @StateItem(cascade = true)
        val cascading: String,
    )
}
