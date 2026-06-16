package wiretap.util

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class StateItem(
    val name: String = "",
)

const val MessagePartNoLabel: String = "\u0000"

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MessagePart(
    val label: String = MessagePartNoLabel,
    val separator: String = ": ",
)
