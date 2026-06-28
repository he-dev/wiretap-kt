package wiretap.util.buzz

import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.Remark

fun getAnnotatedSnippets(
    source: Any,
    report: (String, Any?, Remark) -> Unit,
) {
    findAnnotatedProperties<Remark>(source)
        .forEach { property ->
            report(property.name, property.value(source), property.annotation)
        }

}
