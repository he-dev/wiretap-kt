package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Activity
import wiretap.util.BuzzScope

class ActivityScopeHierarchyTest {
    @Test
    fun iteratesFromCurrentScopeToRoot() {
        val root = BuzzScope(logger, Root(), parent = null)
        val middle = BuzzScope(logger, Middle(), parent = root)
        val current = BuzzScope(logger, Current(), parent = middle)

        assertEquals(
            listOf("Current", "Middle", "Root"),
            current.map { it.activity.name },
        )
    }

    private class Root : Activity.Buzz()
    private class Middle : Activity.Buzz()
    private class Current : Activity.Buzz()

    private companion object {
        val logger = CapturingActivityLogger()
    }
}
