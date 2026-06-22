package wiretap.util

abstract class Activity protected constructor(
    @StateItem
    val role: String,
) {
    open val name: String
        get() = this::class.simpleName!!

    open val tags: Set<String> = emptySet()

    abstract class Buzz internal constructor(role: String) : Activity(role) {
        protected constructor() : this("buzz")
    }

    abstract class Item : Buzz("item")

    abstract class Snap : Activity("snap")

    abstract class Bulk<I : Item> : Buzz("bulk") {
        // core: Bulk calculations live with their activity, so publication needs no scope-specific path.
        internal val math = BulkMath()

        @StateItem("bulk.item_count")
        @MessagePart(label = "Item Count")
        val itemCount: Int
            get() = math.itemCount

        @StateItem("bulk.duration_s")
        @MessagePart(label = "Item Duration", format = "%.3f s")
        val durationS: Double
            get() = math.durationMs / 1_000.0

        @StateItem("bulk.throughput_s")
        @MessagePart(label = "Throughput", format = "%.1f/s")
        val throughputS: Double
            get() = math.throughputMs * 1_000.0
    }
}
