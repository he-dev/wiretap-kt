package wiretap.util

import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.FindAnnotatedProperties
import wiretap.util.buzz.PropertyName

object AnnotatedStateItems {
    fun pushFromSelf(
        prefix: PropertyName,
        push: AddLogProperty,
        source: Any,
    ) {
        FindAnnotatedProperties.on<StateItem>(source)
            .forEach { property ->
                val name = property.annotation.name.nullIfUnset() ?: property.name
                push(prefix.append(name), property.value(source))
            }
    }

    fun pushFromAncestors(
        prefix: PropertyName,
        push: AddLogProperty,
        ancestors: Sequence<Any>,
    ) {
        ancestors.forEach { source ->
            FindAnnotatedProperties.on<StateItem>(source)
                .filter { it.annotation.cascade }
                .forEach { property ->
                    val name = property.annotation.name.nullIfUnset() ?: property.name
                    push(prefix.append(name), property.value(source))
                }
        }
    }
}
