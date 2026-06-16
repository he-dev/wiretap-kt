package wiretap.util

enum class StatusLogOption {
    First,
    Last,
}

val bothStatusLogOptions: Set<StatusLogOption> =
    setOf(StatusLogOption.First, StatusLogOption.Last)
