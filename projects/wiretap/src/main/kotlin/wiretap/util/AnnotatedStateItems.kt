package wiretap.util

import wiretap.util.buzz.FindAnnotatedProperties
import wiretap.util.buzz.PropertyName
import wiretap.util.buzz.PushLogProperty

object AnnotatedStateItems {
    fun pushFrom(prefix: PropertyName, push: PushLogProperty, vararg feeds: Any) {
        for (feed in feeds) {
            for (property in FindAnnotatedProperties.on<StateItem>(feed)) {
                val name = property.annotation.name.ifBlank { property.name }
                push(prefix.append(name), property.value(feed))
            }
        }
    }
}
