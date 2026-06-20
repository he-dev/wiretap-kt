package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource

class BulkMath : LogPropertySource {
    private val statusCounts = linkedMapOf<String, Int>()

    var itemCount: Int = 0
        private set

    var durationMs: Long = 0
        private set

    fun count(status: ActivityStatus<*>, durationMs: Long) {
        // util: Bulk math records item outcomes only after each item reaches its final status.
        itemCount += 1
        this.durationMs += durationMs

        val code = status.code.lowercase()
        statusCounts[code] = statusCounts.getOrElse(code) { 0 } + 1
    }

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        val state = root.activity.state
        add(state.append("item_count"), itemCount)
        add(state.append("duration_ms"), durationMs)

        for ((code, count) in statusCounts) {
            add(state.append("${code}_count"), count)
            add(state.append("${code}_rate"), count.toDouble() / itemCount)
        }
    }
}
