package wiretap.util

data class PropertyName(
    val parts: List<String> = emptyList(),
) {
    constructor(vararg parts: String) : this(parts.flatMap { it.split('.') })

    operator fun plus(next: PropertyName): PropertyName =
        copy(parts = parts + next.parts)

    fun append(vararg next: String): PropertyName =
        copy(parts = parts + next)

    override fun toString(): String =
        parts.joinToString(".")

    companion object {
        fun parse(value: String): PropertyName =
            PropertyName(value)
    }
}

val PropertyName.wiretap: PropertyName get() = append("wiretap")
val PropertyName.activity: PropertyName get() = append("activity")
val PropertyName.state: PropertyName get() = append("state")
val PropertyName.bulk: PropertyName get() = append("bulk")
val PropertyName.status: PropertyName get() = append("status")
val PropertyName.name: PropertyName get() = append("name")
val PropertyName.tags: PropertyName get() = append("tags")
val PropertyName.role: PropertyName get() = append("role")
val PropertyName.code: PropertyName get() = append("code")
val PropertyName.depth: PropertyName get() = append("depth")
val PropertyName.path: PropertyName get() = append("path")
val PropertyName.durationMs: PropertyName get() = append("duration_ms")
val PropertyName.traceId: PropertyName get() = append("trace_id")
val PropertyName.spanId: PropertyName get() = append("span_id")
val PropertyName.parentSpanId: PropertyName get() = append("parent_span_id")
