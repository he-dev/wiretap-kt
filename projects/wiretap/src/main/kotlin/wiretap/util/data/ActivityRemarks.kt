package wiretap.util.data

import wiretap.util.DottedName
import java.util.Locale

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Remark(
    val label: String = "",
    val separator: String = ": ",
    val format: String = "",
)

class RemarkMap(
    private val inner: MutableMap<DottedName, String?> = linkedMapOf(),
) : MutableMap<DottedName, String?> by inner

class RemarkOptions {
    var label: String = ""
    var separator: String = ": "
    var format: String = ""
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

            "$label${options.separator}$valueFormatted"
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
