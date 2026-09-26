package org.itantra.transceiver.radio

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.itantra.transceiver.protocol.TantraPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * High-speed, zero-infrastructure UDP Radio Transceiver.
 * Uses local Wi-Fi Hotspot broadcast (Port 5005) for instant peer-to-peer walkie-talkie link.
 */
class UdpRadioTransceiver(
    private val context: Context,
    private val port: Int = DEFAULT_PORT
) {
    companion object {
        const val TAG = "iTantra-Radio"
        const val DEFAULT_PORT = 5005
        const val BROADCAST_IP = "255.255.255.255"
    }

    private var socket: DatagramSocket? = null
    private var isRunning = false
    private var listenerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var multicastLock: WifiManager.MulticastLock? = null

    var echoSelfPackets: Boolean = false
    private val localSentSeqNums = java.util.Collections.synchronizedSet(LinkedHashSet<Int>())

    private val _incomingPackets = MutableSharedFlow<TantraPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<TantraPacket> = _incomingPackets.asSharedFlow()

    /**
     * Starts listening for incoming packets on the local radio channel.
     */
    fun startListening() {
        if (isRunning) return
        isRunning = true

        try {
            // Acquire MulticastLock so Android doesn't filter broadcast packets
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("iTantraMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            socket = DatagramSocket(port).apply {
                broadcast = true
                reuseAddress = true
            }

            Log.i(TAG, "Transceiver listening on UDP port $port (Broadcast Mode)")

            listenerJob = scope.launch {
                val buffer = ByteArray(65535)
                while (isRunning) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)

                        val rawData = buffer.copyOf(packet.length)
                        try {
                            val decoded = TantraPacket.decode(rawData)
                            val isSelf = localSentSeqNums.contains(decoded.seqNum)
                            if (isSelf && !echoSelfPackets) {
                                Log.d(TAG, "Filtering self broadcast echo seq #${decoded.seqNum}")
                            } else {
                                Log.d(TAG, "Received packet seq #${decoded.seqNum}: ${decoded.text}")
                                _incomingPackets.emit(decoded)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decode incoming radio packet: ${e.message}")
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Socket receive error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UDP transceiver socket: ${e.message}")
        }
    }

    /**
     * Transmits a packet over the airlink to all listening radios.
     */
    fun transmit(packet: TantraPacket) {
        scope.launch {
            try {
                localSentSeqNums.add(packet.seqNum)
                if (localSentSeqNums.size > 200) {
                    val it = localSentSeqNums.iterator()
                    if (it.hasNext()) {
                        it.next()
                        it.remove()
                    }
                }

                val encodedBytes = packet.encode()
                val broadcastAddr = InetAddress.getByName(BROADCAST_IP)
                val datagram = DatagramPacket(encodedBytes, encodedBytes.size, broadcastAddr, port)

                val txSocket = socket ?: DatagramSocket().apply { broadcast = true }
                txSocket.send(datagram)
                Log.i(TAG, "Transmitted packet seq #${packet.seqNum} (${encodedBytes.size} bytes) to $BROADCAST_IP:$port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transmit packet: ${e.message}")
            }
        }
    }

    /**
     * Stops listening and cleans up locks and sockets.
     */
    fun stop() {
        isRunning = false
        listenerJob?.cancel()
        socket?.close()
        socket = null

        multicastLock?.let {
            if (it.isHeld) it.release()
        }
        multicastLock = null
        Log.i(TAG, "Transceiver stopped.")
    }
}
