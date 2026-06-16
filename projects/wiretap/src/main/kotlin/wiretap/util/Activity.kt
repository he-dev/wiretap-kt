package wiretap.util

abstract class Activity {
    open val name: String
        get() = this::class.simpleName!!



    open val tags: Set<String> = emptySet()

    abstract class Buzz : Activity()

    abstract class Snap : Activity()

    abstract class Bulk<I : Buzz>(
        open val itemStatusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    ) : Buzz()
}
