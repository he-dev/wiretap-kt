package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
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

class TraceContextTest {
    @Test
    fun propagatesTraceContextAcrossScopes() {
        val parent = BuzzScope(logger, ParentActivity(), parent = null)
        val child = SnapScope(logger, ChildActivity(), parent)
        val createLogEntry = Configuration.Variant().createLogEntry

        parent.activity.start()
        parent.activity.setStatus(ParentActivity.Okay())
        child.activity.setStatus(ChildActivity.Okay())
        val parentEntry = createLogEntry.from(listOf(parent.activity), parent.traceContext)
        val childEntry = createLogEntry.from(
            listOf(child.activity, parent.activity),
            child.traceContext,
        )

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

        logger.beginBuzz(ParentActivity(), traceId = externalTraceId) { buzz ->
            assertEquals(externalTraceId, buzz.traceContext.traceId)
            buzz.setStatus(ParentActivity.Okay())
        }
    }

    private class ParentActivity : Activity.Buzz() {
        class Okay : ActivityStatus.Okay<ParentActivity>()
    }

    private class ChildActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<ChildActivity>()
    }

    private companion object {
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) = Unit
        }
    }
}
