package org.itantra.transceiver.radio

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.itantra.transceiver.protocol.TantraPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class AirlinkPeer(
    val name: String,
    val ip: String,
    val lastSeenMs: Long = System.currentTimeMillis()
)

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
    var currentUserName: String = "Operator"
    private val localSentSeqNums = java.util.Collections.synchronizedSet(LinkedHashSet<Int>())

    private val _incomingPackets = MutableSharedFlow<TantraPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<TantraPacket> = _incomingPackets.asSharedFlow()

    private val _connectedPeers = MutableStateFlow<List<AirlinkPeer>>(emptyList())
    val connectedPeers: StateFlow<List<AirlinkPeer>> = _connectedPeers.asStateFlow()

    /**
     * Starts listening for incoming packets on the local radio channel.
     */
    fun startListening() {
        if (isRunning) return
        isRunning = true

        try {
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
                        val datagram = DatagramPacket(buffer, buffer.size)
                        socket?.receive(datagram)

                        val senderIp = datagram.address?.hostAddress ?: "Unknown"
                        val rawData = buffer.copyOf(datagram.length)

                        try {
                            val decoded = TantraPacket.decode(rawData)
                            val isSelf = localSentSeqNums.contains(decoded.seqNum)

                            if (isSelf && !echoSelfPackets) {
                                // Ignore self loopback
                            } else {
                                // Peer discovery beacon handling
                                if (decoded.text.startsWith("BEACON_PING|")) {
                                    val peerName = decoded.text.removePrefix("BEACON_PING|").trim()
                                    if (!isSelf) {
                                        updatePeer(peerName, senderIp)
                                        sendBeaconPong(currentUserName)
                                    }
                                } else if (decoded.text.startsWith("BEACON_PONG|")) {
                                    val peerName = decoded.text.removePrefix("BEACON_PONG|").trim()
                                    if (!isSelf) {
                                        updatePeer(peerName, senderIp)
                                    }
                                } else {
                                    // Normal voice or message packet
                                    if (!isSelf) {
                                        updatePeer("Radio Unit ($senderIp)", senderIp)
                                    }
                                    Log.d(TAG, "Received packet seq #${decoded.seqNum} from $senderIp: ${decoded.text}")
                                    _incomingPackets.emit(decoded)
                                }
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

    private fun updatePeer(name: String, ip: String) {
        val currentList = _connectedPeers.value.toMutableList()
        val index = currentList.indexOfFirst { it.ip == ip }
        val peer = AirlinkPeer(name = name.ifBlank { "Radio ($ip)" }, ip = ip)
        if (index >= 0) {
            currentList[index] = peer
        } else {
            currentList.add(0, peer)
        }
        _connectedPeers.value = currentList
    }

    /**
     * Broadcasts discovery beacon to all other iTantra radios on this Wi-Fi / Hotspot.
     */
    fun sendBeaconPing(userName: String) {
        currentUserName = userName
        val packet = TantraPacket(
            text = "BEACON_PING|$userName",
            langId = 1,
            isEmergency = false,
            isPtt = false,
            seqNum = (1..65534).random()
        )
        transmit(packet)
    }

    private fun sendBeaconPong(userName: String) {
        val packet = TantraPacket(
            text = "BEACON_PONG|$userName",
            langId = 1,
            isEmergency = false,
            isPtt = false,
            seqNum = (1..65534).random()
        )
        transmit(packet)
    }

    /**
     * Sends a direct audio chime ping to verify audio link on all connected devices.
     */
    fun sendTestChime(userName: String) {
        val packet = TantraPacket(
            text = "RADIO SIGNAL CHECK: Airlink verified loud and clear from $userName",
            langId = 1,
            isEmergency = false,
            isPtt = false,
            seqNum = (1..65534).random()
        )
        transmit(packet)
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
