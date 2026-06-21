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
import wiretap.util.SnapScope
import wiretap.util.StateItem
import wiretap.util.activity
import wiretap.util.state
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.getLogProperties

class GetLogPropertiesTest {
    @Test
    fun getsPropertiesFromInterfacesAndAnnotations() {
        val properties = getLogProperties(
            PropertyName("wiretap"),
            SnapScope(logger, PropertyActivity(), parent = null),
            PropertyActivity.Okay(),
            Source(local = "local", cascading = "shared"),
        )

        assertEquals("interface", properties["wiretap.source"])
        assertEquals("local", properties["wiretap.activity.state.local"])
        assertEquals("shared", properties["wiretap.activity.state.cascading"])
    }

    @Test
    fun nearestInterfaceClaimWinsAndCanSuppressFallbacksWithNull() {
        val parent = SnapScope(logger, ParentActivity(), parent = null)
        val properties = getLogProperties(
            PropertyName("wiretap"),
            SnapScope(logger, NullingActivity(), parent),
            NullingActivity.Okay(),
        )

        assertNull(properties["wiretap.activity.state.shared"])
        assertNull(properties["wiretap.activity.state.local"])
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
            val properties = getLogProperties(
                PropertyName("wiretap"),
                SnapScope(logger, DuplicateActivity(), parent = null),
                DuplicateActivity.Okay(),
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
            val properties = getLogProperties(
                PropertyName("wiretap"),
                SnapScope(logger, PropertyActivity(), parent = null),
                PropertyActivity.Okay(),
                PrivateSource("hidden"),
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

    class Source(
        @StateItem
        val local: String,

        @StateItem(cascade = true)
        val cascading: String,
    ) : LogPropertySource {
        override fun AddLogProperty.logProperties(root: PropertyName) {
            localOnly(root.append("source"), "interface")
        }
    }

    class PrivateSource(
        @StateItem
        private val hidden: String,
    )

    class PropertyActivity : Activity.Snap() {
        class Okay : ActivityStatus.Okay<PropertyActivity>()
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
        override fun AddLogProperty.logProperties(root: PropertyName) {
            cascading(root.activity.state.append("shared"), null)
        }

        class Okay : ActivityStatus.Okay<NullingActivity>()
    }

    class DuplicateActivity : Activity.Snap(), LogPropertySource {
        override fun AddLogProperty.logProperties(root: PropertyName) {
            val key = root.activity.state.append("duplicate")
            localOnly(key, "first")
            localOnly(key, "second")
        }

        class Okay : ActivityStatus.Okay<DuplicateActivity>()
    }

    private val logger = object : ActivityLogger {
        override fun log(entry: LogEntry) = Unit
    }
}
