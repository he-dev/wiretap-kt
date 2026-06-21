package wiretap.util

import java.security.SecureRandom
import java.util.HexFormat
import wiretap.util.buzz.AddLogProperty
import wiretap.util.buzz.LogPropertySource

class TraceContext internal constructor(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
) : LogPropertySource {
    override fun logProperties(root: PropertyName, add: AddLogProperty) {
        with(add) {
            localOnly(root.traceId, traceId)
            localOnly(root.spanId, spanId)
            parentSpanId?.let { localOnly(root.parentSpanId, it) }
        }
    }

    companion object {
        internal fun create(
            parent: TraceContext?,
            traceId: String? = null,
        ): TraceContext =
            TraceContext(
                traceId = parent?.traceId ?: traceId ?: randomHex(16),
                spanId = randomHex(8),
                parentSpanId = parent?.spanId,
            )
    }
}

private val random = SecureRandom()
private val hex = HexFormat.of()

private fun randomHex(byteCount: Int): String =
    hex.formatHex(ByteArray(byteCount).also(random::nextBytes))
