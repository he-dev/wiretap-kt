package wiretap.util.buzz

import wiretap.util.PropertyName
import wiretap.util.StateItem
import wiretap.util.activity
import wiretap.util.nullIfUnset
import wiretap.util.state

fun getLogProperties(
    root: PropertyName,
    vararg sources: Any?,
): Map<String, Any?> =
    buildMap {
        val add = AddLogProperty { name, value ->
            value?.let { put(name, it) }
        }

        sources.asSequence().filterNotNull().forEach { source ->
            (source as? LogPropertySource)?.logProperties(root, add)
            addAnnotatedLogProperties(root.activity.state, source, add)
        }
    }

internal fun addAnnotatedLogProperties(
    prefix: PropertyName,
    source: Any,
    add: AddLogProperty,
    cascadingOnly: Boolean = false,
) {
    annotatedProperties<StateItem>(source)
        .filter { !cascadingOnly || it.annotation.cascade }
        .forEach { property ->
            val name = property.annotation.name.nullIfUnset() ?: property.name
            add(prefix.append(name), property.value(source))
        }
}
