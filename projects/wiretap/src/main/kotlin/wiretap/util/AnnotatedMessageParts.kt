package wiretap.util

import wiretap.util.buzz.FindAnnotatedProperties
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.AddMessagePart

object AnnotatedMessageParts {

    fun addFrom(addMessagePart: AddMessagePart, source: Any?) {
        if (source == null) return

        for (property in FindAnnotatedProperties.on<MessagePart>(source)) {
            val value = property.value(source) ?: continue
            val annotation = property.annotation

            // core: Null-label sentinel means no label; empty label means use the property name.
            when (annotation.label) {
                MessagePartNoLabel -> addMessagePart("%s", value)
                "" -> addMessagePart("${property.name}${annotation.separator}%s", value)
                else -> addMessagePart("${annotation.label}${annotation.separator}%s", value)
            }
        }
    }
}
