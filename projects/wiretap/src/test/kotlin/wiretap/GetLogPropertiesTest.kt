package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.LogEntryBuilder
import wiretap.util.LogEntryFactory
import wiretap.util.LogEntry
import wiretap.util.DottedName
import wiretap.util.Detail
import wiretap.util.buzz.getLogProperties

class GetLogPropertiesTest {
    @Test
    fun getsPropertiesFromInterfacesAndAnnotations() {
        val activity = PropertyActivity(local = "local", cascading = "shared")
        activity.setStatus(PropertyActivity.Okay())
        val properties = getLogProperties(
            DottedName("wiretap"),
            listOf(activity),
            traceContext = null,
        )

        assertEquals("interface", properties[DottedName("wiretap.activity.state.source")]?.value)
        assertEquals("local", properties[DottedName("wiretap.activity.state.local")]?.value)
        assertEquals("shared", properties[DottedName("wiretap.activity.state.cascading")]?.value)
    }

    @Test
    fun nearestInterfaceClaimWinsAndCanSuppressFallbacksWithNull() {
        val parent = ParentActivity()
        val activity = NullingActivity()
        activity.setStatus(NullingActivity.Okay())
        val properties = getLogProperties(
            DottedName("wiretap"),
            listOf(activity, parent),
            traceContext = null,
        )

        assertEquals(null, properties[DottedName("wiretap.activity.state.shared")]?.value)
        assertEquals(null, properties[DottedName("wiretap.activity.state.local")]?.value)
        assertEquals(1, properties[DottedName("wiretap.activity.depth")]?.value)
        assertEquals("ParentActivity/NullingActivity", properties[DottedName("wiretap.activity.path")]?.value)
    }

    @Test
    fun keepsFirstFeatureWhenFactoryAddsSameKeyTwice() {
        val activity = DuplicateActivity()
        activity.setStatus(DuplicateActivity.Okay())
        val properties = getLogProperties(
            DottedName("wiretap"),
            listOf(activity),
            traceContext = null,
        )

        assertEquals("first", properties[DottedName("wiretap.activity.state.duplicate")]?.value)
    }

    @Test
    fun warnsAndIgnoresNonPublicStateItems() {
        val diagnostics = mutableListOf<LogEntry>()
        val messages = mutableListOf<String>()
        val previous = Configuration.diagnosticLogger
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry, message: String) {
                diagnostics += entry
                messages += message
            }
        }

        try {
            Configuration.logDiagnosticsWith(logger)
            val activity = PrivateSource("hidden")
            activity.setStatus(PrivateSource.Okay())
            val properties = getLogProperties(
                DottedName("wiretap"),
                listOf(activity),
                traceContext = null,
            )
            assertEquals(null, properties[DottedName("wiretap.activity.state.hidden")]?.value)
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(ActivityStatusLevel.Warning, diagnostics.single().level)
        assertEquals(
            "@StateItem on non-public property '${PrivateSource::class.qualifiedName}.hidden' was ignored; " +
                "annotated properties must be public.",
            messages.single(),
        )
    }

    class PropertyActivity(
        @Detail
        val local: String,

        @Detail(cascade = true)
        val cascading: String,
    ) : Activity.Snap(), LogEntryFactory {
        override fun LogEntryBuilder.create() {
            features {
                add(DottedName("source"), "interface")
            }
        }

        class Okay : ActivityStatus.Okay<PropertyActivity>()
    }

    class PrivateSource(
        @Detail
        private val hidden: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<PrivateSource>()
    }

    class ParentActivity(
        @Detail(name = "shared", cascade = true)
        val shared: String = "parent",

        @Detail(name = "local")
        val local: String = "parent-local",
    ) : Activity.Snap()

    class NullingActivity(
        @Detail(name = "shared", cascade = true)
        val shared: String = "annotation",
    ) : Activity.Snap(), LogEntryFactory {
        override fun LogEntryBuilder.create() {
            features {
                add(DottedName("shared"), null) { cascade = true }
            }
        }

        class Okay : ActivityStatus.Okay<NullingActivity>()
    }

    class DuplicateActivity : Activity.Snap(), LogEntryFactory {
        override fun LogEntryBuilder.create() {
            features {
                val key = DottedName("duplicate")
                add(key, "first")
                add(key, "second")
            }
        }

        class Okay : ActivityStatus.Okay<DuplicateActivity>()
    }

    private val logger = object : ActivityLogger {
        override fun log(entry: LogEntry, message: String) = Unit
    }
}
