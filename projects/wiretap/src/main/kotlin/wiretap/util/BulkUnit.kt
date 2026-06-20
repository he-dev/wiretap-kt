package wiretap.util

enum class BulkUnit(
    val suffix: String,
    private val milliseconds: Double,
) {
    Milliseconds("ms", 1.0),
    Seconds("s", 1_000.0),
    Minutes("min", 60_000.0),
    Hours("h", 3_600_000.0),
    ;

    internal fun fromMilliseconds(value: Double): Double =
        value / milliseconds

    internal fun convert(value: Double, to: BulkUnit): Double =
        to.fromMilliseconds(value * milliseconds)
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BulkDurationUnit(
    val value: BulkUnit,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BulkThroughputUnit(
    val value: BulkUnit,
)
