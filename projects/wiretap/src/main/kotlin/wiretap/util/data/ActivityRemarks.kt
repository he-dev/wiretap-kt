package wiretap.util.data

import wiretap.util.DottedName
import java.util.Locale

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Remark(
    val label: String = "",
    val separator: String = ": ",
    val format: String = "",
    val quoteStyle: QuoteStyle = QuoteStyle.Double,
    val quoteMode: QuoteMode = QuoteMode.Never,
)

enum class QuoteStyle {
    Double,
    Single,
}

enum class QuoteMode {
    Never,
    Auto,
    Always,
}

class RemarkMap(
    private val inner: MutableMap<DottedName, String?> = linkedMapOf(),
) : MutableMap<DottedName, String?> by inner

class RemarkOptions {
    var label: String = ""
    var separator: String = ": "
    var format: String = ""
    var quoteStyle: QuoteStyle = QuoteStyle.Double
    var quoteMode: QuoteMode = QuoteMode.Never
}

interface RemarkSource {
    fun RemarkBuilder.remarks()
}

class RemarkBuilder(
    val root: DottedName,
    val details: DetailMap,
    private val remarks: RemarkMap,
) {
    fun add(
        name: DottedName,
        value: Any?,
        configure: RemarkOptions.() -> Unit = {},
    ) {
        val options = RemarkOptions().apply(configure)
        val result = value?.let {
            val label = options.label.ifEmpty { name.parts.last() }
            val valueFormatted = options.format
                .takeIf { it.isNotEmpty() }
                ?.let { format -> String.format(Locale.ROOT, format, it) }
                ?: it.toString()
            val valueRendered = valueFormatted.quote(options.quoteStyle, options.quoteMode)

            "$label${options.separator}$valueRendered"
        }

        if (name in remarks) {
            if (remarks[name] == null && result != null) {
                remarks[name] = result
            }
        } else {
            remarks[name] = result
        }
    }

    fun add(
        name: DottedName,
        configure: RemarkOptions.() -> Unit = {},
    ) {
        add(name, details[name], configure)
    }

    fun add(
        name: DottedName,
        render: (DottedName) -> String,
        configure: RemarkOptions.() -> Unit = {},
    ) {
        val value = render(name)
        add(name, value, configure)
    }
}

private fun String.quote(style: QuoteStyle, mode: QuoteMode): String {
    val quote = when (mode) {
        QuoteMode.Never -> return this
        QuoteMode.Auto -> if (none(Char::isWhitespace)) return this else style.character
        QuoteMode.Always -> style.character
    }

    return "$quote${escapeFor(quote)}$quote"
}

private val QuoteStyle.character: Char
    get() = when (this) {
        QuoteStyle.Double -> '"'
        QuoteStyle.Single -> '\''
    }

private fun String.escapeFor(quote: Char): String =
    replace("\\", "\\\\").replace(quote.toString(), "\\" + quote)
