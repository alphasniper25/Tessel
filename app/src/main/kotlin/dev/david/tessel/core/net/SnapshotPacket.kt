package dev.david.tessel.core.net

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.DataFormatException

/** Lossless, independently decodable snapshots. No delta baseline to lose on reconnect.
 * Small or poorly compressible snapshots stay in the original binary format. */
object SnapshotPacket {
    const val MIN_BYTES = 768
    private const val HEADER = 8
    private const val MAX_BODY = MAX_MESSAGE - 4

    /** Game-thread owned buffers. Only the returned packet is allocated for transport. */
    class Encoder {
        private val deflater = Deflater(Deflater.BEST_SPEED)
        private val scratch = ByteArray(MAX_MESSAGE)
        private val header = ByteBuffer.wrap(scratch).order(ByteOrder.LITTLE_ENDIAN)
        fun encode(raw: ByteBuffer): ByteArray {
            val size = raw.position()
            require(size in 4..MAX_MESSAGE)
            if (size < MIN_BYTES) return Wire.finish(raw)
            deflater.reset()
            deflater.setInput(raw.array(), raw.arrayOffset() + 4, size - 4)
            deflater.finish()
            val compressed = deflater.deflate(scratch, HEADER, scratch.size - HEADER)
            if (!deflater.finished() || compressed + HEADER > size - size / 8) return Wire.finish(raw)
            Wire.begin(header, Wire.SNAPSHOT_PACKED)
            header.putInt(size - 4)
            return scratch.copyOf(compressed + HEADER)
        }
        fun close() = deflater.end()
    }

    /** Returned buffer is valid until the next decode on the same thread. */
    class Decoder {
        private val inflater = Inflater()
        private val scratch = ByteArray(MAX_BODY)
        private val view = ByteBuffer.wrap(scratch).order(ByteOrder.LITTLE_ENDIAN)
        fun decode(bytes: ByteArray): ByteBuffer? {
            if (bytes.size !in 4..MAX_MESSAGE) return null
            when (Wire.typeOf(bytes)) {
                Wire.SNAPSHOT -> return Wire.open(bytes)
                Wire.SNAPSHOT_PACKED -> Unit
                else -> return null
            }
            if (bytes.size <= HEADER) return null
            val size = Wire.open(bytes).int
            if (size !in 1..MAX_BODY) return null
            inflater.reset()
            inflater.setInput(bytes, HEADER, bytes.size - HEADER)
            return try {
                var written = 0
                while (!inflater.finished() && written < size) {
                    val n = inflater.inflate(scratch, written, size - written)
                    if (n == 0) break
                    written += n
                }
                // Reject truncation, trailing bytes, oversized output and invalid checksums.
                if (written != size || !inflater.finished() || inflater.remaining != 0) return null
                view.clear()
                view.limit(size)
                view
            } catch (_: DataFormatException) { null }
        }
        fun close() = inflater.end()
    }
}
