package wiretap.util

import java.util.Locale

class MessagePartRegistry internal constructor(
    private val get: (PropertyName) -> Any? = { null },
) {
    // core: The insertion order is the default message order for parts not explicitly arranged.
    private val entries = linkedMapOf<PropertyName, Entry>()

    internal val values: Collection<Entry>
        get() = entries.values

    fun property(
        name: PropertyName,
        configure: MessagePartOptions.() -> Unit = {},
    ) = discrete(name, get(name), configure)

    fun discrete(
        name: PropertyName,
        value: Any?,
        configure: MessagePartOptions.() -> Unit = {},
    ) {
        entries[name] = Entry(name, value, MessagePartOptions().apply(configure))
    }

    internal fun push(entry: Entry) {
        entries[entry.name] = entry
    }

    internal fun pop(name: PropertyName): Entry? =
        entries.remove(name)

    internal fun clear() {
        entries.clear()
    }

    data class Entry(
        val name: PropertyName,
        val value: Any?,
        val options: MessagePartOptions,
    ) {
        val text: String
            get() {
                val label = options.label?.ifEmpty { name.toString() }
                val valueFormatted = options.format
                    ?.let { String.format(Locale.ROOT, it, value) }
                    ?: value.toString()

                return label?.let { "$it${options.separator}$valueFormatted" } ?: valueFormatted
            }
    }
}

interface MessagePartSource {
    fun MessagePartRegistry.messageParts(root: PropertyName)
}
