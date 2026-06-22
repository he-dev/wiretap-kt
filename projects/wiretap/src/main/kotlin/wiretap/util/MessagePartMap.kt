package wiretap.util

import java.util.Locale

class MessagePartMap internal constructor(
    private val entriesByName: MutableMap<PropertyName, Entry> = linkedMapOf(),
) : MutableMap<PropertyName, MessagePartMap.Entry> by entriesByName {
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

    fun push(
        name: PropertyName,
        value: Any?,
        options: MessagePartOptions = MessagePartOptions(),
    ) {
        this[name] = Entry(name, value, options)
    }

    fun pop(name: PropertyName): Entry? =
        remove(name)
}