package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.BuzzScope
import wiretap.util.LogEntry

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
        assertEquals(
            listOf("Middle", "Root"),
            current.ancestors.map { it.activity.name }.toList(),
        )
    }

    @Test
    fun buildsPathFromRootToCurrentScope() {
        val root = BuzzScope(logger, Root(), parent = null)
        val middle = BuzzScope(logger, Middle(), parent = root)
        val current = BuzzScope(logger, Current(), parent = middle)

        assertEquals("Root/Middle/Current", current.path)
    }

    private class Root : Activity.Buzz()
    private class Middle : Activity.Buzz()
    private class Current : Activity.Buzz()

    private companion object {
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) = Unit
        }
    }
}
