package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import wiretap.core.beginBuzz
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.BuzzScope
import wiretap.util.Configuration
import wiretap.util.LogEntry
import wiretap.util.SnapScope
import wiretap.util.buzz.createLogEntryBy

class TraceContextTest {
    @Test
    fun propagatesTraceContextAcrossScopes() {
        val parent = BuzzScope(logger, ParentActivity(), parent = null)
        val child = SnapScope(logger, ChildActivity(), parent)
        val createLogEntry = createLogEntryBy()

        val parentEntry = createLogEntry.from(parent, ParentActivity.Okay())
        val childEntry = createLogEntry.from(child, ChildActivity.Okay())

        assertTrue(parent.traceContext.traceId.matches(Regex("[0-9a-f]{32}")))
        assertTrue(parent.traceContext.spanId.matches(Regex("[0-9a-f]{16}")))
        assertEquals(parent.traceContext.traceId, parentEntry["wiretap.trace_id"])
        assertEquals(parent.traceContext.spanId, parentEntry["wiretap.span_id"])
        assertNull(parentEntry["wiretap.parent_span_id"])

        assertEquals(parent.traceContext.traceId, child.traceContext.traceId)
        assertNotEquals(parent.traceContext.spanId, child.traceContext.spanId)
        assertEquals(child.traceContext.traceId, childEntry["wiretap.trace_id"])
        assertEquals(child.traceContext.spanId, childEntry["wiretap.span_id"])
        assertEquals(parent.traceContext.spanId, childEntry["wiretap.parent_span_id"])
    }

    @Test
    fun acceptsExternalTraceIdWhenBeginningRootBuzz() {
        val externalTraceId = "0123456789abcdef0123456789abcdef"

        logger.beginBuzz(ParentActivity(), traceId = externalTraceId) {
            assertEquals(externalTraceId, traceContext.traceId)
            setStatus(ParentActivity.Okay())
        }
    }

    @Test
    fun omitsTraceContextWhenDisabled() {
        Configuration.addNamed("without-trace-context") {
            Configuration.Variant(attachTraceContext = false)
        }
        val scope = SnapScope(logger, UntracedActivity(), parent = null)

        val entry = createLogEntryBy().from(scope, UntracedActivity.Okay())

        assertFalse(entry.properties.keys.any { it.startsWith("wiretap.trace_") })
        assertFalse(entry.properties.containsKey("wiretap.span_id"))
        assertFalse(entry.properties.containsKey("wiretap.parent_span_id"))
    }

    private class ParentActivity : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParentActivity>()
    }

    private class ChildActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<ChildActivity>()
    }

    @Configuration.Use("without-trace-context")
    private class UntracedActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<UntracedActivity>()
    }

    private companion object {
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) = Unit
        }
    }
}
