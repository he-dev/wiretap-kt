package wiretap.util

enum class OmitStatus {
    First,
    Last,
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BulkItem(
    vararg val omit: OmitStatus,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CountOnlyBulkItem

internal fun bulkItemStatusOmissions(type: Class<*>): Set<OmitStatus> =
    buildSet {
        type.getAnnotation(BulkItem::class.java)?.omit?.let(::addAll)

        if (type.isAnnotationPresent(CountOnlyBulkItem::class.java)) {
            addAll(OmitStatus.entries)
        }
    }
