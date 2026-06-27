package wiretap.util

import java.util.Locale

// TODO: Draft only. If this survives, MessagePartRegistry and LogPropertyRegistry can become thin facades over it.
class LogRegistry<Options : Any>(
    private val newOptions: () -> Options,
) : Iterable<LogRegistry.Item<Options>> {
    // core: Linked storage keeps insertion order while still allowing named pop/duplicate checks.
    private val entries = linkedMapOf<PropertyName, Item<Options>>()

    fun put(
        name: PropertyName,
        value: Any?,
        configure: Options.() -> Unit = {},
    ): Boolean {
        if (name in entries) return false

        val options = newOptions().apply(configure)
        entries[name] = Item.from(name, value, options)

        return true
    }

    fun pop(name: PropertyName): Item<Options>? =
        entries.remove(name)?.takeUnless { it is Item.Null<*> }

    operator fun get(name: PropertyName): Item<Options>? =
        entries[name]?.takeUnless { it is Item.Null<*> }

    fun clear() {
        entries.clear()
    }

    override fun iterator(): Iterator<Item<Options>> =
        entries.values.asSequence()
            .filter { it !is Item.Null<*> }
            .iterator()

    open class Item<Options : Any>(
        val name: PropertyName,
        val value: Any,
        val options: Options,
    ) {
        class Null<Options : Any>(name: PropertyName, options: Options) : Item<Options>(name, Unit, options)

        companion object {
            fun <Options : Any> from(name: PropertyName, value: Any?, options: Options): Item<Options> {
                return value?.let { Item(name, it, options) } ?: Null(name, options)
            }
        }
    }
}

// todo: rename the original options and remove this
class StatePropertyOptions {
    var cascade: Boolean = false
}

// todo: rename the original options and remove this
class MessageSnippetOptions {
    var label: String? = null
    var separator: String = ": "
    var format: String? = null
}

class StatePropertyBuilder(
    val root: PropertyName,
    private val stateProperties: LogRegistry<StatePropertyOptions>,
) {
    fun add(
        name: PropertyName,
        value: Any?,
        configure: StatePropertyOptions.() -> Unit = {},
    ): Boolean =
        stateProperties.put(root.activity.state + name, value, configure)
}

class MessageSnippetBuilder(
    val root: PropertyName,
    private val messageSnippets: LogRegistry<MessageSnippetOptions>,
    private val stateProperties: LogRegistry<StatePropertyOptions>,
) {
    fun add(
        name: PropertyName,
        value: Any?,
        configure: MessageSnippetOptions.() -> Unit = {},
    ): Boolean =
        messageSnippets.put(root.activity.state + name, value, configure)

    fun add(
        name: PropertyName,
        configure: MessageSnippetOptions.() -> Unit = {},
    ): Boolean =
        messageSnippets.put(root.activity.state + name, stateProperties[root.activity.state + name]?.value, configure)

    fun add(
        name: PropertyName,
        render: (Any) -> Any?,
        configure: MessageSnippetOptions.() -> Unit = {},
    ): Boolean {
        val value = stateProperties[root.activity.state + name]?.value?.let(render)
        return messageSnippets.put(root.activity.state + name, value, configure)
    }
}

interface StatePropertySource {
    fun StatePropertyBuilder.stateProperties()
}

interface MessageSnippetSource {
    fun MessageSnippetBuilder.messageSnippets()
}

internal val LogRegistry.Item<MessageSnippetOptions>.text: String
    get() {
        val label = options.label?.ifEmpty { name.toString() }
        val valueFormatted = options.format
            ?.let { String.format(Locale.ROOT, it, value) }
            ?: value.toString()

        return label?.let { "$it${options.separator}$valueFormatted" } ?: valueFormatted
    }


class DraftImportDocument : Activity.Buzz(), StatePropertySource {
    override fun StatePropertyBuilder.stateProperties() {
        add(PropertyName("records_parsed"), 0)
    }

    class Okay(private val recordsParsed: Int) :
        ActivityStatus.Okay<DraftImportDocument>(),
        StatePropertySource,
        MessageSnippetSource {
        override fun StatePropertyBuilder.stateProperties() {
            add(PropertyName("records_parsed"), recordsParsed)
        }

        override fun MessageSnippetBuilder.messageSnippets() {
            add(PropertyName("records_parsed"), "Records parsed") { label = "Records" }
            add(PropertyName("records_parsed")) { label = "Records" }
            add(PropertyName("records_parsed"), { "-$it" }) { label = "Records" }
        }
    }
}
