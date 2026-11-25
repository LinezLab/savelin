package com.pinfold.app.network.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IconBytesTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

    private fun ico(payload: ByteArray): ByteArray {
        val header = ByteArray(22)
        header[2] = 1
        header[4] = 1
        header[6] = 32
        header[7] = 32
        writeU32(header, 14, payload.size)
        writeU32(header, 18, 22)
        return header + payload
    }

    private fun writeU32(target: ByteArray, at: Int, value: Int) {
        target[at] = (value and 0xFF).toByte()
        target[at + 1] = ((value shr 8) and 0xFF).toByte()
        target[at + 2] = ((value shr 16) and 0xFF).toByte()
        target[at + 3] = ((value shr 24) and 0xFF).toByte()
    }

    @Test
    fun unwrapsPngInsideIco() {
        assertArrayEquals(png, IconBytes.decodeCandidate(ico(png)))
    }

    @Test
    fun leavesNonIcoAndBmpPayloadsUntouched() {
        assertSame(png, IconBytes.decodeCandidate(png))
        val bmpIco = ico(byteArrayOf(0x42, 0x4D, 1, 2, 3))
        assertArrayEquals(bmpIco, IconBytes.decodeCandidate(bmpIco))
    }

    @Test
    fun ignoresMalformedIcoOffsets() {
        val truncated = byteArrayOf(0, 0, 1, 0, 1, 0, 0x20, 0, 0, 0, 0x10, 0, 0, 0)
        assertEquals(truncated.toList(), IconBytes.decodeCandidate(truncated).toList())
    }
}
