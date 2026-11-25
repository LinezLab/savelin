package com.pinfold.app.network.image

/** Pure byte helpers for icon payloads; deliberately free of Android APIs so they are JVM-testable. */
object IconBytes {
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    /**
     * BitmapFactory cannot decode an ICO container, but many sites ship a PNG inside one. Unwrap
     * that PNG; any other payload is returned untouched so existing decoding behaviour is kept.
     */
    fun decodeCandidate(raw: ByteArray): ByteArray {
        if (raw.size < 22) return raw
        if (raw.u16(0) != 0 || raw.u16(2) != 1 || raw.u16(4) < 1) return raw
        val size = raw.u32(14)
        val offset = raw.u32(18)
        if (offset <= 0L || size <= 0L || offset + size > raw.size.toLong()) return raw
        val payload = raw.copyOfRange(offset.toInt(), (offset + size).toInt())
        return if (payload.startsWith(PNG_MAGIC)) payload else raw
    }

    private fun ByteArray.u16(index: Int): Int =
        (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(index: Int): Long =
        u16(index).toLong() or (u16(index + 2).toLong() shl 16)

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
