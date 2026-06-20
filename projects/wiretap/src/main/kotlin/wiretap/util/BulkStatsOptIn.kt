package wiretap.util

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BulkStatsOptIn(
    vararg val value: BulkStat,
)

enum class BulkStat {
    CountByStatus,
    RateByStatus,
    DurationMean,
    DurationExtremes,
    DurationStDev,
}
