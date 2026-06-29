package wiretap.slf4j.util

import org.slf4j.Logger
import org.slf4j.event.Level
import wiretap.util.logging.ActivityLogger
import wiretap.util.logging.ActivityStatusLevel
import kotlin.collections.iterator

class LoggerAdapter(val logger: Logger) : ActivityLogger {
    override fun log(
        level: ActivityStatusLevel,
        details: Map<String, Any?>,
        message: String,
        exception: Throwable?
    ) {
        val builder = logger.atLevel(level.toSlf4jLevel())

        for ((key, value) in details) {
            builder.addKeyValue(key, value)
        }

        exception?.let { builder.setCause(it) }
        builder.log(message)
    }
}

internal fun ActivityStatusLevel.toSlf4jLevel(): Level =
    when (this) {
        ActivityStatusLevel.Error -> Level.ERROR
        ActivityStatusLevel.Warning -> Level.WARN
        ActivityStatusLevel.Info -> Level.INFO
    }
