package org.itantra.transceiver.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * iTantra Binary Neural Radio Access Protocol (iTantra-Packet)
 * Ultra-low-bitrate framing for transmitting speech metadata over constrained links.
 * Compatible with the core Python testbed encoder/decoder.
 */
data class TantraPacket(
    val text: String,
    val langId: Int,
    val isEmergency: Boolean = false,
    val isAlert: Boolean = false,
    val isPtt: Boolean = true,
    val seqNum: Int = 1,
    val nodeId: Int = 0
) {
    val priorityLevel: Int
        get() = when {
            isEmergency -> 1 // Priority 1: SOS (Red)
            isAlert -> 2     // Priority 2: Alert (Yellow)
            else -> 3        // Priority 3: Normal (Green)
        }

    companion object {
        const val MAGIC_BYTE: Byte = 0x54 // 'T'
        const val FLAG_ALERT_EMERGENCY: Byte = 0x80.toByte() // Bit 7: SOS Red
        const val FLAG_PTT_MODE: Byte = 0x40.toByte()        // Bit 6
        const val FLAG_TACTICAL_ALERT: Byte = 0x20.toByte()  // Bit 5: Alert Yellow
        const val FLAG_AUTH_HMAC: Byte = 0x10.toByte()       // Bit 4: AES/HMAC Authenticated Frame
        const val LANG_MASK: Byte = 0x0F                     // Bits 0-3

        val DEFAULT_SECRET_KEY = "iTantra-Tactical-Team-Key-2026".toByteArray(StandardCharsets.UTF_8)

        // 10 Mandated Indian Languages Mapping
        val LANG_NAMES = arrayOf(
            "Hindi",     // 0
            "English",   // 1
            "Gujarati",  // 2
            "Marathi",   // 3
            "Kannada",   // 4
            "Malayalam", // 5
            "Tamil",     // 6
            "Telugu",    // 7
            "Odia",      // 8
            "Bengali"    // 9
        )

        val LANG_CODES = arrayOf("hi", "en", "gu", "mr", "kn", "ml", "ta", "te", "or", "bn")

        /**
         * Auto-LID: Automatically detects the language from the text script.
         */
        fun detectLanguage(text: String): Int {
            var latinCount = 0
            var devanagariCount = 0
            var tamilCount = 0
            var teluguCount = 0
            var kannadaCount = 0
            var malayalamCount = 0
            var bengaliCount = 0
            var gujaratiCount = 0
            var odiaCount = 0

            var hasMarathiChar = false

            for (ch in text) {
                val cp = ch.code
                when {
                    (cp in 65..90) || (cp in 97..122) -> latinCount++
                    cp == 0x0933 -> { devanagariCount++; hasMarathiChar = true }
                    cp in 0x0900..0x097F -> devanagariCount++
                    cp in 0x0B80..0x0BFF -> tamilCount++
                    cp in 0x0C00..0x0C7F -> teluguCount++
                    cp in 0x0C80..0x0CFF -> kannadaCount++
                    cp in 0x0D00..0x0D7F -> malayalamCount++
                    cp in 0x0980..0x09FF -> bengaliCount++
                    cp in 0x0A80..0x0AFF -> gujaratiCount++
                    cp in 0x0B00..0x0B7F -> odiaCount++
                }
            }

            return when {
                tamilCount > 0 -> 6      // Tamil
                teluguCount > 0 -> 7     // Telugu
                kannadaCount > 0 -> 4    // Kannada
                malayalamCount > 0 -> 5  // Malayalam
                bengaliCount > 0 -> 9    // Bengali
                gujaratiCount > 0 -> 2   // Gujarati
                odiaCount > 0 -> 8       // Odia
                devanagariCount > 0 -> if (hasMarathiChar) 3 else 0 // 3 = Marathi, 0 = Hindi
                latinCount > 0 -> 1      // English
                else -> 0                // Default Hindi
            }
        }

        /**
         * Computes CRC-16 CCITT (polynomial 0x1021, init 0xFFFF).
         */
        fun crc16Ccitt(data: ByteArray, length: Int): Int {
            var crc = 0xFFFF
            for (i in 0 until length) {
                crc = crc xor ((data[i].toInt() and 0xFF) shl 8)
                for (j in 0 until 8) {
                    crc = if ((crc and 0x8000) != 0) {
                        ((crc shl 1) xor 0x1021) and 0xFFFF
                    } else {
                        (crc shl 1) and 0xFFFF
                    }
                }
            }
            return crc and 0xFFFF
        }

        /**
         * Computes HMAC-SHA256 truncated to 8 bytes for micro-packet authentication.
         */
        fun computeHmac(data: ByteArray, offset: Int, length: Int, key: ByteArray = DEFAULT_SECRET_KEY): ByteArray {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
            mac.update(data, offset, length)
            val full = mac.doFinal()
            return full.copyOfRange(0, 8)
        }

        /**
         * Parses and validates raw bytes into a TantraPacket.
         */
        fun decode(bytes: ByteArray, key: ByteArray = DEFAULT_SECRET_KEY): TantraPacket {
            if (bytes.size < 8) {
                throw IllegalArgumentException("Packet too short: ${bytes.size} bytes (min 8)")
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = buffer.get()
            if (magic != MAGIC_BYTE) {
                throw IllegalArgumentException("Invalid magic byte: 0x%02X".format(magic))
            }

            val flags = buffer.get().toInt()
            val seqNum = buffer.short.toInt() and 0xFFFF
            val payloadLen = buffer.short.toInt() and 0xFFFF
            val isAuth = (flags and (FLAG_AUTH_HMAC.toInt() and 0xFF)) != 0

            val expectedTotalLen = if (isAuth) {
                6 + 2 + payloadLen + 8 + 2
            } else {
                6 + payloadLen + 2
            }

            if (bytes.size < expectedTotalLen) {
                throw IllegalArgumentException("Incomplete packet: expected $expectedTotalLen, got ${bytes.size}")
            }

            val nodeId = if (isAuth) {
                buffer.short.toInt() and 0xFFFF
            } else {
                0
            }

            val payload = ByteArray(payloadLen)
            buffer.get(payload)

            var isTampered = false
            if (isAuth) {
                val receivedHmac = ByteArray(8)
                buffer.get(receivedHmac)
                val expectedHmac = computeHmac(bytes, 0, 8 + payloadLen, key)
                if (!java.security.MessageDigest.isEqual(receivedHmac, expectedHmac)) {
                    isTampered = true
                }
            }

            val receivedCrc = buffer.short.toInt() and 0xFFFF
            val checkLength = if (isAuth) 8 + payloadLen + 8 else 6 + payloadLen
            val computedCrc = crc16Ccitt(bytes, checkLength)
            if (receivedCrc != computedCrc) {
                throw IllegalArgumentException("CRC mismatch: received 0x%04X, computed 0x%04X".format(receivedCrc, computedCrc))
            }

            if (isTampered) {
                throw SecurityException("Security Alert: Invalid HMAC authentication tag on frame (node: $nodeId, seq: $seqNum). Potential spoofing or tampering!")
            }

            val isEmergency = (flags and (FLAG_ALERT_EMERGENCY.toInt() and 0xFF)) != 0
            val isAlert = (flags and (FLAG_TACTICAL_ALERT.toInt() and 0xFF)) != 0
            val isPtt = (flags and (FLAG_PTT_MODE.toInt() and 0xFF)) != 0
            val langId = flags and (LANG_MASK.toInt() and 0xFF)
            val text = String(payload, StandardCharsets.UTF_8)

            return TantraPacket(
                text = text,
                langId = langId,
                isEmergency = isEmergency,
                isAlert = isAlert,
                isPtt = isPtt,
                seqNum = seqNum,
                nodeId = nodeId
            )
        }
    }

    val langCode: String
        get() = if (langId in LANG_CODES.indices) LANG_CODES[langId] else "hi"

    val langName: String
        get() = if (langId in LANG_NAMES.indices) LANG_NAMES[langId] else "Hindi"

    /**
     * Serializes into a framed binary array with optional HMAC-SHA256 authentication.
     */
    fun encode(enableAuth: Boolean = true, key: ByteArray = DEFAULT_SECRET_KEY): ByteArray {
        val payload = text.toByteArray(StandardCharsets.UTF_8)
        val payloadLen = payload.size

        var flags = (langId and 0x0F)
        if (isEmergency) flags = flags or (FLAG_ALERT_EMERGENCY.toInt() and 0xFF)
        if (isAlert) flags = flags or (FLAG_TACTICAL_ALERT.toInt() and 0xFF)
        if (isPtt) flags = flags or (FLAG_PTT_MODE.toInt() and 0xFF)
        if (enableAuth) flags = flags or (FLAG_AUTH_HMAC.toInt() and 0xFF)

        if (enableAuth) {
            val totalLen = 6 + 2 + payloadLen + 8 + 2
            val buffer = ByteBuffer.allocate(totalLen).order(ByteOrder.BIG_ENDIAN)

            buffer.put(MAGIC_BYTE)
            buffer.put(flags.toByte())
            buffer.putShort(seqNum.toShort())
            buffer.putShort(payloadLen.toShort())
            buffer.putShort(nodeId.toShort())
            buffer.put(payload)

            val hmac = computeHmac(buffer.array(), 0, 8 + payloadLen, key)
            buffer.put(hmac)

            val crc = crc16Ccitt(buffer.array(), 8 + payloadLen + 8)
            buffer.putShort(crc.toShort())

            return buffer.array()
        } else {
            val totalLen = 6 + payloadLen + 2
            val buffer = ByteBuffer.allocate(totalLen).order(ByteOrder.BIG_ENDIAN)

            buffer.put(MAGIC_BYTE)
            buffer.put(flags.toByte())
            buffer.putShort(seqNum.toShort())
            buffer.putShort(payloadLen.toShort())
            buffer.put(payload)

            val crc = crc16Ccitt(buffer.array(), 6 + payloadLen)
            buffer.putShort(crc.toShort())

            return buffer.array()
        }
    }

    /**
     * Calculates efficiency telemetry metrics.
     */
    fun getTelemetry(speechDurationSec: Float = 3.0f): Telemetry {
        val packetBytes = encode().size
        val rawPcmBytes = (speechDurationSec * 16000 * 2).toInt()
        val effectiveBps = (packetBytes * 8f) / maxOf(speechDurationSec, 0.1f)
        val savingsVsPcm = (1f - (packetBytes.toFloat() / rawPcmBytes)) * 100f

        return Telemetry(
            packetBytes = packetBytes,
            effectiveBps = effectiveBps,
            savingsPercent = savingsVsPcm
        )
    }

    data class Telemetry(
        val packetBytes: Int,
        val effectiveBps: Float,
        val savingsPercent: Float
    )
}
