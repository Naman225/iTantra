package org.itantra.transceiver.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.itantra.transceiver.radio.AirlinkPeer

@Composable
fun DashboardScreen(
    connectedPeers: List<AirlinkPeer>,
    onSendPing: () -> Unit,
    onNavigateToRadio: () -> Unit,
    onNavigateToConnect: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogoutOrLogin: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("itantra_profile", Context.MODE_PRIVATE) }

    val operatorName = prefs.getString("name", "Naman Tiwari") ?: "Naman Tiwari"
    val operatorPhone = prefs.getString("phone", "9205917214") ?: "9205917214"
    val operatorDob = prefs.getString("dob", "15/08/2002") ?: "15/08/2002"
    val radioId = prefs.getString("radio_id", "ITANTRA-7249") ?: "ITANTRA-7249"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. Operator Info & Login/Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = operatorName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Radio ID: $radioId",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Direct Login / Switch Account Button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLogoutOrLogin()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Login",
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SurfaceGray)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Phone: $operatorPhone",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "DOB: $operatorDob",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Real Benchmark Telemetry Grid (From BENCHMARK_REPORT.md)
        Text(
            text = "TACTICAL TELEMETRY & BENCHMARKS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BenchmarkMetricCard(
                modifier = Modifier.weight(1f),
                title = "Bandwidth Saved",
                metric = "99.92%",
                subtitle = "vs 64 kbps Audio",
                accentColor = AccentGreen,
                icon = Icons.Default.Compress
            )
            BenchmarkMetricCard(
                modifier = Modifier.weight(1f),
                title = "Neural TTS",
                metric = "0.049 RTF",
                subtitle = "20.2x Real-Time",
                accentColor = PrimaryBlue,
                icon = Icons.Default.RecordVoiceOver
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BenchmarkMetricCard(
                modifier = Modifier.weight(1f),
                title = "Offline STT",
                metric = "0.163 RTF",
                subtitle = "6.1x Real-Time",
                accentColor = WarningAmber,
                icon = Icons.Default.Mic
            )
            BenchmarkMetricCard(
                modifier = Modifier.weight(1f),
                title = "LoRa Airtime",
                metric = "42 ms",
                subtitle = "865.2 MHz (15 km)",
                accentColor = Color(0xFF7C3AED),
                icon = Icons.Default.CellTower
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Subsystem Health Indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SUBSYSTEM HEALTH STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                SubsystemStatusRow("Airlink UDP Mesh", "PORT 5005 • ACTIVE", AccentGreen)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceGray)
                SubsystemStatusRow("Bluetooth SPP Bridge", "RFCOMM • READY", PrimaryBlue)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceGray)
                SubsystemStatusRow("Vosk Kaldi Engine", "100% OFFLINE ASR", AccentGreen)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceGray)
                SubsystemStatusRow("Piper VITS Neural TTS", "ON-DEVICE SYNTHESIS", AccentGreen)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SurfaceGray)
                SubsystemStatusRow("Emergency SOS Gate", "LEVEL 1 PREEMPTION", SOSRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Connected Network Peers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIVE MESH PEERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (connectedPeers.isEmpty()) "0 Peers Detected" else "${connectedPeers.size} Device(s) in Range",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (connectedPeers.isEmpty()) TextSecondary else AccentGreen
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSendPing()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Ping", fontSize = 12.sp)
                    }
                }

                if (connectedPeers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    connectedPeers.forEach { peer ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${peer.name} (${peer.ip})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Quick Feature Launcher Navigation Cards
        Text(
            text = "FEATURE NAVIGATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "PTT Walkie-Talkie Transceiver",
            subtitle = "Push-to-Talk voice, live STT transcription & SOS",
            icon = Icons.Default.Mic,
            color = PrimaryBlue,
            onClick = onNavigateToRadio
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "Connect Devices & Hardware",
            subtitle = "Wi-Fi Hotspot pairing, Bluetooth scan & SX1262 LoRa",
            icon = Icons.Default.Wifi,
            color = AccentGreen,
            onClick = onNavigateToConnect
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "Personal Info & Date of Birth",
            subtitle = "Update profile details, DOB calendar & Radio ID",
            icon = Icons.Default.Badge,
            color = WarningAmber,
            onClick = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "About iTantra & Architecture",
            subtitle = "How it works, ISRO problem statement & tech specs",
            icon = Icons.Default.Info,
            color = Color(0xFF7C3AED),
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BenchmarkMetricCard(
    modifier: Modifier,
    title: String,
    metric: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = metric,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun SubsystemStatusRow(name: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }
    }
}

@Composable
fun FeatureLauncherTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
