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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.itantra.transceiver.audio.AudioPlayerManager
import org.itantra.transceiver.audio.AudioRecordManager
import org.itantra.transceiver.emergency.EmergencyAlertManager
import org.itantra.transceiver.engine.TextToSpeechManager
import org.itantra.transceiver.protocol.TantraPacket
import org.itantra.transceiver.radio.UdpRadioTransceiver
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var radioTransceiver: UdpRadioTransceiver
    private lateinit var audioPlayer: AudioPlayerManager
    private lateinit var alertManager: EmergencyAlertManager
    private lateinit var ttsManager: TextToSpeechManager
    private var audioRecorder: AudioRecordManager? = null

    private val audioBuffer = ByteArrayOutputStream()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Core Engines
        radioTransceiver = UdpRadioTransceiver(this)
        audioPlayer = AudioPlayerManager()
        alertManager = EmergencyAlertManager(this)
        ttsManager = TextToSpeechManager(this)

        audioRecorder = AudioRecordManager(sampleRate = 16000) { pcmChunk, _ ->
            synchronized(audioBuffer) {
                audioBuffer.write(pcmChunk)
            }
        }

        radioTransceiver.startListening()

        setContent {
            TacticalTransceiverScreen(
                radio = radioTransceiver,
                audioPlayer = audioPlayer,
                alertManager = alertManager,
                ttsManager = ttsManager,
                onStartPtt = { startPttRecording() },
                onStopPtt = { isEmergency, selectedLangId, onSent ->
                    stopPttAndTransmit(isEmergency, selectedLangId, onSent)
                }
            )
        }
    }

    private fun startPttRecording() {
        synchronized(audioBuffer) {
            audioBuffer.reset()
        }
        audioRecorder?.startRecording()
    }

    private fun stopPttAndTransmit(
        isEmergency: Boolean,
        selectedLangId: Int,
        onSent: (TantraPacket) -> Unit
    ) {
        audioRecorder?.stopRecording()
        audioPlayer.playRogerBeep()

        val capturedBytes: ByteArray
        synchronized(audioBuffer) {
            capturedBytes = audioBuffer.toByteArray()
            audioBuffer.reset()
        }

        // Generate transcript or test message
        val defaultText = if (selectedLangId == 1) {
            if (isEmergency) "Emergency SOS: Immediate evacuation requested near northern bridge"
            else "Patrol unit calling base. Radio check signal loud and clear."
        } else {
            if (isEmergency) "आपातकालीन संदेश: बाढ़ का पानी बढ़ रहा है तुरंत सहायता भेजें"
            else "सभी दलों को सूचित किया जाता है कि मार्ग सुरक्षित है"
        }

        val packet = TantraPacket(
            text = defaultText,
            langId = selectedLangId,
            isEmergency = isEmergency,
            isPtt = true,
            seqNum = (System.currentTimeMillis() % 1000).toInt()
        )

        radioTransceiver.transmit(packet)
        onSent(packet)
    }

    override fun onDestroy() {
        super.onDestroy()
        radioTransceiver.stop()
        audioRecorder?.stopRecording()
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
    onStartPtt: () -> Unit,
    onStopPtt: (Boolean, Int, (TantraPacket) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPttPressed by remember { mutableStateOf(false) }
    var isEmergencySos by remember { mutableStateOf(false) }
    var isPhoneMode by remember { mutableStateOf(false) } // PTT vs Phone Mode
    var selectedLangId by remember { mutableStateOf(0) } // 0 = Hindi, 1 = English

    val messageLog = remember { mutableStateListOf<TransmissionItem>() }

    // Request Audio & Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required for walkie-talkie", Toast.LENGTH_SHORT).show()
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
        targetValue = if (isPttPressed) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
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

            // Radio Link Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
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
                    text = if (isEmergencySos) "SOS ACTIVE" else "AIRLINK OK",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Telemetry HUD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BITRATE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("~160 bps", color = TacticalCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BANDWIDTH SAVED", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("99.9%", color = TacticalGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MODE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(if (isPhoneMode) "PHONE (VAD)" else "WALKIE (PTT)", color = TacticalAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Mode & Emergency Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language selector button
            Button(
                onClick = { selectedLangId = (selectedLangId + 1) % 2 },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = TacticalAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = TantraPacket.LANG_NAMES[selectedLangId].uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Emergency SOS Button Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isEmergencySos) TacticalRed.copy(alpha = 0.2f) else DarkSurface)
                    .border(1.dp, if (isEmergencySos) TacticalRed else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { isEmergencySos = !isEmergencySos }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = if (isEmergencySos) TacticalRed else TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOS DISTRESS",
                    color = if (isEmergencySos) TacticalRed else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Large PTT Walkie-Talkie Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
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
                                onStartPtt()
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
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPttPressed) "TRANSMITTING" else "HOLD TO TALK",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
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
                .height(140.dp)
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
                                    text = if (item.packet.isEmergency) "[EMERGENCY ALERT]" else "[RADIO MSG]",
                                    color = if (item.packet.isEmergency) TacticalRed else TacticalAmber,
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun TacticalTransceiverPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("iTANTRA TRANSCEIVER", color = TacticalAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Ch 1 (433.500 MHz / UDP 5005)", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("AIRLINK OK", color = TacticalGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Telemetry
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BITRATE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("~160 bps", color = TacticalCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BANDWIDTH SAVED", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("99.9%", color = TacticalGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MODE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("WALKIE (PTT)", color = TacticalAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PTT Circle
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(3.dp, TacticalAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MicNone, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("HOLD TO TALK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Traffic Log
            Text("TRANSCEIVER TRAFFIC LOG", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallReceived, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("[RADIO MSG] Hindi • 52B", color = TacticalAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("बाढ़ का पानी पुल तक आ गया है तुरंत सहायता भेजें", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
