package wiretap.util.buzz

import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.Detail

fun getAnnotatedFeatures(
    source: Any,
    report: (String, Any?, Detail) -> Unit,
) {
    findAnnotatedProperties<Detail>(source)
        .forEach { property ->
            val annotation = property.annotation
            val name = annotation.name.ifEmpty { property.name }
            report(name, property.value(source), annotation)
        }
}
