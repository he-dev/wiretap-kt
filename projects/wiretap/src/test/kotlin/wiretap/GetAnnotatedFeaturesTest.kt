package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.ActivityStatusLevel
import wiretap.util.Configuration
import wiretap.util.Detail
import wiretap.util.buzz.getAnnotatedFeatures

class GetAnnotatedFeaturesTest {
    @Test
    fun reportsAnnotatedFeatureMatches() {
        val matches = mutableListOf<Match>()

        getAnnotatedFeatures(Source("local", "shared")) { name, value, annotation ->
            matches += Match(name, value, annotation.cascade)
        }

        assertEquals(
            setOf(
                Match("local", "local", false),
                Match("renamed", "shared", true),
            ),
            matches.toSet(),
        )
    }

    @Test
    fun warnsAndIgnoresNonPublicFeatureAnnotations() {
        val diagnostics = mutableListOf<CapturedLog>()
        val previous = Configuration.diagnosticLogger

        try {
            Configuration.logDiagnosticsWith(CapturingActivityLogger(diagnostics))
            getAnnotatedFeatures(PrivateSource("hidden")) { _, _, _ -> error("private property must not be reported") }
        } finally {
            Configuration.logDiagnosticsWith(previous)
        }

        val diagnostic = diagnostics.single()
        assertEquals(ActivityStatusLevel.Warning, diagnostic.level)
        assertEquals(
            "@Detail on non-public property '${PrivateSource::class.qualifiedName}.hidden' was ignored; " +
                "annotated properties must be public.",
            diagnostic.message,
        )
    }

    data class Match(
        val name: String,
        val value: Any?,
        val cascade: Boolean,
    )

    class Source(
        @Detail
        val local: String,

        @Detail(name = "renamed", cascade = true)
        val shared: String,
    )

    class PrivateSource(
        @Detail
        private val hidden: String,
    )
}
