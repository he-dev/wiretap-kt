package wiretap.util

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Detail(
    val name: String = "",
    val cascade: Boolean = false,
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Remark(
    val label: String = "",
    val separator: String = ": ",
    val format: String = "",
)
