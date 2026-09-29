package org.itantra.transceiver.radio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.itantra.transceiver.protocol.TantraPacket
import java.io.InputStream

/**
 * Bluetooth Server Thread (I-07).
 * Listens for incoming RFCOMM SPP connections so two phones can pair directly app-to-app.
 */
class BluetoothServerManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onPacketReceived: (TantraPacket) -> Unit
) {
    companion object {
        const val TAG = "iTantra-BTServer"
        const val SERVICE_NAME = "iTantraVoiceTransceiver"
    }

    private val connectedClients = java.util.Collections.synchronizedList(mutableListOf<BluetoothSocket>())
    private var serverSocket: BluetoothServerSocket? = null
    private var isRunning = false
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun broadcast(packet: TantraPacket) {
        scope.launch {
            val encoded = packet.encode()
            synchronized(connectedClients) {
                val it = connectedClients.iterator()
                while (it.hasNext()) {
                    val client = it.next()
                    try {
                        if (client.isConnected) {
                            val os = client.outputStream
                            synchronized(os) {
                                os.write(encoded)
                                os.flush()
                            }
                        } else {
                            it.remove()
                        }
                    } catch (e: Exception) {
                        it.remove()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRunning || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        isRunning = true

        serverJob = scope.launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME,
                    BluetoothTransceiver.SPP_UUID
                )
                Log.i(TAG, "RFCOMM server socket listening on SPP UUID")

                while (isRunning) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    }

                    if (socket != null) {
                        Log.i(TAG, "Accepted incoming Bluetooth client: ${socket.remoteDevice?.name}")
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BT Server error: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: BluetoothSocket) {
        connectedClients.add(socket)
        scope.launch {
            try {
                val inputStream = socket.inputStream
                val headerBuffer = ByteArray(6)

                while (isRunning && socket.isConnected) {
                    var totalHeaderRead = 0
                    while (totalHeaderRead < 6) {
                        val read = inputStream.read(headerBuffer, totalHeaderRead, 6 - totalHeaderRead)
                        if (read == -1) break
                        totalHeaderRead += read
                    }
                    if (totalHeaderRead < 6) break

                    if (headerBuffer[0] != TantraPacket.MAGIC_BYTE) continue

                    val payloadLen = ((headerBuffer[4].toInt() and 0xFF) shl 8) or (headerBuffer[5].toInt() and 0xFF)
                    val remainingLen = payloadLen + 2
                    val remainingBuffer = ByteArray(remainingLen)

                    var totalRemRead = 0
                    while (totalRemRead < remainingLen) {
                        val read = inputStream.read(remainingBuffer, totalRemRead, remainingLen - totalRemRead)
                        if (read == -1) break
                        totalRemRead += read
                    }
                    if (totalRemRead < remainingLen) break

                    val fullPacket = ByteArray(6 + remainingLen)
                    System.arraycopy(headerBuffer, 0, fullPacket, 0, 6)
                    System.arraycopy(remainingBuffer, 0, fullPacket, 6, remainingLen)

                    try {
                        val packet = TantraPacket.decode(fullPacket)
                        onPacketReceived(packet)
                    } catch (e: Exception) {
                        Log.w(TAG, "Server decode error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Client socket disconnected: ${e.message}")
            } finally {
                connectedClients.remove(socket)
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    fun stop() {
        isRunning = false
        synchronized(connectedClients) {
            connectedClients.forEach { try { it.close() } catch (e: Exception) {} }
            connectedClients.clear()
        }
        try { serverSocket?.close() } catch (e: Exception) {}
        serverJob?.cancel()
    }

    fun stopListening() {
        stop()
    }
}
