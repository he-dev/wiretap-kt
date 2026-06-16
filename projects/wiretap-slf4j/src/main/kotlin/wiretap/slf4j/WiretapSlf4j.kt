package wiretap.slf4j

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import wiretap.Wiretap
import wiretap.util.ActivityLogger
import wiretap.util.ActivityStatusLevel
import wiretap.util.buzz.ActivityLogRecord

class WiretapSlf4j(logger: Logger) : ActivityLogger {
    private val inner = Slf4jActivityLogger(logger)

    override fun log(record: ActivityLogRecord) {
        inner.log(record)
    }

    companion object {
        fun getLogger(name: String = Wiretap.name): WiretapSlf4j =
            WiretapSlf4j(LoggerFactory.getLogger(name))

        fun getLogger(type: Class<*>): WiretapSlf4j =
            WiretapSlf4j(LoggerFactory.getLogger(type))
    }
}

class Slf4jActivityLogger(private val logger: Logger) : ActivityLogger {
    override fun log(record: ActivityLogRecord) {
        val builder = logger.atLevel(record.level.toSlf4jLevel())

        for ((key, value) in record.stateItems) {
            builder.addKeyValue(key, value)
        }

        record.status.exception?.let { builder.setCause(it) }
        builder.log(record.message)
    }

    private fun ActivityStatusLevel.toSlf4jLevel(): Level =
        when (this) {
            ActivityStatusLevel.Info -> Level.INFO
            ActivityStatusLevel.Warning -> Level.WARN
            ActivityStatusLevel.Error -> Level.ERROR
        }
}
