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
        val add = object : AddLogProperty {
            override fun localOnly(key: PropertyName, value: Any?) {
                value?.let { put(key.toString(), it) }
            }

            override fun cascading(key: PropertyName, value: Any?) {
                localOnly(key, value)
            }
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
) {
    annotatedProperties<StateItem>(source)
        .forEach { property ->
            val name = property.annotation.name.nullIfUnset() ?: property.name
            val value = property.value(source)

            if (property.annotation.cascade) {
                add.cascading(prefix.append(name), value)
            } else {
                add.localOnly(prefix.append(name), value)
            }
        }
}

internal fun AddLogProperty.cascadingOnly(): AddLogProperty =
    object : AddLogProperty {
        override fun localOnly(key: PropertyName, value: Any?) = Unit

        override fun cascading(key: PropertyName, value: Any?) {
            this@cascadingOnly.cascading(key, value)
        }
    }
