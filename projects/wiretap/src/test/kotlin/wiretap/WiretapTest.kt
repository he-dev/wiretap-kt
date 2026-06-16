package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import wiretap.util.ActivityLogger
import wiretap.util.Activity
import wiretap.util.ActivityScope
import wiretap.util.ActivityStatus
import wiretap.util.beginBuzz
import wiretap.util.buzz.ActivityLogRecord

class WiretapTest {
    private val logger = TestActivityLogger()

    @Test
    fun exposesName() {
        assertEquals("wiretap", Wiretap.name)
    }

    @Test
    fun ambientScopeBuildsNestedPath() {
        logger.beginBuzz(ImportDocument()).use { import ->
            logger.beginBuzz(ParseDocument()).use { parse ->
                assertEquals(import, parse.parent)
                assertEquals(1, parse.depth)
                assertEquals("ImportDocument/ParseDocument", parse.path)
                parse.setStatus(ParseDocument.Okay())
            }

            import.setStatus(ImportDocument.Okay())
        }

        assertNull(ActivityScope.current())
    }

    @Test
    fun explicitParentCanCrossAmbientBoundary() {
        val parent = logger.beginBuzz(ImportDocument())

        try {
            parent.close()

            logger.beginBuzz(ParseDocument(), parent = parent).use { child ->
                assertEquals(parent, child.parent)
                assertEquals("ImportDocument/ParseDocument", child.path)
            }
        } finally {
            parent.close()
        }
    }

    @Test
    fun activityLoggerOwnsLifecycleApiAndUsesSink() {
        val records = mutableListOf<ActivityLogRecord>()
        val logger = TestActivityLogger(records)

        logger.beginBuzz(ImportDocument()).use { scope ->
            scope.setStatus(ImportDocument.Okay())
        }

        assertEquals(listOf("Ready", "Okay"), records.map { it.status.code })
        assertEquals(listOf("ImportDocument", "ImportDocument"), records.map { it.scope.activity.name })
    }

    private class ImportDocument : Activity.Buzz() {
        override val name: String = "ImportDocument"

        class Okay : ActivityStatus.Okay<ImportDocument>()
    }

    private class ParseDocument : Activity.Buzz() {
        override val name: String = "ParseDocument"

        class Okay : ActivityStatus.Okay<ParseDocument>()
    }

    private class TestActivityLogger(
        private val records: MutableList<ActivityLogRecord> = mutableListOf(),
    ) : ActivityLogger {
        override fun log(record: ActivityLogRecord) {
            records += record
        }
    }
}
