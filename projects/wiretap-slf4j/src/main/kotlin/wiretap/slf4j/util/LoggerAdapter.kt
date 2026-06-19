package wiretap.slf4j.util

import org.slf4j.Logger
import org.slf4j.event.Level
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.LogEntry
import kotlin.collections.iterator

class LoggerAdapter(val logger: Logger) : ActivityLogger {
    override fun log(entry: LogEntry) {
        val builder = logger.atLevel(entry.level.toSlf4jLevel())

        for ((key, value) in entry.properties) {
            builder.addKeyValue(key, value)
        }

        entry.exception?.let { builder.setCause(it) }
        builder.log(entry.message)
    }
}

internal fun ActivityStatusLevel.toSlf4jLevel(): Level =
    when (this) {
        ActivityStatusLevel.Error -> Level.ERROR
        ActivityStatusLevel.Warning -> Level.WARN
        ActivityStatusLevel.Info -> Level.INFO
    }
