package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import wiretap.util.Activity
import wiretap.util.ActivityLogRecord
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.SnapScope
import wiretap.util.StateItem
import wiretap.util.activityLogEntryFactory
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.state

class ActivityLogEntryFactoryTest {
    @Test
    fun createsFinalEntryWithCustomMessageComposition() {
        val exception = IllegalStateException("broken")
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val status = EntryActivity.Fail(exception)
        val factory = activityLogEntryFactory {
            arrangeMessageParts {
                parts.push(
                    "record",
                    properties[root.state.append("recordId").toString()],
                    wiretap.util.buzz.MessagePartOptions(
                        label = "Record",
                        separator = " ",
                    ),
                )
                listOfNotNull(parts.pop("record"))
            }
            joinMessageParts {
                "Record ${single().value} failed"
            }
        }

        val entry = factory.create(scope, status)

        assertEquals(ActivityStatusLevel.Error, entry.level)
        assertEquals("Record customer-001 failed", entry.message)
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals(entry.properties, entry.toMap())
        assertSame(exception, entry.exception)
    }

    @Test
    fun allowsNamespaceConfigurationWithoutReplacingCollection() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = activityLogEntryFactory {
            root = PropertyName("application", "activity")
        }

        val entry = factory.create(scope, EntryActivity.Fail(IllegalStateException()))

        assertEquals("customer-001", entry["application.activity.state.recordId"])
        assertEquals("Fail", entry["application.activity.status.code"])
    }

    @Test
    fun arrangesCollectedMessagePartsBeforeJoining() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = activityLogEntryFactory {
            arrangeMessageParts {
                parts.push("prefix", "Prefix")
                listOfNotNull(
                    parts.pop("activity"),
                    parts.pop("duration"),
                ) + parts.entries.sortedBy { it.key }.map { it.value }
            }
            joinMessageParts {
                joinToString(" | ") { it.value.toString() }
            }
        }

        val entry = factory.create(scope, EntryActivity.Fail(IllegalStateException("broken")))

        assertEquals(
            "EntryActivity[Fail] | N/A | broken | Prefix",
            entry.message,
        )
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
