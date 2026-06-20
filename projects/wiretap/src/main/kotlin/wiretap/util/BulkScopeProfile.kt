package wiretap.util

import kotlin.reflect.KClass

data class BulkScopeProfile(
    val stats: Set<BulkStat> = emptySet(),
    val durationUnit: BulkUnit = BulkUnit.Milliseconds,
    val throughputUnit: BulkUnit = BulkUnit.Seconds,
) {
    companion object {
        fun from(type: KClass<*>): BulkScopeProfile =
            BulkScopeProfile(
                stats = type.java
                    .getAnnotation(BulkStatsOptIn::class.java)
                    ?.value
                    ?.toSet()
                    .orEmpty(),
                durationUnit = type.java
                    .getAnnotation(BulkDurationUnit::class.java)
                    ?.value
                    ?: BulkUnit.Milliseconds,
                throughputUnit = type.java
                    .getAnnotation(BulkThroughputUnit::class.java)
                    ?.value
                    ?: BulkUnit.Seconds,
            )
    }
}
