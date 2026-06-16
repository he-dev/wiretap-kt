package wiretap.slf4j.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import wiretap.util.Wiretap
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.ActivityLogRecord
import kotlin.collections.iterator

class ActivityLogger(val logger: Logger) : ActivityLogger {
    override fun log(record: ActivityLogRecord) {
        val builder = logger.atLevel(record.level.toSlf4jLevel())

        for ((key, value) in record.stateItems) {
            builder.addKeyValue(key, value)
        }

        record.status.exception?.let { builder.setCause(it) }
        builder.log(record.message)
    }

    companion object {
        fun getLogger(name: String = Wiretap.name): ActivityLogger =
            ActivityLogger(LoggerFactory.getLogger(name))

        fun getLogger(type: Class<*>): ActivityLogger =
            ActivityLogger(LoggerFactory.getLogger(type))
    }
}

internal fun ActivityStatusLevel.toSlf4jLevel(): Level =
    when (this) {
        ActivityStatusLevel.Error -> Level.ERROR
        ActivityStatusLevel.Warning -> Level.WARN
        ActivityStatusLevel.Info -> Level.INFO
    }
