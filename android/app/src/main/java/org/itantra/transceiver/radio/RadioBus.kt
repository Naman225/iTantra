package org.itantra.transceiver.radio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.itantra.transceiver.protocol.TantraPacket

/**
 * Unified RadioBus (I-07 & I-09).
 * Merges incoming packets from UDP Broadcast, Bluetooth RFCOMM, and LoRa.
 * Performs duplicate suppression and self-echo filtering based on (nodeId, seqNum).
 */
object RadioBus {
    private const val TAG = "iTantra-RadioBus"

    private val _incomingPackets = MutableSharedFlow<TantraPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<TantraPacket> = _incomingPackets.asSharedFlow()

    // 60-second duplicate suppression cache: Pair(nodeId, seqNum) -> timestamp
    private val seenCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<Pair<Int, Int>, Long>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Int, Int>, Long>?): Boolean {
                return size > 500
            }
        }
    )

    private var localNodeId: Int = 0
    private var udpTransceiver: UdpRadioTransceiver? = null
    private var btTransceiver: BluetoothTransceiver? = null
    private var btServerManager: BluetoothServerManager? = null

    fun setLocalNodeId(nodeId: Int) {
        localNodeId = nodeId
    }

    fun registerTransceivers(
        udp: UdpRadioTransceiver?,
        btClient: BluetoothTransceiver?,
        btServer: BluetoothServerManager?
    ) {
        udpTransceiver = udp
        btTransceiver = btClient
        btServerManager = btServer
    }

    /**
     * Unified multi-transport broadcast (I-07):
     * Transmits outgoing TantraPacket across all active wireless physical layers:
     * 1. UDP Local Hotspot / Mesh Broadcast
     * 2. Bluetooth RFCOMM Client link
     * 3. Bluetooth RFCOMM Server link to all connected peer phones
     */
    fun transmit(packet: TantraPacket) {
        val now = System.currentTimeMillis()
        val key = Pair(packet.nodeId, packet.seqNum)
        synchronized(seenCache) {
            seenCache[key] = now
        }

        // Transmit via UDP
        try {
            udpTransceiver?.transmit(packet)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "UDP broadcast error: ${e.message}")
        }

        // Transmit via Bluetooth RFCOMM client
        try {
            btTransceiver?.transmit(packet)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bluetooth client transmit error: ${e.message}")
        }

        // Transmit via Bluetooth RFCOMM server to connected peers
        try {
            btServerManager?.broadcast(packet)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bluetooth server broadcast error: ${e.message}")
        }
    }

    fun postIncoming(packet: TantraPacket, source: String = "RADIO") {
        val now = System.currentTimeMillis()
        val key = Pair(packet.nodeId, packet.seqNum)

        // Clean up entries older than 60s
        synchronized(seenCache) {
            val it = seenCache.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                if (now - entry.value > 60_000L) {
                    it.remove()
                }
            }

            // Check if already seen
            if (seenCache.containsKey(key)) {
                // Drop duplicate frame
                return
            }
            seenCache[key] = now
        }

        // If it's our own packet transmitted out and looped back, don't replay it to the speech pipeline
        if (packet.nodeId == localNodeId && localNodeId != 0) {
            return
        }

        _incomingPackets.tryEmit(packet)
    }
}
