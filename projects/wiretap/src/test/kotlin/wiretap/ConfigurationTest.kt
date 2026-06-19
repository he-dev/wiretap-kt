package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import wiretap.util.Activity
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.LogEntry
import wiretap.util.buzz.createLogEntryBy

class ConfigurationTest {
    @Test
    fun configuresDiagnosticLogger() {
        val previous = Configuration.diagnosticLogger
        val adapter = object : ActivityLogger {
            override fun log(entry: LogEntry) = Unit
        }

        try {
            val configured = Configuration.logDiagnosticsWith(adapter)

            assertSame(Configuration, configured)
            assertSame(adapter, Configuration.diagnosticLogger)
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }
    }

    @Test
    fun resolvesDefaultForUnannotatedActivity() {
        val resolved = Configuration.resolve(DefaultActivity())

        assertSame(Configuration.default, resolved)
    }

    @Test
    fun setsDefaultVariant() {
        val previous = Configuration.default
        val replacement = Configuration.Variant(createLogEntryBy = createLogEntryBy())

        try {
            val configured = Configuration.setDefault { replacement }

            assertSame(Configuration, configured)
            assertSame(replacement, Configuration.resolve(ReplacementDefaultActivity()))
        } finally {
            Configuration.setDefault { previous }
        }
    }

    @Test
    fun addsAndResolvesNamedVariant() {
        val variant = Configuration.Variant(createLogEntryBy = createLogEntryBy())

        val configured = Configuration.addNamed("compact") { variant }
        val resolved = Configuration.resolve(CompactActivity())

        assertSame(Configuration, configured)
        assertSame(variant, Configuration["compact"])
        assertSame(variant, resolved)
    }

    @Test
    fun resolvesNamedVariantAddedAfterCachedFallback() {
        val variant = Configuration.Variant(createLogEntryBy = createLogEntryBy())

        assertSame(Configuration.default, Configuration.resolve(LateActivity()))

        Configuration.addNamed("late") { variant }

        assertSame(variant, Configuration.resolve(LateActivity()))
    }

    @Test
    fun rejectsDuplicateNamedVariant() {
        Configuration.addNamed("duplicate") { Configuration.Variant() }

        assertFailsWith<IllegalStateException> {
            Configuration.addNamed("duplicate") { Configuration.Variant() }
        }
    }

    @Test
    fun fallsBackWhenNamedVariantIsMissing() {
        val diagnostics = mutableListOf<LogEntry>()
        val previous = Configuration.diagnosticLogger
        val adapter = object : ActivityLogger {
            override fun log(entry: LogEntry) {
                diagnostics += entry
            }
        }

        val resolved = try {
            Configuration.logDiagnosticsWith(adapter)
            Configuration.resolve(MisconfiguredActivity())
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        assertSame(Configuration.default, resolved)
        assertEquals(ActivityStatusLevel.Warning, diagnostics.single().level)
        assertEquals(emptyMap(), diagnostics.single().properties)
        assertEquals(
            "Configuration variant 'missing' requested by ${MisconfiguredActivity::class.java.name} " +
                "was not found; using the default one.",
            diagnostics.single().message,
        )
    }

    private class DefaultActivity : Activity.Snap()

    private class ReplacementDefaultActivity : Activity.Snap()

    @Configuration.Use("compact")
    private class CompactActivity : Activity.Snap()

    @Configuration.Use("late")
    private class LateActivity : Activity.Snap()

    @Configuration.Use("missing")
    private class MisconfiguredActivity : Activity.Snap()
}
