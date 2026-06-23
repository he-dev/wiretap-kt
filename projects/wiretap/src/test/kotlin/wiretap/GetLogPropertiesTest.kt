package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatus
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.LogEntry
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.activity
import wiretap.util.state
import wiretap.util.LogPropertyRegistry
import wiretap.util.LogPropertySource
import wiretap.util.buzz.getLogProperties

class GetLogPropertiesTest {
    @Test
    fun getsPropertiesFromInterfacesAndAnnotations() {
        val activity = PropertyActivity(local = "local", cascading = "shared")
        activity.setStatus(PropertyActivity.Okay())
        val properties = getLogProperties(
            PropertyName("wiretap"),
            listOf(activity),
            traceContext = null,
        )

        assertEquals("interface", properties["wiretap.source"])
        assertEquals("local", properties["wiretap.activity.state.local"])
        assertEquals("shared", properties["wiretap.activity.state.cascading"])
    }

    @Test
    fun nearestInterfaceClaimWinsAndCanSuppressFallbacksWithNull() {
        val parent = ParentActivity()
        val activity = NullingActivity()
        activity.setStatus(NullingActivity.Okay())
        val properties = getLogProperties(
            PropertyName("wiretap"),
            listOf(activity, parent),
            traceContext = null,
        )

        assertNull(properties["wiretap.activity.state.shared"])
        assertNull(properties["wiretap.activity.state.local"])
        assertEquals(1, properties["wiretap.activity.depth"])
        assertEquals("ParentActivity/NullingActivity", properties["wiretap.activity.path"])
    }

    @Test
    fun warnsWhenCurrentInterfacePushesTheSameKeyTwice() {
        val diagnostics = mutableListOf<LogEntry>()
        val previous = Configuration.diagnosticLogger
        val diagnosticLogger = object : ActivityLogger {
            override fun log(entry: LogEntry) {
                diagnostics += entry
            }
        }

        try {
            Configuration.logDiagnosticsWith(diagnosticLogger)
            val activity = DuplicateActivity()
            activity.setStatus(DuplicateActivity.Okay())
            val properties = getLogProperties(
                PropertyName("wiretap"),
                listOf(activity),
                traceContext = null,
            )

            assertEquals("first", properties["wiretap.activity.state.duplicate"])
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(
            "Log-property source '${DuplicateActivity::class.qualifiedName}' pushed " +
                "'wiretap.activity.state.duplicate' more than once; the first value was kept.",
            diagnostics.single().message,
        )
    }

    @Test
    fun warnsAndIgnoresNonPublicStateItems() {
        val diagnostics = mutableListOf<LogEntry>()
        val previous = Configuration.diagnosticLogger
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) {
                diagnostics += entry
            }
        }

        try {
            Configuration.logDiagnosticsWith(logger)
            val activity = PrivateSource("hidden")
            activity.setStatus(PrivateSource.Okay())
            val properties = getLogProperties(
                PropertyName("wiretap"),
                listOf(activity),
                traceContext = null,
            )
            assertNull(properties["wiretap.activity.state.hidden"])
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(ActivityStatusLevel.Warning, diagnostics.single().level)
        assertEquals(
            "@StateItem on non-public property '${PrivateSource::class.qualifiedName}.hidden' was ignored; " +
                "annotated properties must be public.",
            diagnostics.single().message,
        )
    }

    class PropertyActivity(
        @StateItem
        val local: String,

        @StateItem(cascade = true)
        val cascading: String,
    ) : Activity.Snap(), LogPropertySource {
        override fun LogPropertyRegistry.logProperties(root: PropertyName) {
            register(root.append("source"), "interface")
        }

        class Okay : ActivityStatus.Okay<PropertyActivity>()
    }

    class PrivateSource(
        @StateItem
        private val hidden: String,
    ) : Activity.Snap() {
        class Okay : ActivityStatus.Okay<PrivateSource>()
    }

    class ParentActivity(
        @StateItem(name = "shared", cascade = true)
        val shared: String = "parent",

        @StateItem(name = "local")
        val local: String = "parent-local",
    ) : Activity.Snap()

    class NullingActivity(
        @StateItem(name = "shared", cascade = true)
        val shared: String = "annotation",
    ) : Activity.Snap(), LogPropertySource {
        override fun LogPropertyRegistry.logProperties(root: PropertyName) {
            register(root.activity.state.append("shared"), null) { cascade = true }
        }

        class Okay : ActivityStatus.Okay<NullingActivity>()
    }

    class DuplicateActivity : Activity.Snap(), LogPropertySource {
        override fun LogPropertyRegistry.logProperties(root: PropertyName) {
            val key = root.activity.state.append("duplicate")
            register(key, "first")
            register(key, "second")
        }

        class Okay : ActivityStatus.Okay<DuplicateActivity>()
    }

    private val logger = object : ActivityLogger {
        override fun log(entry: LogEntry) = Unit
    }
}
