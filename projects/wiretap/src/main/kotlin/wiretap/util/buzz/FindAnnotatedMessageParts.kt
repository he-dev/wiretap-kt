package wiretap.util.buzz

import wiretap.util.MessagePart
import wiretap.util.nullIfUnset

object FindAnnotatedMessageParts {

    fun on(source: Any?, addMessagePart: AddMessagePart) {
        if (source == null) return

        for (property in FindAnnotatedProperties.on<MessagePart>(source)) {
            val value = property.value(source) ?: continue
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