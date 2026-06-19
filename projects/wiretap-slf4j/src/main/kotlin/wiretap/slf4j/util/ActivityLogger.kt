package wiretap.slf4j.util

import org.slf4j.Logger
import org.slf4j.event.Level
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
}

internal fun ActivityStatusLevel.toSlf4jLevel(): Level =
    when (this) {
        ActivityStatusLevel.Error -> Level.ERROR
        ActivityStatusLevel.Warning -> Level.WARN
        ActivityStatusLevel.Info -> Level.INFO
    }
