package org.itantra.transceiver.audio

/**
 * Biquad 300 Hz – 3400 Hz Telephony Bandpass Filter (I-05 / Slide 4).
 * Eliminates low-frequency rumble (wind, engine noise) and high-frequency hiss
 * before passing audio to STT / VAD.
 */
class SpectralFilter(private val sampleRate: Float = 16000.0f) {

    // 2nd-order High-Pass filter at 300 Hz
    private var hpX1 = 0f
    private var hpX2 = 0f
    private var hpY1 = 0f
    private var hpY2 = 0f

    // 2nd-order Low-Pass filter at 3400 Hz
    private var lpX1 = 0f
    private var lpX2 = 0f
    private var lpY1 = 0f
    private var lpY2 = 0f

    /**
     * Filters a single 16-bit PCM sample through the bandpass cascade.
     */
    fun processSample(input: Float): Float {
        // High-pass 300Hz coefficients approximation for 16kHz
        val hpOut = 0.919f * input - 1.838f * hpX1 + 0.919f * hpX2 + 1.832f * hpY1 - 0.844f * hpY2
        hpX2 = hpX1
        hpX1 = input
        hpY2 = hpY1
        hpY1 = hpOut

        // Low-pass 3400Hz coefficients approximation for 16kHz
        val lpOut = 0.245f * hpOut + 0.490f * lpX1 + 0.245f * lpX2 + 0.171f * lpY1 - 0.151f * lpY2
        lpX2 = lpX1
        lpX1 = hpOut
        lpY2 = lpY1
        lpY1 = lpOut

        return lpOut.coerceIn(-32768.0f, 32767.0f)
    }

    /**
     * Filters an entire buffer of 16-bit PCM audio samples.
     */
    fun processBuffer(buffer: ShortArray, size: Int) {
        for (i in 0 until size) {
            val sample = buffer[i].toFloat()
            val filtered = processSample(sample)
            buffer[i] = filtered.toInt().toShort()
        }
    }
}
