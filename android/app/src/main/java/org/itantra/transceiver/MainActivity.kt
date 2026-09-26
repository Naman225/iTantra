package org.itantra.transceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.itantra.transceiver.audio.AudioPlayerManager
import org.itantra.transceiver.emergency.EmergencyAlertManager
import org.itantra.transceiver.engine.SpeechToTextManager
import org.itantra.transceiver.engine.TextToSpeechManager
import org.itantra.transceiver.protocol.TantraPacket
import org.itantra.transceiver.radio.UdpRadioTransceiver

class MainActivity : ComponentActivity() {

    private lateinit var radioTransceiver: UdpRadioTransceiver
    private lateinit var audioPlayer: AudioPlayerManager
    private lateinit var alertManager: EmergencyAlertManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var sttManager: SpeechToTextManager

    private var pendingEmergency = false
    private var pendingLangId = 0
    private var pendingOnSent: ((TantraPacket) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Core Engines
        radioTransceiver = UdpRadioTransceiver(this).apply {
            echoSelfPackets = true // Single-device demo mode: enabled by default so user can hear TTS feedback
        }
        audioPlayer = AudioPlayerManager()
        alertManager = EmergencyAlertManager(this)
        ttsManager = TextToSpeechManager(this)
        sttManager = SpeechToTextManager(this)

        radioTransceiver.startListening()

        setContent {
            TacticalTransceiverScreen(
                radio = radioTransceiver,
                audioPlayer = audioPlayer,
                alertManager = alertManager,
                ttsManager = ttsManager,
                sttManager = sttManager,
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
        sttManager.startListening(selectedLangId) { recognizedText ->
            handleSpeechRecognitionResult(recognizedText)
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

    private fun handleSpeechRecognitionResult(text: String) {
        val onSent = pendingOnSent
        pendingOnSent = null
        val isEmergency = pendingEmergency
        val selectedLangId = pendingLangId

        val trimmedText = text.trim()
        if (trimmedText.isNotBlank()) {
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
            runOnUiThread {
                onSent?.invoke(packet)
            }
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
                runOnUiThread {
                    onSent?.invoke(packet)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "No voice recognized. Hold PTT button and speak clearly.",
                        Toast.LENGTH_SHORT
                    ).show()
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
        val packet = TantraPacket(
            text = text.trim(),
            langId = langId,
            isEmergency = isEmergency,
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
        sttManager.shutdown()
        ttsManager.shutdown()
    }
}

// Tactical Military Color Scheme
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF151B26)
val TacticalAmber = Color(0xFFFFB000)
val TacticalGreen = Color(0xFF00E676)
val TacticalRed = Color(0xFFFF1744)
val TacticalCyan = Color(0xFF00E5FF)
val TextMuted = Color(0xFF8B949E)

data class TransmissionItem(
    val packet: TantraPacket,
    val isIncoming: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TacticalTransceiverScreen(
    radio: UdpRadioTransceiver,
    audioPlayer: AudioPlayerManager,
    alertManager: EmergencyAlertManager,
    ttsManager: TextToSpeechManager,
    sttManager: SpeechToTextManager,
    onStartPtt: (Int) -> Unit,
    onStopPtt: (Boolean, Int, (TantraPacket) -> Unit) -> Unit,
    onSendDirectText: (String, Int, Boolean, (TantraPacket) -> Unit) -> Unit
) {
    val context = LocalContext.current

    var isPttPressed by remember { mutableStateOf(false) }
    var isEmergencySos by remember { mutableStateOf(false) }
    var selectedLangId by remember { mutableStateOf(0) } // 0 = Hindi, 1 = English
    var echoSelfMode by remember { mutableStateOf(radio.echoSelfPackets) }
    var directTextInput by remember { mutableStateOf("") }
    var showDirectInput by remember { mutableStateOf(false) }

    val partialText by sttManager.partialText.collectAsState()
    val audioLevel by sttManager.audioLevel.collectAsState()

    val messageLog = remember { mutableStateListOf<TransmissionItem>() }

    // Request Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required for voice transceiver", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Listen for incoming radio packets
        radio.incomingPackets.collect { packet ->
            messageLog.add(0, TransmissionItem(packet, isIncoming = true))

            if (packet.isEmergency) {
                alertManager.overrideVolumeToMax()
                alertManager.triggerDistressVibration()
                ttsManager.speak(packet.text, packet.langCode, isEmergency = true)
            } else {
                audioPlayer.playRogerBeep()
                ttsManager.speak(packet.text, packet.langCode, isEmergency = false)
            }
        }
    }

    // PTT Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPttPressed) (1.05f + audioLevel * 0.2f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // 1. Header Bar with Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "iTANTRA TRANSCEIVER",
                    color = TacticalAmber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Ch 1 (433.500 MHz / UDP 5005)",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Radio Link Badge / Echo Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .clickable {
                        echoSelfMode = !echoSelfMode
                        radio.echoSelfPackets = echoSelfMode
                        Toast.makeText(
                            context,
                            if (echoSelfMode) "Echo Playback: ON (Single-Device Demo)" else "Echo Playback: OFF (Radio Field Link)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isEmergencySos) TacticalRed else TacticalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (echoSelfMode) "DEMO ECHO ON" else "FIELD LINK",
                    color = if (echoSelfMode) TacticalCyan else TacticalGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Telemetry HUD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BITRATE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("~160 bps", color = TacticalCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COMPRESSION", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("99.9%", color = TacticalGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ASR ENGINE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(TantraPacket.LANG_NAMES[selectedLangId].uppercase(), color = TacticalAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Language & SOS Control Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector button
            Button(
                onClick = { selectedLangId = (selectedLangId + 1) % 2 },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = TacticalAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LANG: ${TantraPacket.LANG_NAMES[selectedLangId].uppercase()}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Keyboard direct input toggle
            IconButton(
                onClick = { showDirectInput = !showDirectInput },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = "Keyboard input", tint = if (showDirectInput) TacticalCyan else TextMuted, modifier = Modifier.size(18.dp))
            }

            // Emergency SOS Button Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isEmergencySos) TacticalRed.copy(alpha = 0.25f) else DarkSurface)
                    .border(1.dp, if (isEmergencySos) TacticalRed else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { isEmergencySos = !isEmergencySos }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = if (isEmergencySos) TacticalRed else TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOS DISTRESS",
                    color = if (isEmergencySos) TacticalRed else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Direct Text Input Drawer (Optional text dispatch)
        AnimatedVisibility(visible = showDirectInput) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = directTextInput,
                        onValueChange = { directTextInput = it },
                        placeholder = { Text("Type tactical text or emergency message...", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (directTextInput.isNotBlank()) {
                                onSendDirectText(directTextInput, selectedLangId, isEmergencySos) { sentPacket ->
                                    messageLog.add(0, TransmissionItem(sentPacket, isIncoming = false))
                                }
                                directTextInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Speech Recognition Status Banner
        AnimatedVisibility(
            visible = isPttPressed || partialText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13222E)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TacticalGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SPEECH TRANSCRIPTION (${TantraPacket.LANG_NAMES[selectedLangId].uppercase()})",
                            color = TacticalCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (partialText.isNotBlank()) "\"$partialText\"" else "Listening... speak now into microphone",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 4. Large Circular PTT Walkie-Talkie Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isEmergencySos) TacticalRed
                        else if (isPttPressed) TacticalGreen
                        else DarkSurface
                    )
                    .border(
                        3.dp,
                        if (isPttPressed) Color.White else TacticalAmber,
                        CircleShape
                    )
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                isPttPressed = true
                                onStartPtt(selectedLangId)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isPttPressed = false
                                onStopPtt(isEmergencySos, selectedLangId) { sentPacket ->
                                    messageLog.add(0, TransmissionItem(sentPacket, isIncoming = false))
                                }
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPttPressed) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "PTT",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPttPressed) "TRANSMITTING..." else "HOLD TO TALK",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isPttPressed) "LIVE ASR ON" else "RELEASE TO SEND",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 5. Message History Log
        Text(
            text = "TRANSCEIVER TRAFFIC LOG",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            items(messageLog) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.packet.isEmergency) TacticalRed.copy(alpha = 0.15f) else DarkSurface
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (item.packet.isEmergency) TacticalRed else if (item.isIncoming) TacticalCyan else TacticalGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (item.packet.isEmergency) "[EMERGENCY ALERT]" else if (item.isIncoming) "[RX RECEIVED]" else "[TX SENT]",
                                    color = if (item.packet.isEmergency) TacticalRed else if (item.isIncoming) TacticalCyan else TacticalAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${item.packet.langName} • ${item.packet.encode().size}B",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = item.packet.text,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                        IconButton(
                            onClick = {
                                ttsManager.speak(
                                    item.packet.text,
                                    item.packet.langCode,
                                    isEmergency = item.packet.isEmergency
                                )
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play", tint = TacticalCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
