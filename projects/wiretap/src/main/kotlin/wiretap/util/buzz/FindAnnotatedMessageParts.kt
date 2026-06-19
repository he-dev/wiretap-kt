package wiretap.util.buzz

import wiretap.util.MessagePart
import wiretap.util.MessagePartOptions
import wiretap.util.nullIfUnset

object FindAnnotatedMessageParts {
    fun on(source: Any?, addMessagePart: AddMessagePart) {
        if (source == null) return

        FindAnnotatedProperties.on<MessagePart>(source)
            .mapNotNull { property ->
                property.value(source)?.let { property to it }
            }
            .forEach { (property, value) ->
                val annotation = property.annotation
                addMessagePart(
                    property.name,
                    value,
                    MessagePartOptions(
                        label = annotation.label.nullIfUnset(),
                        separator = annotation.separator,
                        format = annotation.format.nullIfUnset(),
                    ),
                )
            }
    }
}
