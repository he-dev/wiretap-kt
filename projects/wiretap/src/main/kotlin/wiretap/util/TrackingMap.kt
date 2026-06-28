package wiretap.util

import java.util.Locale


class DetailOptions {
    var cascade: Boolean = false
}

class RemarkOptions {
    var label: String? = null
    var separator: String = ": "
    var format: String? = null
}

interface DetailSource {
    fun DetailBuilder.details()
}

interface RemarkSource {
    fun RemarkBuilder.remarks()
}

class DetailBuilder(
    private val root: DottedName,
    private val level: Int,
    private val details: MutableMap<DottedName, Any?>,
) {
    fun add(
        name: DottedName,
        value: Any?,
        configure: DetailOptions.() -> Unit = {},
    ) {
        val options = DetailOptions().apply(configure)
        val key = root + name

        if (level == 0 || options.cascade) {
            if (key in details) {
                if (details[key] == null && value != null) {
                    details[key] = value
                }
            } else {
                details[key] = value
            }
        }
    }
}

class RemarkBuilder(
    val root: DottedName,
    val details: Map<DottedName, Any?>,
    private val remarks: MutableMap<DottedName, String?>
) {
    fun add(
        name: DottedName,
        value: Any?,
        configure: RemarkOptions.() -> Unit = {},
    ) {
        val options = RemarkOptions().apply(configure)
        val result = value?.let {
            val label = options.label ?: name.parts.last()
            val valueFormatted = options.format
                ?.let { format -> String.format(Locale.ROOT, format, it) }
                ?: it.toString()

            "$label${options.separator}$valueFormatted"
        }

        val previous = remarks[name]
        if (!remarks.containsKey(name) || previous == null && result != null) {
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
        //val value = details[name]?.let(render)
        val value = render(name)
        add(name, value, configure)
    }
}
