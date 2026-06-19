package wiretap.util

data class LogEntry(
    val level: ActivityStatusLevel,
    val message: String,
    val properties: Map<String, Any?>,
    val exception: Throwable? = null,
) : Map<String, Any?> by properties {
    operator fun get(property: PropertyName): Any? =
        get(property.toString())
}
