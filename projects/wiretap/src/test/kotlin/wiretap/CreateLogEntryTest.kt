package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.MessagePart
import wiretap.util.LogEntry
import wiretap.util.SnapScope
import wiretap.util.StateItem
import wiretap.util.buzz.MessagePartOptions
import wiretap.util.buzz.createLogEntryBy
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.activity
import wiretap.util.buzz.durationMs
import wiretap.util.buzz.name
import wiretap.util.buzz.state

class CreateLogEntryTest {
    @Test
    fun createsFinalEntryWithCustomMessageComposition() {
        val exception = IllegalStateException("broken")
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val status = EntryActivity.Fail(exception)
        val factory = createLogEntryBy {
            arrangeMessageParts {
                parts.push(
                    "record",
                    properties[root.activity.state.append("recordId").toString()],
                    MessagePartOptions(
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

        val entry = factory.from(scope, status)

        assertEquals(ActivityStatusLevel.Error, entry.level)
        assertEquals("Record customer-001 failed", entry.message)
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals(entry.properties, entry.toMap())
        assertSame(exception, entry.exception)
    }

    @Test
    fun allowsNamespaceConfigurationWithoutReplacingCollection() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = createLogEntryBy {
            root = PropertyName("application")
        }

        val entry = factory.from(scope, EntryActivity.Fail(IllegalStateException()))

        assertEquals("customer-001", entry["application.activity.state.recordId"])
        assertEquals("Fail", entry["application.activity.status.code"])
    }

    @Test
    fun arrangesCollectedMessagePartsBeforeJoining() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = createLogEntryBy {
            arrangeMessageParts {
                parts.push("prefix", "Prefix")
                listOfNotNull(
                    parts.pop(root.activity.name),
                    parts.pop(root.activity.durationMs),
                ) + parts.entries.sortedBy { it.key }.map { it.value }
            }
            joinMessageParts {
                joinToString(" | ") { it.value.toString() }
            }
        }

        val entry = factory.from(scope, EntryActivity.Fail(IllegalStateException("broken")))

        assertEquals(
            "EntryActivity[Fail] | N/A | broken | Prefix",
            entry.message,
        )
    }

    @Test
    fun formatsAnnotatedMessagePartValues() {
        val scope = SnapScope(logger, FormattedActivity(12.345), parent = null)
        val factory = createLogEntryBy()

        val entry = factory.from(scope, FormattedActivity.Okay())

        assertEquals(
            "FormattedActivity[Okay]; Duration: N/A; Rate: 12.35",
            entry.message,
        )
    }

    class EntryActivity(
        @StateItem
        val recordId: String,
    ) : Activity.Snap() {
        class Fail(exception: Throwable) : ActivityStatus.Fail<EntryActivity>(exception)
    }

    class FormattedActivity(
        @MessagePart(label = "Rate", format = "%.2f")
        val rate: Double,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<FormattedActivity>()
    }

    private companion object {
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) = Unit
        }
    }
}
