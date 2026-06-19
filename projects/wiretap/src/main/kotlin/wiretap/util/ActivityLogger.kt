package wiretap.util

interface ActivityLogger {
    fun log(entry: LogEntry)

    object Noop : ActivityLogger {
        override fun log(entry: LogEntry) = Unit
    }
}
