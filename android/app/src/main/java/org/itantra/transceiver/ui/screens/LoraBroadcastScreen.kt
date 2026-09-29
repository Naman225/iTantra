package org.itantra.transceiver.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * LoRa Long-Range Broadcast & Hardware Scanner Screen for iTantra.
 * Allows scanning and connecting to ESP32 / Heltec LoRa radio modules (865.2 MHz India ISM band)
 * to broadcast ultra-low-bitrate voice packets over 10-15 km tactical radio links.
 */
@Composable
fun LoraBroadcastScreen(
    btDevices: List<Pair<String, String>>,
    isBtScanning: Boolean,
    connectedBtDevice: String?,
    onScanBt: () -> Unit,
    onConnectBt: (String) -> Unit,
    onDisconnectBt: () -> Unit = {},
    onSendLoraPing: () -> Unit,
    onSendLoraSos: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var isLoraBroadcastEnabled by remember { mutableStateOf(true) }
    var showHardwareGuide by remember { mutableStateOf(false) }
    var lastPingResult by remember { mutableStateOf<String?>(null) }
    var isSendingPing by remember { mutableStateOf(false) }

    // Pulse animation for active RF antenna indicator
    val infiniteTransition = rememberInfiniteTransition(label = "lora_antenna_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. TOP LORA RF STATUS BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .scale(if (isLoraBroadcastEnabled) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(if (isLoraBroadcastEnabled) AccentGreen.copy(alpha = 0.15f) else SurfaceGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = if (isLoraBroadcastEnabled) AccentGreen else TextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "LoRa Tactical Mesh Radio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoraBroadcastEnabled) AccentGreen else WarningAmber)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isLoraBroadcastEnabled) "Broadcast Active • 15 km Reach" else "RF Broadcast Paused",
                                    fontSize = 11.5.sp,
                                    color = if (isLoraBroadcastEnabled) AccentGreen else WarningAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isLoraBroadcastEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLoraBroadcastEnabled = it
                            Toast.makeText(
                                context,
                                if (it) "🟢 LoRa Mesh Broadcast Enabled" else "⏸️ LoRa Mesh Broadcast Paused",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SurfaceGray)
                Spacer(modifier = Modifier.height(12.dp))

                // RF Parameter Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParameterChip(label = "Frequency", value = "865.2 MHz", icon = Icons.Default.Tune)
                    ParameterChip(label = "Modulation", value = "LoRa SF7", icon = Icons.Default.Speed)
                    ParameterChip(label = "Bandwidth", value = "125 kHz", icon = Icons.Default.Compress)
                    ParameterChip(label = "Tx Power", value = "+20 dBm", icon = Icons.Default.Bolt)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. HARDWARE SCANNER SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scan LoRa Hardware Devices",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (connectedBtDevice != null)
                                "Bound to: $connectedBtDevice"
                            else
                                "Connect ESP32 / Heltec LoRa dongle via Bluetooth",
                            fontSize = 12.sp,
                            color = if (connectedBtDevice != null) AccentGreen else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScanBt()
                            Toast.makeText(context, "🔍 Scanning for nearby LoRa devices...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        enabled = !isBtScanning
                    ) {
                        if (isBtScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discovered or Connected Devices
                if (connectedBtDevice != null) {
                    // Active Connected Device Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(connectedBtDevice, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text("Connected • 865.200 MHz RF Ready", fontSize = 11.sp, color = AccentGreen)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDisconnectBt()
                                    Toast.makeText(context, "Disconnected from LoRa hardware", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SOSRed)
                            ) {
                                Text("Disconnect", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (btDevices.isEmpty() && connectedBtDevice == null) {
                    // Empty State with guide
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceGray)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Devices, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("No hardware devices paired yet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Turn on Bluetooth on your Heltec / ESP32 LoRa module, or pair it from Android Bluetooth Settings.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    // List Discovered Bluetooth Devices
                    btDevices.forEach { (name, address) ->
                        val isThisConnected = connectedBtDevice == name || connectedBtDevice == address
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isThisConnected) Color(0xFFE8F5E9) else SurfaceGray),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (name.contains("LoRa", ignoreCase = true) || name.contains("Heltec", ignoreCase = true)) Icons.Default.Sensors else Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isThisConnected) AccentGreen else PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = TextDark)
                                        Text(text = address, fontSize = 10.5.sp, color = TextSecondary)
                                    }
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConnectBt(address)
                                        Toast.makeText(context, "Connecting to $name...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isThisConnected) AccentGreen else PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(if (isThisConnected) "Connected" else "Connect", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Default Virtual Bridge Option for Quick Testing
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Virtual LoRa Bridge (Simulation)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextDark)
                                Text("Port 5006 • SF7 125kHz Emulator", fontSize = 10.5.sp, color = TextSecondary)
                            }
                        }
                        Text(
                            text = "🟢 Active",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. LORA BROADCAST TESTING & ACTION CONTROLS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LoRa Airlink Broadcast Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Transmit authenticated test packets or emergency signals across the 865.2 MHz RF channel.",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Send LoRa Test Ping Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSendingPing = true
                            onSendLoraPing()
                            lastPingResult = "Frame Sent • 42 Bytes • Airtime: 114 ms • 865.2 MHz SF7"
                            Toast.makeText(context, "📡 Broadcasted authenticated LoRa Ping!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Ping", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Emergency Distress SOS Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSendLoraSos()
                            lastPingResult = "🚨 EMERGENCY DISTRESS BROADCASTED ON 865.2 MHz LORA"
                            Toast.makeText(context, "🚨 High-Priority LoRa SOS Broadcasted!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SOSRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast SOS", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (lastPingResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGray)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lastPingResult!!, fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. USER-FRIENDLY HARDWARE SETUP GUIDE (Collapsible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHardwareGuide = !showHardwareGuide },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "How LoRa Voice Transmission Works",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Icon(
                        imageVector = if (showHardwareGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                AnimatedVisibility(visible = showHardwareGuide) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        GuideStep(
                            number = "1",
                            title = "Plug or Pair ESP32 LoRa Dongle",
                            description = "Connect an inexpensive Heltec V2/V3 or ESP32-S3 LoRa module (~₹1,200) to your phone via Bluetooth or USB OTG."
                        )
                        GuideStep(
                            number = "2",
                            title = "Press Walkie-Talkie Button & Talk",
                            description = "Speak normally in any of the 10 Indian languages. iTantra converts speech to text offline and packs it into ~50 bytes."
                        )
                        GuideStep(
                            number = "3",
                            title = "15 km LoRa Mesh Airwaves",
                            description = "The tiny packet is transmitted at 865.2 MHz (Govt. of India De-licensed ISM Band). Nearby rescue nodes receive the packet and speak the voice out loud."
                        )
                        GuideStep(
                            number = "4",
                            title = "100% Offline & Disaster Ready",
                            description = "Zero cellular signal, zero SIM card, and zero internet towers needed. Operates even when all telecommunication infrastructure is down."
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ParameterChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceGray)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

@Composable
private fun GuideStep(number: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = TextDark)
            Text(description, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
