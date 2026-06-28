package wiretap.util

interface ActivityLogger {
    fun log(
        level: ActivityStatusLevel,
        details: Map<String, Any?>,
        message: String,
        exception: Throwable?
    )

    object Noop : ActivityLogger {
        override fun log(level: ActivityStatusLevel, details: Map<String, Any?>, message: String, exception: Throwable?) = Unit
    }
}
