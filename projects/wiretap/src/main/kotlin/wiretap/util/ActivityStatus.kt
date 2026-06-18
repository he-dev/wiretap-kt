package wiretap.util

import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.GetLogProperty
import wiretap.util.buzz.MessagePartSource
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.code
import wiretap.util.buzz.role
import wiretap.util.buzz.status

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
) : LogPropertySource, MessagePartSource {
    open val code: String
        get() = this::class.simpleName ?: "Status"

    open val level: ActivityStatusLevel = ActivityStatusLevel.Info

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        add(root.status.code, code)
        add(root.status.role, (this as? ActivityStatusRole)?.role)
    }

    override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
        AnnotatedMessageParts.addFrom(add, this)
    }

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

        override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
            exception?.message?.let { add("exception", it) }
        }
    }

    class Void<A : Activity>(
        val reason: String = "The activity scope exited without an explicit last status.",
    ) : ActivityStatus<A>(), Last {
        override val code: String = "Void"

        override val level: ActivityStatusLevel = ActivityStatusLevel.Warning

        override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
            add("reason", reason)
        }
    }
}
