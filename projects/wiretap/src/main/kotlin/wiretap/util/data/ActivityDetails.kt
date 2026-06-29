package wiretap.util.data

import wiretap.util.DottedName

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Detail(
    val name: String = "",
    val cascade: Boolean = false,
)

class DetailMap(
    private val inner: MutableMap<DottedName, Any?> = linkedMapOf(),
) : MutableMap<DottedName, Any?> by inner

class DetailOptions {
    var cascade: Boolean = false
}

interface DetailSource {
    fun DetailBuilder.details()
}

class DetailBuilder(
    private val root: DottedName,
    private val level: Int,
    private val details: DetailMap,
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
