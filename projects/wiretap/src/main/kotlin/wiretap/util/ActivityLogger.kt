package wiretap.util

interface ActivityLogger {
    fun log(record: ActivityLogRecord)

    object Noop : ActivityLogger {
        override fun log(record: ActivityLogRecord) = Unit
    }
}
