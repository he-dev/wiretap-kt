package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.MessagePart
import wiretap.util.LogEntry
import wiretap.util.SnapScope
import wiretap.util.StateItem
import wiretap.util.PropertyName
import wiretap.util.activity
import wiretap.util.code
import wiretap.util.durationMs
import wiretap.util.name
import wiretap.util.state
import wiretap.util.status
import wiretap.util.wiretap
import wiretap.util.buzz.CreateLogEntry
import wiretap.util.buzz.activityDuration
import wiretap.util.buzz.activityHeader
import wiretap.util.buzz.composeMessage

class CreateLogEntryTest {
    @Test
    fun createsFinalEntryWithCustomMessageComposition() {
        val exception = IllegalStateException("broken")
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val status = EntryActivity.Fail(exception)
        val factory = CreateLogEntry(PropertyName().wiretap, composeMessage {
            include {
                discrete(PropertyName("record"), this[root.activity.state.append("recordId")]) {
                    label = "Record"
                    separator = " "
                }
            }
            arrange {
                positional(PropertyName("record"))
            }
            join {
                "Record ${single().value} failed"
            }
        })

        scope.activity.setStatus(status)
        val entry = factory.from(listOf(scope.activity), scope.traceContext)

        assertEquals(ActivityStatusLevel.Error, entry.level)
        assertEquals("Record customer-001 failed", entry.message)
        assertEquals("customer-001", entry["wiretap.activity.state.recordId"])
        assertEquals(entry.properties, entry.toMap())
        assertSame(exception, entry.exception)
    }

    @Test
    fun allowsNamespaceConfigurationWithoutReplacingCollection() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = Configuration.Variant(root = PropertyName("application")).createLogEntry

        scope.activity.setStatus(EntryActivity.Fail(IllegalStateException()))
        val entry = factory.from(listOf(scope.activity), scope.traceContext)

        assertEquals("customer-001", entry["application.activity.state.recordId"])
        assertEquals("Fail", entry["application.activity.status.code"])
    }

    @Test
    fun arrangesCollectedMessagePartsBeforeJoining() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = CreateLogEntry(PropertyName().wiretap, composeMessage {
            include {
                activityHeader()
                activityDuration()
                discrete(PropertyName("prefix"), "Prefix")
            }
            arrange {
                positional(root.activity.name)
                positional(root.activity.durationMs)
                remaining()
            }
            join {
                joinToString(" | ") { it.value.toString() }
            }
        })

        scope.activity.setStatus(EntryActivity.Fail(IllegalStateException("broken")))
        val entry = factory.from(listOf(scope.activity), scope.traceContext)

        assertEquals(
            "EntryActivity[Fail] | N/A | broken | Prefix",
            entry.message,
        )
    }

    @Test
    fun formatsAnnotatedMessagePartValues() {
        val scope = SnapScope(logger, FormattedActivity(12.345), parent = null)
        val factory = Configuration.Variant().createLogEntry

        scope.activity.setStatus(FormattedActivity.Okay())
        val entry = factory.from(listOf(scope.activity), scope.traceContext)

        assertEquals(
            "FormattedActivity[Okay]; Duration: N/A; Rate: 12.35",
            entry.message,
        )
    }

    @Test
    fun registeredMessagePartsCanReplaceDefaultsByName() {
        val scope = SnapScope(logger, EntryActivity("customer-001"), parent = null)
        val factory = CreateLogEntry(PropertyName().wiretap, composeMessage {
            include {
                activityHeader()
                activityDuration()
                discrete(root.activity.name, "Custom")
            }
            arrange {
                positional(root.activity.name)
                positional(root.activity.durationMs)
                remaining()
            }
            join {
                joinToString("; ") { it.text }
            }
        })

        scope.activity.setStatus(EntryActivity.Fail(IllegalStateException("broken")))
        val entry = factory.from(listOf(scope.activity), scope.traceContext)

        assertEquals("Custom; Duration: N/A; Exception: broken", entry.message)
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
