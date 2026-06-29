package wiretap.util.buzz

import wiretap.meta.buzz.findAnnotatedProperties
import wiretap.util.Activity
import wiretap.util.ActivityStatus
import wiretap.util.DottedName
import wiretap.util.data.DetailMap
import wiretap.util.data.Remark
import wiretap.util.data.RemarkBuilder
import wiretap.util.data.RemarkMap
import wiretap.util.data.RemarkSource

internal fun collectRemarks(
    root: DottedName,
    activity: Activity,
    status: ActivityStatus<*>,
    details: DetailMap,
    remarks: RemarkMap,
) {
    for (source in listOf(activity, status)) {
        val builder = RemarkBuilder(root, details, remarks)

        if (source is RemarkSource) {
            with(source) {
                builder.remarks()
            }
        }

        findAnnotatedProperties<Remark>(source).forEach { property ->
            builder.add(DottedName(property.name), property.value(source)) {
                label = property.annotation.label
                separator = property.annotation.separator
                format = property.annotation.format
            }
        }
    }
}
