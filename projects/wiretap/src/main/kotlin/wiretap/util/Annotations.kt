package wiretap.util

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeedToStateItem(
    val name: String = "",
)

const val FeedToMessagePartNoLabel: String = "\u0000"

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeedToMessagePart(
    val label: String = FeedToMessagePartNoLabel,
    val separator: String = ": ",
)
