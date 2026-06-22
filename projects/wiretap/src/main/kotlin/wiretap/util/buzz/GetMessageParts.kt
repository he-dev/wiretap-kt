package wiretap.util.buzz

import wiretap.util.MessagePart
import wiretap.util.MessagePartMap
import wiretap.util.MessagePartOptions
import wiretap.util.PropertyName
import wiretap.util.nullIfUnset

fun getMessageParts(
    root: PropertyName,
    get: GetLogProperty,
    vararg sources: Any?,
): MessagePartMap {
    val parts = MessagePartMap()
    val add = AddMessagePart(parts::push)

    sources.asSequence().filterNotNull().forEach { source ->
        annotatedProperties<MessagePart>(source)
            .mapNotNull { property ->
                property.value(source)?.let { property to it }
            }
            .forEach { (property, value) ->
                val annotation = property.annotation
                add(
                    property.name,
                    value,
                    MessagePartOptions(
                        label = annotation.label.nullIfUnset(),
                        separator = annotation.separator,
                        format = annotation.format.nullIfUnset(),
                    ),
                )
            }

        (source as? MessagePartSource)?.messageParts(root, get, add)
    }

    return parts
}
