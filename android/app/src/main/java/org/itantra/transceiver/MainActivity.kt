package org.itantra.transceiver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.itantra.transceiver.audio.AudioPlayerManager
import org.itantra.transceiver.emergency.EmergencyAlertManager
import org.itantra.transceiver.emergency.NotificationHelper
import org.itantra.transceiver.engine.SpeechToTextManager
import org.itantra.transceiver.engine.TextToSpeechManager
import org.itantra.transceiver.protocol.TantraPacket
import org.itantra.transceiver.radio.BluetoothTransceiver
import org.itantra.transceiver.radio.UdpRadioTransceiver
import org.itantra.transceiver.ui.screens.*

// Navigation pages
enum class NavPage(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CONNECT("Connect Device", Icons.Default.Wifi),
    CONTACT("Contact Us", Icons.Default.Email),
    PROFILE("Personal Info", Icons.Default.Person),
    ABOUT("About", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private lateinit var radioTransceiver: UdpRadioTransceiver
    private lateinit var audioPlayer: AudioPlayerManager
    private lateinit var alertManager: EmergencyAlertManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sttManager: SpeechToTextManager
    private lateinit var btTransceiver: BluetoothTransceiver
    private lateinit var notificationHelper: NotificationHelper

    private var pendingEmergency = false
    private var pendingLangId = 0
    private var pendingOnSent: ((TantraPacket) -> Unit)? = null

    // Emergency keyword detection lists
    private val emergencyKeywordsEn = listOf(
        "alert", "emergency", "sos", "help", "mayday", "danger",
        "evacuate", "flood", "fire", "rescue", "distress"
    )
    private val emergencyKeywordsHi = listOf(
        "आपातकालीन", "संकट", "बचाओ", "मदद", "खतरा", "बाढ़",
        "आग", "तुरंत", "सहायता", "चेतावनी", "संकटकालीन"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("itantra_profile", Context.MODE_PRIVATE)
        val initialName = prefs.getString("name", "Operator") ?: "Operator"

        // Initialize Core Engines
        notificationHelper = NotificationHelper(this)
        radioTransceiver = UdpRadioTransceiver(this).apply {
            echoSelfPackets = true
            currentUserName = initialName
        }
        audioPlayer = AudioPlayerManager()
        alertManager = EmergencyAlertManager(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechToTextManager(this)
        btTransceiver = BluetoothTransceiver()
        btTransceiver.init(this)

        radioTransceiver.startListening()

        // Request runtime permissions
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            ITantraApp(
                radio = radioTransceiver,
                audioPlayer = audioPlayer,
                alertManager = alertManager,
                ttsManager = ttsManager,
                sttManager = sttManager,
                btTransceiver = btTransceiver,
                notificationHelper = notificationHelper,
                onStartPtt = { langId -> startPttRecording(langId) },
                onStopPtt = { isEmergency, selectedLangId, onSent ->
                    stopPttAndTransmit(isEmergency, selectedLangId, onSent)
                },
                onSendDirectText = { text, langId, isEmergency, onSent ->
                    transmitDirectMessage(text, langId, isEmergency, onSent)
                }
            )
        }
    }

    private fun startPttRecording(selectedLangId: Int) {
        sttManager.startListening(selectedLangId) { recognizedText, errorMsg ->
            handleSpeechRecognitionResult(recognizedText, errorMsg)
        }
    }

    private fun stopPttAndTransmit(
        isEmergency: Boolean,
        selectedLangId: Int,
        onSent: (TantraPacket) -> Unit
    ) {
        pendingEmergency = isEmergency
        pendingLangId = selectedLangId
        pendingOnSent = onSent
        sttManager.stopListening()
    }

    private fun handleSpeechRecognitionResult(text: String, errorMsg: String? = null) {
        val onSent = pendingOnSent
        pendingOnSent = null
        var isEmergency = pendingEmergency
        val selectedLangId = pendingLangId

        val trimmedText = text.trim()
        if (trimmedText.isNotBlank()) {
            // Smart Voice Emergency Detection
            val lowerText = trimmedText.lowercase()
            val isAutoEmergency = emergencyKeywordsEn.any { lowerText.contains(it) } ||
                    emergencyKeywordsHi.any { trimmedText.contains(it) }
            if (isAutoEmergency && !isEmergency) {
                isEmergency = true
                runOnUiThread {
                    Toast.makeText(this, "⚠️ Emergency keyword detected — sending as SOS alert!", Toast.LENGTH_SHORT).show()
                }
            }

            val detected = TantraPacket.detectLanguage(trimmedText)
            val langToUse = if (selectedLangId == 0 || selectedLangId == 1) {
                if (detected == 1 || detected == 0) detected else selectedLangId
            } else {
                selectedLangId
            }

            val packet = TantraPacket(
                text = trimmedText,
                langId = langToUse,
                isEmergency = isEmergency,
                isPtt = true,
                seqNum = (1..65534).random()
            )

            radioTransceiver.transmit(packet)
            audioPlayer.playRogerBeep()
            runOnUiThread { onSent?.invoke(packet) }
        } else {
            if (isEmergency) {
                val emergencyText = if (selectedLangId == 1) {
                    "Emergency SOS: Distress signal beacon activated!"
                } else {
                    "आपातकालीन संदेश: संकट संकेत सक्रिय किया गया तुरंत सहायता भेजें!"
                }
                val packet = TantraPacket(
                    text = emergencyText,
                    langId = selectedLangId,
                    isEmergency = true,
                    isPtt = true,
                    seqNum = (1..65534).random()
                )
                radioTransceiver.transmit(packet)
                audioPlayer.playRogerBeep()
                runOnUiThread { onSent?.invoke(packet) }
            } else {
                runOnUiThread {
                    val msg = errorMsg ?: "No voice recognized. Hold the button and speak clearly."
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun transmitDirectMessage(
        text: String,
        langId: Int,
        isEmergency: Boolean,
        onSent: (TantraPacket) -> Unit
    ) {
        if (text.isBlank()) return

        // Smart text emergency detection
        val lowerText = text.lowercase()
        val isAutoEmergency = emergencyKeywordsEn.any { lowerText.contains(it) } ||
                emergencyKeywordsHi.any { text.contains(it) }
        val finalEmergency = isEmergency || isAutoEmergency

        if (isAutoEmergency && !isEmergency) {
            runOnUiThread {
                Toast.makeText(this, "⚠️ Emergency keyword detected — sending as SOS alert!", Toast.LENGTH_SHORT).show()
            }
        }

        val packet = TantraPacket(
            text = text.trim(),
            langId = langId,
            isEmergency = finalEmergency,
            isPtt = false,
            seqNum = (1..65534).random()
        )
        radioTransceiver.transmit(packet)
        audioPlayer.playRogerBeep()
        onSent(packet)
    }

    override fun onDestroy() {
        super.onDestroy()
        radioTransceiver.stop()
        btTransceiver.cleanup(this)
        sttManager.shutdown()
        ttsManager.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ITantraApp(
    radio: UdpRadioTransceiver,
    audioPlayer: AudioPlayerManager,
    alertManager: EmergencyAlertManager,
    ttsManager: TextToSpeechManager,
    sttManager: SpeechToTextManager,
    btTransceiver: BluetoothTransceiver,
    notificationHelper: NotificationHelper,
    onStartPtt: (Int) -> Unit,
    onStopPtt: (Boolean, Int, (TantraPacket) -> Unit) -> Unit,
    onSendDirectText: (String, Int, Boolean, (TantraPacket) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("itantra_profile", Context.MODE_PRIVATE) }
    var isLoggedIn by remember { mutableStateOf(prefs.getBoolean("is_logged_in", false)) }

    // Listen for incoming radio packets and trigger system notifications
    LaunchedEffect(Unit) {
        radio.incomingPackets.collect { packet ->
            notificationHelper.showMessageNotification(
                sender = "${packet.langName} Radio Unit",
                message = packet.text,
                isEmergency = packet.isEmergency
            )
        }
    }

    if (!isLoggedIn) {
        // App starts with 2-Step Login/Onboarding Screen
        LoginScreen(
            onCompleteProfile = { phone, name, dob, radioId ->
                prefs.edit()
                    .putString("phone", phone)
                    .putString("name", name)
                    .putString("dob", dob)
                    .putString("radio_id", radioId)
                    .putBoolean("is_logged_in", true)
                    .apply()
                radio.currentUserName = name
                isLoggedIn = true
            },
            onContinueAsGuest = {
                val guestRadioId = "GUEST-${(1000..9999).random()}"
                prefs.edit()
                    .putString("name", "Guest Operator")
                    .putString("phone", "9205917214")
                    .putString("dob", "01/01/2000")
                    .putString("radio_id", guestRadioId)
                    .putBoolean("is_logged_in", true)
                    .apply()
                radio.currentUserName = "Guest Operator"
                isLoggedIn = true
            }
        )
    } else {
        // Main Application with Navigation Drawer
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentPage by remember { mutableStateOf(NavPage.HOME) }

        // Live state
        val btDevices by btTransceiver.discoveredDevices.collectAsState()
        val isBtScanning by btTransceiver.isScanning.collectAsState()
        val connectedBtDevice by btTransceiver.connectedDeviceName.collectAsState()
        val connectedPeers by radio.connectedPeers.collectAsState()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = Color.White
                ) {
                    // Drawer Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "iTantra",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Smart Voice Transceiver",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val userName = prefs.getString("name", "User") ?: "User"
                                Text(text = userName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(text = "Online • Airlink Ready", color = AccentGreen, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Navigation Items
                    NavPage.entries.forEach { page ->
                        val isSelected = currentPage == page
                        NavigationDrawerItem(
                            icon = { Icon(page.icon, contentDescription = null) },
                            label = { Text(page.title, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            selected = isSelected,
                            onClick = {
                                currentPage = page
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PrimaryLight,
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextDark
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = SurfaceGray)

                    // Log Out / Switch User Option
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = SOSRed) },
                        label = { Text("Log Out / Switch User", color = SOSRed, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = {
                            prefs.edit().putBoolean("is_logged_in", false).apply()
                            isLoggedIn = false
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = SOSRed,
                            unselectedTextColor = SOSRed
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Footer
                    Text(
                        text = "Made with ❤️ in India • SIH 2026",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (currentPage == NavPage.HOME) "iTantra" else currentPage.title,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                fontSize = 20.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextDark)
                            }
                        },
                        actions = {
                            // Connection status dot
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = PrimaryDark
                        )
                    )
                },
                containerColor = LightBackground
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentPage) {
                        NavPage.HOME -> HomeScreen(
                            radio = radio,
                            audioPlayer = audioPlayer,
                            alertManager = alertManager,
                            ttsManager = ttsManager,
                            sttManager = sttManager,
                            onStartPtt = onStartPtt,
                            onStopPtt = onStopPtt,
                            onSendDirectText = onSendDirectText
                        )
                        NavPage.CONNECT -> ConnectDeviceScreen(
                            connectedPeers = connectedPeers,
                            onScanAirlink = {
                                val currentName = prefs.getString("name", "Operator") ?: "Operator"
                                radio.sendBeaconPing(currentName)
                            },
                            onSendTestChime = {
                                val currentName = prefs.getString("name", "Operator") ?: "Operator"
                                radio.sendTestChime(currentName)
                            },
                            btDevices = btDevices,
                            isBtScanning = isBtScanning,
                            connectedBtDevice = connectedBtDevice,
                            onScanBt = { btTransceiver.startDiscovery(context) },
                            onConnectBt = { address -> btTransceiver.connectToDevice(address) }
                        )
                        NavPage.CONTACT -> ContactUsScreen()
                        NavPage.PROFILE -> PersonalInfoScreen()
                        NavPage.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}
