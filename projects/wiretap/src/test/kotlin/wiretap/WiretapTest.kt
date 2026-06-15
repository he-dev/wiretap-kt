package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals

class WiretapTest {
    @Test
    fun exposesName() {
        assertEquals("wiretap", Wiretap.name)
    }
}
