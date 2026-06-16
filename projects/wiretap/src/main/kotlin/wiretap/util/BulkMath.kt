package wiretap.util

import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushStateItem
import wiretap.util.buzz.StateItemFeed
import wiretap.util.buzz.activity
import wiretap.util.buzz.state

class BulkMath : StateItemFeed {
    private val statusCounts = linkedMapOf<String, Int>()

    var itemCount: Int = 0
        private set

    var durationMs: Long = 0
        private set

    fun count(status: ActivityStatus<*>, durationMs: Long) {
        itemCount += 1
        this.durationMs += durationMs

        val code = status.code.lowercase()
        statusCounts[code] = statusCounts.getOrDefault(code, 0) + 1
    }

    override fun stateItems(name: PropertyName, push: PushStateItem) {
        if (itemCount == 0) {
            return
        }

        val state = name.activity.state
        push(state.append("item_count"), itemCount)
        push(state.append("duration_ms"), durationMs)

        for ((code, count) in statusCounts) {
            push(state.append("${code}_count"), count)
            push(state.append("${code}_rate"), count.toDouble() / itemCount)
        }
    }
}
