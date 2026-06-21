package wiretap.util

import java.security.SecureRandom
import java.util.HexFormat

class TraceContext internal constructor(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
) {

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
