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

        // todo: overwrite when value not null but was null before

        if (level == 0) {
            details.putIfAbsent(root + name, value)
        } else {
            if (options.cascade) {
                details.putIfAbsent(root + name, value)
            }
        }
    }
}

class RemarkBuilder(
    val root: DottedName,
    val details: Map<DottedName, Any?>,
    private val remarks: MutableMap<DottedName, String>
) {
    fun add(
        name: DottedName,
        value: Any?,
        configure: RemarkOptions.() -> Unit = {},
    ) {
        val options = RemarkOptions().apply(configure)

        val label = options.label ?: name.parts.last()
        val valueFormatted = options.format
            ?.let { String.format(Locale.ROOT, it, value) }
            ?: value.toString()

        val result =  "$label${options.separator}$valueFormatted"

        remarks.putIfAbsent(name, result)
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
