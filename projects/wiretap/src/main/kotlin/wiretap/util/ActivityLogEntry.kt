package wiretap.util

import wiretap.util.buzz.PropertyName

data class ActivityLogEntry(
    val level: ActivityStatusLevel,
    val message: String,
    val properties: Map<String, Any?>,
    val exception: Throwable? = null,
) : Map<String, Any?> by properties {
    operator fun get(property: PropertyName): Any? =
        get(property.toString())
}
