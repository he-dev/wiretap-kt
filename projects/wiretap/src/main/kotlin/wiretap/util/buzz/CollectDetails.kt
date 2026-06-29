package wiretap.util.buzz

import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.Activity
import wiretap.util.data.Detail
import wiretap.util.data.DetailBuilder
import wiretap.util.data.DetailMap
import wiretap.util.data.DetailSource
import wiretap.util.DottedName
import wiretap.util.activity
import wiretap.util.state

internal fun collectDetails(
    root: DottedName,
    activities: List<Activity>,
    details: DetailMap,
) {
    activities.forEachIndexed { level, source ->
        val builder = DetailBuilder(root.activity.state, level, details)

        if (source is DetailSource) {
            with(source) {
                builder.details()
            }
        }

        findAnnotatedProperties<Detail>(source).forEach { property ->
            val annotation = property.annotation
            val name = annotation.name.ifEmpty { property.name }
            builder.add(DottedName(name), property.value(source)) {
                cascade = annotation.cascade
            }
        }
    }
}
