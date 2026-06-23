package wiretap.util.buzz

import wiretap.util.MessagePart
import wiretap.util.MessagePartMap
import wiretap.util.PropertyName
import wiretap.util.nullIfUnset

fun getMessageParts(
    root: PropertyName,
    properties: Map<String, Any?>,
    vararg sources: Any?,
): MessagePartMap {
    val parts = MessagePartMap()
    val add = AddMessagePart(parts) { properties[it.toString()] }

    sources.asSequence().filterNotNull().forEach { source ->
        findAnnotatedProperties<MessagePart>(source)
            .mapNotNull { property ->
                property.value(source)?.let { property to it }
            }
            .forEach { (property, value) ->
                val annotation = property.annotation
                add.discrete(PropertyName(property.name), value) {
                    label = annotation.label.nullIfUnset()
                    separator = annotation.separator
                    format = annotation.format.nullIfUnset()
                }
            }

        (source as? MessagePartSource)?.let { messagePartSource ->
            with(messagePartSource) {
                add.messageParts(root)
            }
        }
    }

    return parts
}
