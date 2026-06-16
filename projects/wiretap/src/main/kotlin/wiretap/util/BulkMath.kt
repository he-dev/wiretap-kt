package wiretap.util

import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushLogProperty
import wiretap.util.buzz.LogPropertyFeed
import wiretap.util.buzz.state

class BulkMath : LogPropertyFeed {
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
        statusCounts[code] = statusCounts.getOrDefault(code, 0) + 1
    }

    override fun logProperties(root: PropertyName, push: PushLogProperty) {
        val state = root.state
        push(state.append("item_count"), itemCount)
        push(state.append("duration_ms"), durationMs)

        for ((code, count) in statusCounts) {
            push(state.append("${code}_count"), count)
            push(state.append("${code}_rate"), count.toDouble() / itemCount)
        }
    }
}
