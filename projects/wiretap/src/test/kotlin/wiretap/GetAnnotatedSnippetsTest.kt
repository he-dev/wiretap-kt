package wiretap

import kotlin.test.Test
import kotlin.test.assertEquals
import wiretap.util.Remark
import wiretap.util.nullIfUnset
import wiretap.util.buzz.getAnnotatedSnippets

class GetAnnotatedSnippetsTest {
    @Test
    fun reportsAnnotatedSnippetMatches() {
        val matches = mutableListOf<Match>()

        getAnnotatedSnippets(Source("first", "second")) { name, value, annotation ->
            matches += Match(name, value, annotation.label.nullIfUnset(), annotation.separator, annotation.format.nullIfUnset())
        }

        assertEquals(
            setOf(
                Match("plain", "first", null, ": ", null),
                Match("formatted", "second", "Label", " = ", "%s!"),
            ),
            matches.toSet(),
        )
    }

    data class Match(
        val name: String,
        val value: Any?,
        val label: String?,
        val separator: String,
        val format: String?,
    )

    class Source(
        @Remark
        val plain: String,

        @Remark(label = "Label", separator = " = ", format = "%s!")
        val formatted: String,
    )
}
