package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource

abstract class Activity : LogPropertySource {
    open val name: String
        get() = this::class.simpleName!!

    open val tags: Set<String> = emptySet()

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        add(root.activity.name, name)
        if (tags.isNotEmpty()) {
            add(root.activity.tags, tags)
        }
    }

    abstract class Buzz : Activity()

    abstract class Snap : Activity()

    abstract class Bulk<I : Buzz> : Buzz()
}
