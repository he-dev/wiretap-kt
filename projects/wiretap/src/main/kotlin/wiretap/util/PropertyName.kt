package wiretap.util

data class PropertyName(
    val parts: List<String> = emptyList(),
    val separator: String = ".",
) {
    constructor(vararg parts: String) : this(parts.toList())

    fun append(vararg next: String): PropertyName =
        copy(parts = parts + next)

    override fun toString(): String =
        parts.joinToString(separator)
}

val PropertyName.wiretap: PropertyName get() = append("wiretap")
val PropertyName.activity: PropertyName get() = append("activity")
val PropertyName.state: PropertyName get() = append("state")
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
