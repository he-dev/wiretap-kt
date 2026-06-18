package wiretap.util

import wiretap.util.buzz.FindAnnotatedProperties
import wiretap.util.buzz.AddMessagePart
import wiretap.util.buzz.MessagePartOptions

object AnnotatedMessageParts {

    fun addFrom(addMessagePart: AddMessagePart, source: Any?) {
        if (source == null) return

        for (property in FindAnnotatedProperties.on<MessagePart>(source)) {
            val value = property.value(source) ?: continue
            val annotation = property.annotation

            addMessagePart(
                property.name,
                value,
                MessagePartOptions(
                    label = annotation.label.takeUnless { it == MessagePartNoLabel },
                    separator = annotation.separator,
                ),
            )
        }
    }
}
