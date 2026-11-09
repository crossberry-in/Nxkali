package com.crossberry.noxs.emulator

import java.io.DataInputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * RFB VNC password authentication (security type 2), used by the local
 * Kali NetHunter KeX server. Password bytes are never persisted by this class.
 */
object VncAuth {
    const val SECURITY_TYPE_VNC_AUTH = 2

    fun authenticate(
        input: DataInputStream,
        output: OutputStream,
        password: CharArray,
        protocolMinor: Int
    ) {
        require(password.isNotEmpty()) { "Enter the KeX password" }
        val challenge = ByteArray(16)
        val key = ByteArray(8)
        var response = ByteArray(0)
        try {
            input.readFully(challenge)
            for (i in 0 until minOf(8, password.size)) {
                val value = password[i].code
                require(value in 0..255) { "KeX password must use single-byte characters" }
                key[i] = reverseBits(value.toByte())
            }
            val cipher = Cipher.getInstance("DES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DES"))
            response = cipher.doFinal(challenge)
            output.write(response)
            output.flush()

            val result = input.readInt()
            if (result != 0) {
                var reason = ""
                if (protocolMinor >= 8) {
                    val length = input.readInt().toLong() and 0xFFFFFFFFL
                    if (length in 1..4096) {
                        val bytes = ByteArray(length.toInt())
                        input.readFully(bytes)
                        reason = String(bytes, Charsets.UTF_8)
                        bytes.fill(0)
                    }
                }
                throw IllegalStateException(
                    if (reason.isBlank()) "KeX authentication failed" else "KeX authentication failed: $reason"
                )
            }
        } finally {
            challenge.fill(0)
            key.fill(0)
            response.fill(0)
        }
    }

    private fun reverseBits(value: Byte): Byte {
        var x = value.toInt() and 0xFF
        x = ((x and 0x55) shl 1) or ((x ushr 1) and 0x55)
        x = ((x and 0x33) shl 2) or ((x ushr 2) and 0x33)
        x = ((x and 0x0F) shl 4) or ((x ushr 4) and 0x0F)
        return x.toByte()
    }
}
