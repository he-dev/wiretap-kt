package wiretap.util

data class DottedName(
    val parts: List<String> = emptyList(),
) {
    constructor(vararg parts: String) : this(parts.flatMap { it.split('.') })

    operator fun plus(next: DottedName): DottedName =
        copy(parts = parts + next.parts)

    fun append(vararg next: String): DottedName =
        copy(parts = parts + next)

    override fun toString(): String =
        parts.joinToString(".")

    companion object {
        fun parse(value: String): DottedName =
            DottedName(value)
    }
}

val DottedName.wiretap: DottedName get() = append("wiretap")
val DottedName.activity: DottedName get() = append("activity")
val DottedName.state: DottedName get() = append("state")
val DottedName.bulk: DottedName get() = append("bulk")
val DottedName.status: DottedName get() = append("status")
val DottedName.name: DottedName get() = append("name")
val DottedName.tags: DottedName get() = append("tags")
val DottedName.role: DottedName get() = append("role")
val DottedName.code: DottedName get() = append("code")
val DottedName.depth: DottedName get() = append("depth")
val DottedName.path: DottedName get() = append("path")
val DottedName.durationMs: DottedName get() = append("duration_ms")
val DottedName.traceId: DottedName get() = append("trace_id")
val DottedName.spanId: DottedName get() = append("span_id")
val DottedName.parentSpanId: DottedName get() = append("parent_span_id")
