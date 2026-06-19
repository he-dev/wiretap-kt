package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.AnnotatedStateItems
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.LogEntry
import wiretap.util.StateItem
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.PropertyName

class AnnotatedStateItemsTest {
    @Test
    fun pushesEveryStateItemFromSelf() {
        val properties = linkedMapOf<String, Any?>()

        AnnotatedStateItems.pushFromSelf(
            PropertyName("state"),
            AddLogProperty(properties::put),
            Source(local = "local", cascading = "shared"),
        )

        assertEquals(
            mapOf<String, Any?>(
                "state.local" to "local",
                "state.cascading" to "shared",
            ),
            properties,
        )
    }

    @Test
    fun pushesOnlyCascadingStateItemsFromAncestors() {
        val properties = linkedMapOf<String, Any?>()

        AnnotatedStateItems.pushFromAncestors(
            PropertyName("state"),
            AddLogProperty(properties::put),
            sequenceOf(Source(local = "local", cascading = "shared")),
        )

        assertEquals(
            mapOf<String, Any?>("state.cascading" to "shared"),
            properties,
        )
    }

    @Test
    fun warnsAndIgnoresNonPublicStateItems() {
        val diagnostics = mutableListOf<LogEntry>()
        val previous = Configuration.diagnosticLogger
        val properties = linkedMapOf<String, Any?>()
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) {
                diagnostics += entry
            }
        }

        try {
            Configuration.logDiagnosticsWith(logger)
            AnnotatedStateItems.pushFromSelf(
                PropertyName("state"),
                AddLogProperty(properties::put),
                PrivateSource("hidden"),
            )
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertEquals(emptyMap(), properties)
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
    )

    class PrivateSource(
        @StateItem
        private val hidden: String,
    )
}
