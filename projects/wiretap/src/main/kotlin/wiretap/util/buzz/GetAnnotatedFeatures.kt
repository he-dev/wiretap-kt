package wiretap.util.buzz

import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.Detail
import wiretap.util.nullIfUnset

fun getAnnotatedFeatures(
    source: Any,
    report: (String, Any?, Detail) -> Unit,
) {
    findAnnotatedProperties<Detail>(source)
        .forEach { property ->
            val annotation = property.annotation
            val name = annotation.name.nullIfUnset() ?: property.name
            report(name, property.value(source), annotation)
        }
}
