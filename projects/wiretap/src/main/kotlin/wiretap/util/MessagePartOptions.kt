package wiretap.util

import wiretap.util.buzz.LabelStyle

data class MessagePartOptions(
    val label: String? = null,
    val style: String = LabelStyle.AS_IS,
    val separator: String = ": ",
    val format: String? = null,
)
