package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.LogEntry
import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.addAnnotatedLogProperties
import wiretap.util.buzz.cascadingOnly
import wiretap.util.buzz.getLogProperties

class GetLogPropertiesTest {
    @Test
    fun getsPropertiesFromInterfacesAndAnnotations() {
        val properties = getLogProperties(
            PropertyName("wiretap"),
            Source(local = "local", cascading = "shared"),
        )

        assertEquals("interface", properties["wiretap.source"])
        assertEquals("local", properties["wiretap.activity.state.local"])
        assertEquals("shared", properties["wiretap.activity.state.cascading"])
    }

    @Test
    fun addsOnlyCascadingStateItemsFromAncestors() {
        val properties = linkedMapOf<String, Any?>()

        addAnnotatedLogProperties(
            PropertyName("state"),
            Source(local = "local", cascading = "shared"),
            object : AddLogProperty {
                override fun localOnly(key: PropertyName, value: Any?) {
                    value?.let { properties[key.toString()] = it }
                }

                override fun cascading(key: PropertyName, value: Any?) {
                    localOnly(key, value)
                }
            }.cascadingOnly(),
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
        val logger = object : ActivityLogger {
            override fun log(entry: LogEntry) {
                diagnostics += entry
            }
        }

        try {
            Configuration.logDiagnosticsWith(logger)
            val properties = getLogProperties(PropertyName("wiretap"), PrivateSource("hidden"))
            assertEquals(emptyMap(), properties)
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
        override fun logProperties(root: PropertyName, add: AddLogProperty) = with(add) {
            localOnly(root.append("source"), "interface")
        }
    }

    class PrivateSource(
        @StateItem
        private val hidden: String,
    )
}
