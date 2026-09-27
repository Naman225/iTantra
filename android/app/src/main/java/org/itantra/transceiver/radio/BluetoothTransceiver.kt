package org.itantra.transceiver.radio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.itantra.transceiver.protocol.TantraPacket
import java.io.InputStream
import java.util.UUID

/**
 * Bluetooth Serial (RFCOMM / SPP) Transceiver for the iTantra Neural Tactical Radio.
 * Enables zero-infrastructure, device-to-device binary communication by streaming
 * serialized [TantraPacket] frames over a standard Bluetooth SPP profile.
 */
class BluetoothTransceiver(context: Context? = null) {

    companion object {
        const val TAG = "iTantra-BT"
        const val SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
        val SPP_UUID: UUID = UUID.fromString(SPP_UUID_STRING)
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectedSocket: BluetoothSocket? = null
    private var isRunning: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO)

    val _incomingPackets = MutableSharedFlow<TantraPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<TantraPacket> = _incomingPackets.asSharedFlow()

    val _discoveredDevices = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val discoveredDevices: StateFlow<List<Pair<String, String>>> = _discoveredDevices.asStateFlow()

    val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private var discoveryReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered: Boolean = false
    private var discoveryTimeoutJob: Job? = null
    private var connectionJob: Job? = null

    init {
        context?.let { init(it) }
    }

    /**
     * Obtains the system BluetoothAdapter from BluetoothManager.
     */
    fun init(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
        Log.i(TAG, "BluetoothTransceiver initialized, adapter available: ${bluetoothAdapter != null}")
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<String, String>> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.map { device ->
                (device.name ?: "Paired Device") to device.address
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Starts device discovery and populates discoveredDevices list.
     * Registers a BroadcastReceiver for ACTION_FOUND and automatically cancels after 12 seconds.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(context: Context) {
        if (bluetoothAdapter == null) {
            init(context)
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            Log.w(TAG, "Cannot start discovery: BluetoothAdapter is null")
            return
        }

        if (!adapter.isEnabled) {
            Log.w(TAG, "Cannot start discovery: Bluetooth is disabled")
            return
        }

        // Cancel previous ongoing discovery
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        // Clean up any previously registered receiver
        if (isReceiverRegistered && discoveryReceiver != null) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister previous discovery receiver: ${e.message}")
            }
            isReceiverRegistered = false
            discoveryReceiver = null
        }

        _discoveredDevices.value = getPairedDevices()
        _isScanning.value = true

        val receiver = DiscoveryReceiver()
        discoveryReceiver = receiver

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register discovery receiver: ${e.message}")
            _isScanning.value = false
            return
        }

        val started = adapter.startDiscovery()
        Log.i(TAG, "Bluetooth discovery started: $started")

        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = scope.launch {
            delay(12_000L)
            if (_isScanning.value) {
                Log.d(TAG, "12 seconds elapsed, cancelling Bluetooth discovery")
                try {
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error cancelling discovery on timeout: ${e.message}")
                }
                _isScanning.value = false
            }
        }
    }

    /**
     * BroadcastReceiver for catching newly found Bluetooth devices during discovery.
     */
    private inner class DiscoveryReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    if (device != null) {
                        val name = device.name ?: intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: "Unknown Device"
                        val address = device.address ?: return
                        val currentList = _discoveredDevices.value
                        if (currentList.none { it.second == address }) {
                            _discoveredDevices.value = currentList + Pair(name, address)
                            Log.d(TAG, "Discovered device: $name ($address)")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    Log.d(TAG, "Bluetooth discovery completed by adapter")
                }
            }
        }
    }

    /**
     * Connects to a remote Bluetooth device by MAC address using RFCOMM SPP.
     * On connection success, starts the incoming frame listen loop.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Log.e(TAG, "Cannot connect: BluetoothAdapter is null")
            return
        }

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                disconnect()

                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                    _isScanning.value = false
                }

                val device: BluetoothDevice = adapter.getRemoteDevice(address)
                val deviceName = device.name ?: address
                Log.i(TAG, "Connecting to Bluetooth RFCOMM device: $deviceName ($address)")

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                connectedSocket = socket
                isRunning = true
                _connectedDeviceName.value = deviceName
                Log.i(TAG, "Successfully connected to $deviceName ($address)")

                listenLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed to device $address: ${e.message}", e)
                disconnect()
            }
        }
    }

    /**
     * Reads from connectedSocket's InputStream in a loop.
     * Follows the TantraPacket protocol:
     * - Searches for magic byte 0x54 ('T')
     * - Reads 6-byte header to determine payload length (bytes 4-5)
     * - Reads payloadLen bytes + 2 bytes CRC
     * - Decodes the full assembled byte array and emits to _incomingPackets
     */
    private fun listenLoop() {
        val socket = connectedSocket ?: return
        val inputStream = try {
            socket.inputStream
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get InputStream from socket: ${e.message}")
            disconnect()
            return
        }

        val headerBuffer = ByteArray(6)

        while (isRunning && socket.isConnected) {
            try {
                // Synchronize stream: search for magic byte 0x54 ('T')
                var firstByte = inputStream.read()
                if (firstByte == -1) {
                    Log.i(TAG, "Bluetooth InputStream closed (EOF)")
                    break
                }

                while (firstByte != (TantraPacket.MAGIC_BYTE.toInt() and 0xFF)) {
                    firstByte = inputStream.read()
                    if (firstByte == -1) {
                        Log.i(TAG, "Bluetooth InputStream closed while seeking magic byte")
                        break
                    }
                }
                if (firstByte == -1) break

                headerBuffer[0] = firstByte.toByte()

                // Read remaining 5 bytes of header: [flags: 1B, seqNum: 2B, payloadLen: 2B]
                if (!readFully(inputStream, headerBuffer, 1, 5)) {
                    Log.w(TAG, "Connection closed while reading header frame")
                    break
                }

                // Extract payload length at bytes 4-5 (Big Endian Short)
                val payloadLen = ((headerBuffer[4].toInt() and 0xFF) shl 8) or (headerBuffer[5].toInt() and 0xFF)

                // Read payloadLen bytes + 2 bytes CRC-16
                val remainingLen = payloadLen + 2
                val remainingBuffer = ByteArray(remainingLen)
                if (!readFully(inputStream, remainingBuffer, 0, remainingLen)) {
                    Log.w(TAG, "Connection closed while reading payload and CRC")
                    break
                }

                // Assemble full packet: 6 header bytes + payloadLen + 2 CRC bytes
                val fullPacketBytes = ByteArray(6 + remainingLen)
                System.arraycopy(headerBuffer, 0, fullPacketBytes, 0, 6)
                System.arraycopy(remainingBuffer, 0, fullPacketBytes, 6, remainingLen)

                try {
                    val packet = TantraPacket.decode(fullPacketBytes)
                    Log.d(TAG, "Decoded TantraPacket: seq #${packet.seqNum}, text='${packet.text}', lang=${packet.langName}")
                    if (!_incomingPackets.tryEmit(packet)) {
                        scope.launch {
                            _incomingPackets.emit(packet)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode TantraPacket: ${e.message}")
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error in Bluetooth listen loop: ${e.message}")
                }
                break
            }
        }

        if (isRunning) {
            disconnect()
        }
    }

    /**
     * Reads exactly [length] bytes into [buffer] starting at [offset], blocking until filled.
     * Returns false if EOF is reached before all bytes are read.
     */
    private fun readFully(inputStream: InputStream, buffer: ByteArray, offset: Int, length: Int): Boolean {
        var totalRead = 0
        while (totalRead < length) {
            val bytesRead = inputStream.read(buffer, offset + totalRead, length - totalRead)
            if (bytesRead == -1) return false
            totalRead += bytesRead
        }
        return true
    }

    /**
     * Encodes a TantraPacket and writes the binary data to the connected socket's OutputStream.
     */
    fun transmit(packet: TantraPacket) {
        scope.launch {
            try {
                val socket = connectedSocket
                if (socket == null || !socket.isConnected) {
                    Log.w(TAG, "Cannot transmit: Bluetooth socket is not connected")
                    return@launch
                }

                val encoded = packet.encode()
                val outputStream = socket.outputStream
                synchronized(outputStream) {
                    outputStream.write(encoded)
                    outputStream.flush()
                }
                Log.d(TAG, "Transmitted TantraPacket seq #${packet.seqNum} (${encoded.size} bytes) over Bluetooth")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transmit TantraPacket seq #${packet.seqNum}: ${e.message}", e)
            }
        }
    }

    /**
     * Closes the active BluetoothSocket and resets connection state.
     */
    fun disconnect() {
        isRunning = false
        try {
            connectedSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing connected Bluetooth socket: ${e.message}")
        }
        connectedSocket = null
        _connectedDeviceName.value = null
        Log.i(TAG, "Bluetooth disconnected and state reset")
    }

    /**
     * Unregisters any discovery receiver, cancels discovery, and disconnects the active socket.
     */
    @SuppressLint("MissingPermission")
    fun cleanup(context: Context) {
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = null

        connectionJob?.cancel()
        connectionJob = null

        if (isReceiverRegistered && discoveryReceiver != null) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver during cleanup: ${e.message}")
            }
            isReceiverRegistered = false
            discoveryReceiver = null
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling discovery during cleanup: ${e.message}")
        }
        _isScanning.value = false

        disconnect()
        Log.i(TAG, "BluetoothTransceiver cleanup finished")
    }
}
