package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import wiretap.util.Activity
import wiretap.util.ActivityLogEntryFactory
import wiretap.util.ActivityLogRecord
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.SnapScope
import wiretap.util.StateItem
import wiretap.util.buzz.ComposeMessage

class ActivityLogEntryFactoryTest {
    @Test
    fun createsFinalEntryWithCustomMessageComposition() {
        val exception = IllegalStateException("broken")
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val status = EntryActivity.Fail(exception)
        val factory = ActivityLogEntryFactory.default(
            ComposeMessage { logProperties, _ ->
                "Record ${logProperties["wiretap.activity.state.recordId"]} failed"
            },
        )

        val entry = factory.create(scope, status)

        assertEquals(ActivityStatusLevel.Error, entry.level)
        assertEquals("Record customer-001 failed", entry.message)
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals(entry.properties, entry.toMap())
        assertSame(exception, entry.exception)
    }

    class EntryActivity(
        @StateItem
        val recordId: String,
    ) : Activity.Snap() {
        class Fail(exception: Throwable) : ActivityStatus.Fail<EntryActivity>(exception)
    }

    private companion object {
        val logger = object : ActivityLogger {
            override fun log(record: ActivityLogRecord) = Unit
        }
    }
}
