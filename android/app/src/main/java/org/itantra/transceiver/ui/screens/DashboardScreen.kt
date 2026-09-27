package org.itantra.transceiver.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.itantra.transceiver.radio.AirlinkPeer
import java.io.File

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
    val radioId = prefs.getString("radio_id", "ITANTRA-7249") ?: "ITANTRA-7249"
    val hasPhoto = prefs.getBoolean("has_photo", false)
    val photoFile = remember { File(context.filesDir, "profile_photo.jpg") }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. Operator Profile Showcase Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, PrimaryBlue, CircleShape)
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasPhoto && photoFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(photoFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = operatorName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Radio ID: $radioId",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Direct Login / Switch User Button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLogoutOrLogin()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
                        text = "Phone: +91 $operatorPhone",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Status: 100% Offline Airlink",
                        fontSize = 12.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. HERO SHOWCASE: 3-Tier Life Safety Protocol
        Text(
            text = "3-TIER LIFE SAFETY PROTOCOL",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tier 1: Red SOS
                TierProtocolItem(
                    tierBadgeColor = SOSRed,
                    badgeText = "🔴 RED SOS",
                    title = "Life-Safety Emergency Distress",
                    description = "Say 'SOS', 'Help', 'Bachao', or tap the red SOS button. Instantly overrides phone volume to maximum, sounds rescue sirens, and broadcasts an emergency alert to all nearby radios.",
                    voiceTrigger = "Voice: \"SOS\", \"Emergency\", \"Help\", \"बचाओ\""
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGray)

                // Tier 2: Yellow Alert
                TierProtocolItem(
                    tierBadgeColor = Color(0xFFF59E0B),
                    badgeText = "🟡 YELLOW ALERT",
                    title = "Tactical Hazard Warning",
                    description = "Say 'Alert', 'Warning', 'Khatra', or 'Eccarikkai' before speaking. Dispatches a high-priority warning notification and distinct vibration across field units.",
                    voiceTrigger = "Voice: \"Alert\", \"Warning\", \"Khatra\", \"चेतावनी\""
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGray)

                // Tier 3: Green Normal
                TierProtocolItem(
                    tierBadgeColor = AccentGreen,
                    badgeText = "🟢 GREEN NORMAL",
                    title = "Routine Field Walkie-Talkie",
                    description = "Hold the button and speak normally. Audio is converted to text on-device and synthesized cleanly on receiving handsets with zero internet.",
                    voiceTrigger = "Voice: Any regular team communication"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. PRODUCT SHOWCASE CARDS (Modern Consumer App Style)
        Text(
            text = "PRODUCT CAPABILITIES",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Product Card 1: Zero-Internet Walkie Talkie
        ProductShowcaseCard(
            title = "Zero-Internet Push-To-Talk",
            subtitle = "Talk phone-to-phone without cellular towers or internet.",
            description = "Turn on Mobile Hotspot on one phone, connect the other, and communicate peer-to-peer over long distances instantly.",
            icon = Icons.Default.WifiTethering,
            accentColor = PrimaryBlue,
            tags = listOf("100% Offline", "Peer-to-Peer", "Hotspot / Bluetooth")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Product Card 2: Multilingual Speech AI
        ProductShowcaseCard(
            title = "Multilingual Offline Voice AI",
            subtitle = "Natural on-device speech transcription and synthesis.",
            description = "Converts spoken voice into tiny data packets on your device and speaks it out loud naturally in your preferred language.",
            icon = Icons.Default.Language,
            accentColor = Color(0xFF7C3AED),
            tags = listOf("हिंदी Hindi", "English", "தமிழ் Tamil")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. ACTIVE RESCUE RADAR & PEERS
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .scale(radarPulse)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CellTower,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Rescue Airlink Radar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (connectedPeers.isEmpty()) "Scanning for active field radios…" else "${connectedPeers.size} Operator(s) in direct range",
                                fontSize = 12.sp,
                                color = if (connectedPeers.isEmpty()) TextSecondary else AccentGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                        Text("Send Ping", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (connectedPeers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    connectedPeers.forEach { peer ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceGray)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = peer.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Active • Airlink",
                                fontSize = 11.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. QUICK NAVIGATION ACTIONS
        Text(
            text = "QUICK ACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "PTT Walkie-Talkie",
            subtitle = "Push-to-Talk voice radio with live transcription",
            icon = Icons.Default.Mic,
            color = PrimaryBlue,
            onClick = onNavigateToRadio
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "Connect Devices",
            subtitle = "Pair over Wi-Fi Hotspot or Bluetooth RFCOMM",
            icon = Icons.Default.Wifi,
            color = AccentGreen,
            onClick = onNavigateToConnect
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "Personal Info & Photo",
            subtitle = "Edit your profile details, crop photo and view Radio ID",
            icon = Icons.Default.Badge,
            color = WarningAmber,
            onClick = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "About iTantra",
            subtitle = "Smart India Hackathon 2026 • ISRO problem statement",
            icon = Icons.Default.Info,
            color = Color(0xFF7C3AED),
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TierProtocolItem(
    tierBadgeColor: Color,
    badgeText: String,
    title: String,
    description: String,
    voiceTrigger: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tierBadgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = tierBadgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = voiceTrigger,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryBlue
        )
    }
}

@Composable
fun ProductShowcaseCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    tags: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tag badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
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
