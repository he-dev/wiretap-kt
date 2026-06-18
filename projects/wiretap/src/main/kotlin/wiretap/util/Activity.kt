package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.GetLogProperty
import wiretap.util.buzz.LogPropertySource
import wiretap.util.buzz.MessagePartSource
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.code
import wiretap.util.buzz.depth
import wiretap.util.buzz.name
import wiretap.util.buzz.path
import wiretap.util.buzz.role
import wiretap.util.buzz.status
import wiretap.util.buzz.tags

abstract class Activity : LogPropertySource, MessagePartSource {
    open val name: String
        get() = this::class.simpleName!!

    open val tags: Set<String> = emptySet()

    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        add(root.name, name)
        if (tags.isNotEmpty()) {
            add(root.tags, tags)
        }
    }

    override fun messageParts(root: PropertyName, get: GetLogProperty, add: AddMessagePart) {
        add(root.name, "${name}[${get(root.status.code)}]")
    }

    abstract class Buzz : Activity()

    abstract class Snap : Activity()

    abstract class Bulk<I : Buzz>(
        open val itemStatusLogOptions: Set<StatusLogOption> = bothStatusLogOptions,
    ) : Buzz()
}
