package org.itantra.transceiver.ui.screens

import android.Manifest
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
import org.itantra.transceiver.engine.SpeechToTextManager
import org.itantra.transceiver.engine.TextToSpeechManager
import org.itantra.transceiver.protocol.TantraPacket
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
    onSendDirectText: (String, Int, Boolean, (TantraPacket) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isPttPressed by remember { mutableStateOf(false) }
    var isEmergencySos by remember { mutableStateOf(false) }
    var selectedLangId by remember { mutableStateOf(0) }
    var directTextInput by remember { mutableStateOf("") }
    var showHowToDialog by remember { mutableStateOf(false) }

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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        radio.incomingPackets.collect { packet ->
            messageLog.add(0, TransmissionItem(packet, isIncoming = true))
            if (packet.isEmergency) {
                alertManager.overrideVolumeToMax()
                alertManager.triggerDistressVibration()
                ttsManager.speak(packet.text, packet.langCode, isEmergency = true)
            } else if (packet.isAlert) {
                alertManager.triggerDistressVibration()
                ttsManager.speak("Warning: " + packet.text, packet.langCode, isEmergency = false)
            } else {
                audioPlayer.playRogerBeep()
                ttsManager.speak(packet.text, packet.langCode, isEmergency = false)
            }
        }
    }

    // Dynamic wave pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPttPressed) (1.08f + audioLevel * 0.18f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Ripple wave ring animation
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPttPressed) 1.28f else 1f,
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
        // Control Bar: Language, SOS, Help Dialog
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
                // 3 Primary Languages: Hindi (0), English (1), Tamil (6)
                val primaryLanguages = listOf(0, 1, 6)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val currIdx = primaryLanguages.indexOf(selectedLangId)
                        val nextIdx = if (currIdx == -1) 0 else (currIdx + 1) % primaryLanguages.size
                        selectedLangId = primaryLanguages[nextIdx]
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

        // Live Speech Recognition Status Banner
        AnimatedVisibility(
            visible = isPttPressed || partialText.isNotBlank() || isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryLight),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = PrimaryBlue,
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
                            text = if (isProcessing) "Processing audio transcription…" else "Live Transcription (${TantraPacket.LANG_NAMES[selectedLangId]})",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            partialText.isNotBlank() -> "\"$partialText\""
                            isProcessing -> "Transcribing speech into data packet…"
                            else -> "Listening… speak now"
                        },
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // PTT Button Area with Expanding Wave Ripple and Tactile Touch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Expanding concentric ripple waves during hold
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
                            modifier = Modifier.widthIn(max = 290.dp),
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
                        title = "🇮🇳 3 Indian Languages",
                        description = "Tap the top language button to switch between Hindi, English, and Tamil."
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
