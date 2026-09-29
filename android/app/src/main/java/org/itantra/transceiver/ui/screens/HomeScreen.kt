package org.itantra.transceiver.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.itantra.transceiver.audio.AudioPlayerManager
import org.itantra.transceiver.emergency.EmergencyAlertManager
import org.itantra.transceiver.engine.ModelStore
import org.itantra.transceiver.engine.SpeechToTextManager
import org.itantra.transceiver.engine.TextToSpeechManager
import org.itantra.transceiver.protocol.PhraseCodebook
import org.itantra.transceiver.protocol.TantraPacket
import org.itantra.transceiver.radio.RadioBus
import org.itantra.transceiver.radio.UdpRadioTransceiver

data class TransmissionItem(
    val packet: TantraPacket,
    val isIncoming: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    radio: UdpRadioTransceiver,
    audioPlayer: AudioPlayerManager,
    alertManager: EmergencyAlertManager,
    ttsManager: TextToSpeechManager,
    sttManager: SpeechToTextManager,
    onStartPtt: (Int) -> Unit,
    onStopPtt: (Boolean, Int, (TantraPacket) -> Unit) -> Unit,
    onSendDirectText: (String, Int, Boolean, (TantraPacket) -> Unit) -> Unit,
    sosCountdown: Pair<TantraPacket, Int>? = null,
    onCancelSos: () -> Unit = {},
    onConfirmSos: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember { context.getSharedPreferences("itantra_profile", Context.MODE_PRIVATE) }
    var isPttPressed by remember { mutableStateOf(false) }
    var isEmergencySos by remember { mutableStateOf(false) }
    var selectedLangId by remember { mutableStateOf(prefs.getInt("preferred_lang_id", 0)) }
    var directTextInput by remember { mutableStateOf("") }
    var showHowToDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isPhoneCallMode by remember { mutableStateOf(false) }
    var isCallActive by remember { mutableStateOf(false) }
    var isCallMuted by remember { mutableStateOf(false) }
    var activeSosPacket by remember { mutableStateOf<TantraPacket?>(null) }

    val partialText by sttManager.partialText.collectAsState()
    val audioLevel by sttManager.audioLevel.collectAsState()
    val isProcessing by sttManager.isProcessing.collectAsState()

    val messageLog = remember { mutableStateListOf<TransmissionItem>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required for voice", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        selectedLangId = prefs.getInt("preferred_lang_id", selectedLangId)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        RadioBus.incomingPackets.collect { packet ->
            messageLog.add(0, TransmissionItem(packet, isIncoming = true))

            // Cross-Language Tactical Translation (I-14)
            // If the message matches an operational phrase, translate to receiver's selected language
            val targetLangCode = TantraPacket.LANG_CODES.getOrElse(selectedLangId) { "en" }
            val phraseId = PhraseCodebook.findPhraseId(packet.text)
            val spokenText = if (phraseId != null) {
                PhraseCodebook.getTranslation(phraseId, targetLangCode) ?: packet.text
            } else {
                packet.text
            }

            if (packet.isEmergency) {
                // User requirement: pure heavy continuous vibration ONLY, no alarm voice/sound until read
                activeSosPacket = packet
                alertManager.startContinuousDistressVibration()
            } else if (packet.isAlert) {
                alertManager.triggerDistressVibration()
                ttsManager.speak("Warning: " + spokenText, targetLangCode, isEmergency = false)
            } else {
                audioPlayer.playRogerBeep()
                ttsManager.speak(spokenText, targetLangCode, isEmergency = false)
            }
        }
    }

    // Dynamic wave pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPttPressed || isCallActive) (1.08f + audioLevel * 0.22f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Ripple wave ring animation
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPttPressed || isCallActive) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Control Bar: 10-Language Selector, SOS, Help Dialog
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 10 Indian Languages Picker Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showLanguageDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TantraPacket.LANG_NAMES[selectedLangId],
                        color = PrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                }

                // SOS Emergency Toggle
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isEmergencySos = !isEmergencySos
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmergencySos) SOSRed else CardWhite
                    ),
                    border = BorderStroke(1.dp, if (isEmergencySos) SOSRed else Color(0xFFD1D5DB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isEmergencySos) Color.White else SOSRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SOS",
                        color = if (isEmergencySos) Color.White else SOSRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // How to Interact Help Icon Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHowToDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help Guide",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // High-Priority Tactical Distress Banner (continuous vibration active until acknowledged/read)
        if (activeSosPacket != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        alertManager.stopDistressVibration()
                        activeSosPacket = null
                    },
                colors = CardDefaults.cardColors(containerColor = SOSRed),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚨 SOS DISTRESS RECEIVED",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "\"${activeSosPacket?.text}\"",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Tactical heavy vibration active • Tap to acknowledge",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            alertManager.stopDistressVibration()
                            activeSosPacket = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = SOSRed
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Acknowledge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Operating Mode Toggle Bar: Walkie-Talkie (PTT) vs Phone Call (Hands-Free VAD)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                // Mode 1: Walkie-Talkie (PTT)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isPhoneCallMode) PrimaryBlue else Color.Transparent)
                        .clickable {
                            if (isPhoneCallMode) {
                                if (isCallActive) {
                                    isCallActive = false
                                    sttManager.isPhoneMode = false
                                    sttManager.stopListening()
                                }
                                isPhoneCallMode = false
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (!isPhoneCallMode) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Walkie-Talkie (PTT)",
                            fontWeight = if (!isPhoneCallMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isPhoneCallMode) Color.White else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Mode 2: Phone Call (Hands-Free VAD)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPhoneCallMode) AccentGreen else Color.Transparent)
                        .clickable {
                            if (!isPhoneCallMode) {
                                isPhoneCallMode = true
                                isCallActive = false // Show dialer first, don't call automatically
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (isPhoneCallMode) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Phone Call (VAD)",
                            fontWeight = if (isPhoneCallMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPhoneCallMode) Color.White else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Speech Recognition Status Banner
        AnimatedVisibility(
            visible = isPttPressed || isCallActive || isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isCallActive) Color(0xFFE8F5E9) else PrimaryLight),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, (if (isCallActive) AccentGreen else PrimaryBlue).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = if (isCallActive) AccentGreen else PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCallActive) "Hands-Free Call • VAD Auto-Detecting Speech (${TantraPacket.LANG_NAMES[selectedLangId]})"
                            else if (isProcessing) "Processing audio transcription…"
                            else "Live Transcription (${TantraPacket.LANG_NAMES[selectedLangId]})",
                            color = if (isCallActive) Color(0xFF2E7D32) else PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            partialText.isNotBlank() -> "\"$partialText\""
                            isProcessing -> "Transcribing speech into data packet…"
                            isCallActive -> "Listening continuously… speak naturally without holding button"
                            else -> "Listening… speak now"
                        },
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center Area: PTT Walkie-Talkie OR Hands-Free Phone Call
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (isPhoneCallMode) {
                if (!isCallActive) {
                    // READY TO DIAL STATE: Option to dial first
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Full-Duplex Phone Call",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hands-free voice with Voice Activity Detection (VAD)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Channel / Language Info Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceGray)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Language: ${TantraPacket.LANG_NAMES[selectedLangId]}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Change",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.clickable { showLanguageDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // DIAL BUTTON
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCallActive = true
                                    isCallMuted = false
                                    sttManager.isPhoneMode = true
                                    audioPlayer.playRogerBeep()
                                    onStartPtt(selectedLangId)
                                    Toast.makeText(context, "Call Connected • Speak naturally", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("DIAL / START CALL", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap Dial to begin hands-free conversation. Your voice transmits automatically when you speak.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    // IN-CALL ACTIVE STATE
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .scale(rippleScale)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(195.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.28f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(165.dp)
                                .shadow(16.dp, CircleShape, spotColor = AccentGreen)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AccentGreen, Color(0xFF1B5E20))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isCallMuted) Icons.Default.MicOff else Icons.Default.Call,
                                    contentDescription = "Active Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isCallMuted) "MUTED" else "IN CALL",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isCallMuted) "Mic Paused" else "Hands-Free VAD",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // In-Call Controls: Mute and End Call
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCallMuted = !isCallMuted
                                    if (isCallMuted) {
                                        sttManager.stopListening()
                                        Toast.makeText(context, "Microphone Muted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onStartPtt(selectedLangId)
                                        Toast.makeText(context, "Microphone Active", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isCallMuted) WarningAmber else Color(0xFFD1D5DB)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCallMuted) Color(0xFFFEF3C7) else CardWhite
                                )
                            ) {
                                Icon(
                                    if (isCallMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isCallMuted) Color(0xFFB45309) else TextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCallMuted) "Unmute" else "Mute", color = if (isCallMuted) Color(0xFFB45309) else TextDark, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCallActive = false
                                    sttManager.isPhoneMode = false
                                    sttManager.stopListening()
                                    audioPlayer.playRogerBeep()
                                    Toast.makeText(context, "Call Ended", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SOSRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End Call", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // PTT WALKIE-TALKIE MODE: Hold-to-Talk button with expanding wave ripple
                if (isPttPressed) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(rippleScale)
                            .clip(CircleShape)
                            .background((if (isEmergencySos) SOSRed else PrimaryBlue).copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(195.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background((if (isEmergencySos) SOSRed else PrimaryBlue).copy(alpha = 0.28f))
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(165.dp)
                            .scale(pulseScale)
                            .shadow(
                                elevation = if (isPttPressed) 16.dp else 6.dp,
                                shape = CircleShape,
                                ambientColor = if (isEmergencySos) SOSRed else PrimaryBlue,
                                spotColor = if (isEmergencySos) SOSRed else PrimaryBlue
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (isEmergencySos) {
                                        listOf(SOSRed, Color(0xFFC62828))
                                    } else if (isPttPressed) {
                                        listOf(AccentGreen, Color(0xFF1B5E20))
                                    } else {
                                        listOf(PrimaryBlue, PrimaryDark)
                                    }
                                )
                            )
                            .pointerInteropFilter { motionEvent ->
                                when (motionEvent.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isPttPressed = true
                                        onStartPtt(selectedLangId)
                                        true
                                    }

                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                contentDescription = "Push to Talk",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isPttPressed -> "Transmitting…"
                                    isProcessing -> "Sending…"
                                    else -> "Hold to Talk"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPttPressed) "Release when done speaking" else "Press and hold to record",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Direct Text Input Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = directTextInput,
                    onValueChange = { directTextInput = it },
                    placeholder = { Text("Type message…", color = TextSecondary, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGray,
                        unfocusedContainerColor = SurfaceGray,
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (directTextInput.isNotBlank()) {
                            onSendDirectText(directTextInput, selectedLangId, isEmergencySos) { sentPacket ->
                                messageLog.add(0, TransmissionItem(sentPacket, isIncoming = false))
                            }
                            directTextInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Traffic Log
        if (messageLog.isNotEmpty()) {
            Text(
                text = "Messages (${messageLog.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                items(messageLog) { item ->
                    val isOutgoing = !item.isIncoming
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .clickable {
                                    alertManager.stopDistressVibration()
                                    activeSosPacket = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    item.packet.isEmergency -> Color(0xFFFEE2E2) // Red SOS tint
                                    item.packet.isAlert -> Color(0xFFFEF3C7)     // Yellow Alert tint
                                    isOutgoing -> PrimaryLight
                                    else -> CardWhite
                                }
                            ),
                            border = when {
                                item.packet.isEmergency -> BorderStroke(1.dp, SOSRed)
                                item.packet.isAlert -> BorderStroke(1.dp, Color(0xFFF59E0B))
                                else -> BorderStroke(0.5.dp, Color(0xFFE5E7EB))
                            },
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isOutgoing) 12.dp else 4.dp,
                                bottomEnd = if (isOutgoing) 4.dp else 12.dp
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (item.packet.isEmergency) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = SOSRed, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "🚨 RED SOS DISTRESS",
                                            color = SOSRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                } else if (item.packet.isAlert) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "⚠️ YELLOW TACTICAL ALERT",
                                            color = Color(0xFFB45309),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AccentGreen)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "🟢 ROUTINE COMMS",
                                            color = AccentGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                }

                                Text(
                                    text = item.packet.text,
                                    color = TextDark,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.packet.langName} • ${if (isOutgoing) "Sent" else "Received"}",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Play",
                                        tint = PrimaryBlue,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                ttsManager.speak(
                                                    item.packet.text,
                                                    item.packet.langCode,
                                                    isEmergency = item.packet.isEmergency
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // HOW TO INTERACT QUICK POP-UP DIALOG
    if (showHowToDialog) {
        AlertDialog(
            onDismissRequest = { showHowToDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3-Tier Life Safety Protocol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HowToGuideItem(
                        icon = Icons.Default.Warning,
                        title = "🔴 Red SOS: Life Distress",
                        description = "Say 'SOS', 'Help', 'Bachao', or tap the SOS button. Sounds max-volume siren and pushes emergency alert to all radios."
                    )

                    HowToGuideItem(
                        icon = Icons.Default.WarningAmber,
                        title = "🟡 Yellow Alert: Tactical Warning",
                        description = "Say 'Alert', 'Warning', 'Khatra', or 'Eccarikkai'. Broadcasts high-priority warning notification across the field."
                    )

                    HowToGuideItem(
                        icon = Icons.Default.Radio,
                        title = "🟢 Green Normal: Routine Voice",
                        description = "Hold the button and speak normally. Converts speech to text and transmits 100% offline peer-to-peer."
                    )

                    HowToGuideItem(
                        icon = Icons.Default.Language,
                        title = "🇮🇳 10 Mandated Indian Languages",
                        description = "Tap the language selector to switch between Hindi, English, Tamil, Telugu, Bengali, Marathi, Gujarati, Kannada, Malayalam, and Odia."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHowToDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 10 INDIAN LANGUAGES PICKER MODAL DIALOG
    if (showLanguageDialog) {
        val nativeNames = listOf(
            "हिंदी (Hindi)",
            "English (English)",
            "ગુજરાતી (Gujarati)",
            "मराठी (Marathi)",
            "ಕನ್ನಡ (Kannada)",
            "മലയാളം (Malayalam)",
            "தமிழ் (Tamil)",
            "తెలుగు (Telugu)",
            "ଓଡ଼ିଆ (Odia)",
            "বাংলা (Bengali)"
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Language (10 Mandated)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(10) { idx ->
                        val isSelected = selectedLangId == idx

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.5.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedLangId = idx
                                    prefs.edit().putInt("preferred_lang_id", idx).apply()
                                    sttManager.resetPartialText()
                                    showLanguageDialog = false
                                    Toast.makeText(context, "Switched to ${TantraPacket.LANG_NAMES[idx]} (100% Offline Ready)", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryLight else SurfaceGray
                            ),
                            border = if (isSelected) BorderStroke(1.2.dp, PrimaryBlue) else null,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nativeNames[idx],
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) PrimaryBlue else TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AccentGreen)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Active • 100% Offline Speech & Voice",
                                            fontSize = 10.5.sp,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3-SECOND LIFE SAFETY SOS CANCELLATION MODAL DIALOG (I-06)
    sosCountdown?.let { (pkt, secondsLeft) ->
        AlertDialog(
            onDismissRequest = { /* Modal: Operator must confirm or cancel */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SOSRed, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚨 EMERGENCY SOS TRIGGERED",
                        fontWeight = FontWeight.ExtraBold,
                        color = SOSRed,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Distress voice trigger recognized:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SOSRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${pkt.text}\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SOSRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SOSRed.copy(alpha = 0.15f))
                            .border(2.dp, SOSRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${secondsLeft}s",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SOSRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Broadcasting to tactical network in ${secondsLeft}s unless aborted",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSos,
                    colors = ButtonDefaults.buttonColors(containerColor = SOSRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SEND NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelSos,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                ) {
                    Text("CANCEL SOS", color = TextDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun HowToGuideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
            Text(text = description, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}
