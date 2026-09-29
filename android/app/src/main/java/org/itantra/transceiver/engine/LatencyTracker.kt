package org.itantra.transceiver.engine

import android.util.Log

/**
 * Latency Instrumentation & Trace Object (I-17).
 * Tracks millisecond timestamps across the speech pipeline:
 * t0: speech start
 * t1: speech end
 * t2: STT final recognized
 * t3: transmit out
 * t4: receive in
 * t5: TTS first audio played
 */
data class LatencyTrace(
    val utteranceId: String,
    var t0SpeechStart: Long = 0L,
    var t1SpeechEnd: Long = 0L,
    var t2SttFinal: Long = 0L,
    var t3Transmit: Long = 0L,
    var t4Receive: Long = 0L,
    var t5TtsFirstAudio: Long = 0L
) {
    val sttDurationMs: Long
        get() = if (t2SttFinal > t1SpeechEnd && t1SpeechEnd > 0) t2SttFinal - t1SpeechEnd else 0L

    val transmitDurationMs: Long
        get() = if (t4Receive > t3Transmit && t3Transmit > 0) t4Receive - t3Transmit else 0L

    val totalTurnaroundMs: Long
        get() = if (t5TtsFirstAudio > t1SpeechEnd && t1SpeechEnd > 0) t5TtsFirstAudio - t1SpeechEnd else 0L

    fun logTrace(tag: String = "iTantra-Latency") {
        Log.i(tag, "TRACE [$utteranceId] -> STT: ${sttDurationMs}ms | Transmit: ${transmitDurationMs}ms | Total Turnaround: ${totalTurnaroundMs}ms")
    }

    fun toCsvLine(): String {
        return "$utteranceId,$t0SpeechStart,$t1SpeechEnd,$t2SttFinal,$t3Transmit,$t4Receive,$t5TtsFirstAudio,$sttDurationMs,$transmitDurationMs,$totalTurnaroundMs"
    }
}

object LatencyTracker {
    private val traceLog = mutableListOf<LatencyTrace>()

    fun recordTrace(trace: LatencyTrace) {
        synchronized(traceLog) {
            traceLog.add(trace)
            if (traceLog.size > 200) traceLog.removeAt(0)
        }
        trace.logTrace()
    }

    fun getAverageTurnaround(): Double {
        synchronized(traceLog) {
            val valid = traceLog.map { it.totalTurnaroundMs }.filter { it in 100..5000 }
            return if (valid.isNotEmpty()) valid.average() else 750.0
        }
    }
}
