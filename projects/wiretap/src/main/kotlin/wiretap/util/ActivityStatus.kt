package wiretap.util

enum class ActivityStatusLevel {
    Info,
    Warning,
    Error,
}

sealed interface ActivityStatusRole {
    val role: String
}

interface First : ActivityStatusRole {
    override val role: String
        get() = "first"
}

interface Last : ActivityStatusRole {
    override val role: String
        get() = "last"
}

abstract class ActivityStatus<A : Activity>(
    open val exception: Throwable? = null,
) {
    open val code: String
        get() = this::class.simpleName ?: "Status"

    open val level: ActivityStatusLevel = ActivityStatusLevel.Info

    class Ready<A : Activity> : ActivityStatus<A>(), First {
        override val code: String = "Ready"
    }

    abstract class Okay<A : Activity> : ActivityStatus<A>(), Last {
        override val code: String = "Okay"
    }

    abstract class Noop<A : Activity> : ActivityStatus<A>(), Last {
        override val code: String = "Noop"
    }

    abstract class Fail<A : Activity>(
        override val exception: Throwable? = null,
    ) : ActivityStatus<A>(exception), Last {
        override val code: String = "Fail"

        override val level: ActivityStatusLevel = ActivityStatusLevel.Error

        @MessagePart("Exception")
        val exceptionMessage: String?
            get() = exception?.message
    }

    class Void<A : Activity>(
        @MessagePart
        val reason: String = "The activity scope exited without an explicit last status.",
    ) : ActivityStatus<A>(), Last {
        override val code: String = "Void"

        override val level: ActivityStatusLevel = ActivityStatusLevel.Warning
    }
}
