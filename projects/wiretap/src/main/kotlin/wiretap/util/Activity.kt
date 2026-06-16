package wiretap.util

abstract class Activity {
    open val name: String
        get() = this::class.simpleName ?: "Activity"

    open val tags: List<String> = emptyList()

    abstract class Buzz : Activity()

    abstract class Snap : Activity()

    abstract class Bulk<I : Buzz>(
        open val itemStatusLogOptions: Set<StatusLogOption> = defaultStatusLogOptions,
    ) : Buzz()
}
