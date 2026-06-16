package wiretap.util

import wiretap.util.buzz.FindAnnotatedProperties
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushMessagePart

object AnnotatedMessageParts {

    fun pushFrom(prefix: PropertyName, push: PushMessagePart, source: Any?) {
        if (source == null) return

        for (property in FindAnnotatedProperties.on<MessagePart>(source)) {
            val value = property.value(source) ?: continue
            val annotation = property.annotation

            // core: Null-label sentinel means no label; empty label means use the property name.
            when (annotation.label) {
                MessagePartNoLabel -> push("%s", value)
                "" -> push("${property.name}${annotation.separator}%s", value)
                else -> push("${annotation.label}${annotation.separator}%s", value)
            }
        }
    }
}
