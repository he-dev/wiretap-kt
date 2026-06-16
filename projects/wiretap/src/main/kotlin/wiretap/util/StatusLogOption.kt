package wiretap.util

enum class StatusLogOption {
    First,
    Last,
}

val defaultStatusLogOptions: Set<StatusLogOption> =
    setOf(StatusLogOption.First, StatusLogOption.Last)
