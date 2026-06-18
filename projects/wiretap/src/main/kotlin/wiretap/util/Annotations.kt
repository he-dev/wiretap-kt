package wiretap.util

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateItem(
    val name: String = Unset,
    val cascade: Boolean = false,
)

// meta: Annotation parameters cannot be nullable, so this sentinel represents an unspecified value.
const val Unset: String = "\u0000"

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MessagePart(
    val label: String = Unset,
    val separator: String = ": ",
    val format: String = Unset,
)

internal fun String.nullIfUnset(): String? =
    takeUnless { it == Unset }
