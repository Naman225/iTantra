package org.itantra.transceiver.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
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
import org.itantra.transceiver.protocol.TantraPacket
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
    onNavigateToContact: () -> Unit,
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

    // Preferred language drives the default SOS voice/text language app-wide.
    var preferredLangId by remember { mutableIntStateOf(prefs.getInt("preferred_lang_id", 0)) }
    var langMenuExpanded by remember { mutableStateOf(false) }

    // Which safety tier card is expanded (progressive disclosure instead of a wall of text)
    var expandedTier by remember { mutableIntStateOf(-1) }

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
        // 1. COMPACT STATUS STRIP - identity + connectivity + language in one glanceable card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = operatorName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1
                        )
                        Text(
                            text = "ID $radioId  -  +91 $operatorPhone",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLogoutOrLogin()
                        }
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Login / Switch user",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Connectivity chip - plain language, no jargon
                    StatusPill(
                        icon = Icons.Default.CloudOff,
                        label = "Offline & ready",
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )

                    // Language picker - always visible, one tap to change
                    Box(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            icon = Icons.Default.Language,
                            label = TantraPacket.LANG_NAMES[preferredLangId],
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { langMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false }
                        ) {
                            TantraPacket.LANG_NAMES.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        preferredLangId = index
                                        prefs.edit().putInt("preferred_lang_id", index).apply()
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. ALWAYS-VISIBLE, ONE-TAP SOS - the single most important control on this screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToRadio()
                },
            colors = CardDefaults.cardColors(containerColor = SOSRed),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Sos, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Send Emergency SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Also reachable from any screen using the red button, bottom-right",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. SAFETY PROTOCOL - collapsed by default, tap a tier to see its voice commands
        Text(
            text = "SAFETY PROTOCOL - TAP TO EXPAND",
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
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                TierProtocolRow(
                    index = 0,
                    expandedTier = expandedTier,
                    onToggle = { expandedTier = if (expandedTier == 0) -1 else 0 },
                    tierBadgeColor = SOSRed,
                    badgeText = "RED",
                    title = "Emergency Distress",
                    description = "Say \"SOS\", \"Help\", or \"Bachao\", or tap the red SOS button anywhere in the app. Instantly maxes out your volume, sounds a siren, and broadcasts your alert to every nearby radio.",
                    voiceTrigger = "Voice: \"SOS\" - \"Help\" - \"\u092c\u091a\u093e\u0913\""
                )
                HorizontalDivider(color = SurfaceGray)
                TierProtocolRow(
                    index = 1,
                    expandedTier = expandedTier,
                    onToggle = { expandedTier = if (expandedTier == 1) -1 else 1 },
                    tierBadgeColor = WarningAmber,
                    badgeText = "YELLOW",
                    title = "Hazard Warning",
                    description = "Say \"Alert\", \"Warning\", or \"Khatra\" before speaking. Sends a high-priority warning with a distinct vibration to your team - one step below a full SOS.",
                    voiceTrigger = "Voice: \"Alert\" - \"Warning\" - \"\u0916\u0924\u0930\u093e\""
                )
                HorizontalDivider(color = SurfaceGray)
                TierProtocolRow(
                    index = 2,
                    expandedTier = expandedTier,
                    onToggle = { expandedTier = if (expandedTier == 2) -1 else 2 },
                    tierBadgeColor = AccentGreen,
                    badgeText = "GREEN",
                    title = "Regular Talk",
                    description = "Hold the mic button and speak normally, like a walkie-talkie. Your voice is turned into text on your phone and spoken aloud on the other end - no internet needed.",
                    voiceTrigger = "Any normal team message"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. RESCUE RADAR & PEERS
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nearby Radios",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (connectedPeers.isEmpty())
                                    "Looking for other iTantra phones nearby..."
                                else
                                    "${connectedPeers.size} operator(s) in range - ready to talk",
                                fontSize = 12.sp,
                                color = if (connectedPeers.isEmpty()) TextSecondary else AccentGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    var isScanning by remember { mutableStateOf(false) }

                    if (isScanning) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)
                            isScanning = false
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isScanning = true
                            onSendPing()
                            Toast.makeText(context, "📡 Scanning airlink for nearby iTantra radios...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 72.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isScanning) "Scanning" else "Scan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (connectedPeers.isEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No phones found yet? Turn on mobile hotspot on one phone and connect the other from \"Connect\" below.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                } else {
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
                                text = "Active",
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

        // 5. WHAT THIS APP DOES - condensed into one card instead of two heavy showcase cards
        Text(
            text = "HOW IT WORKS",
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
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                CapabilityRow(
                    icon = Icons.Default.WifiTethering,
                    accentColor = PrimaryBlue,
                    title = "Talk without internet or towers",
                    subtitle = "Hotspot or Bluetooth, phone-to-phone"
                )
                Spacer(modifier = Modifier.height(12.dp))
                CapabilityRow(
                    icon = Icons.Default.RecordVoiceOver,
                    accentColor = Color(0xFF7C3AED),
                    title = "Speaks your language back to you",
                    subtitle = "10 Indian languages, works even if you can't read or write"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. MORE - only actions not already reachable from the bottom navigation bar
        Text(
            text = "MORE",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "About iTantra",
            subtitle = "What this app does and why",
            icon = Icons.Default.Info,
            color = Color(0xFF7C3AED),
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(8.dp))

        FeatureLauncherTile(
            title = "Contact & Support",
            subtitle = "Get help or send feedback",
            icon = Icons.Default.Email,
            color = WarningAmber,
            onClick = onNavigateToContact
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatusPill(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
        if (onClick != null) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TierProtocolRow(
    index: Int,
    expandedTier: Int,
    onToggle: () -> Unit,
    tierBadgeColor: Color,
    badgeText: String,
    title: String,
    description: String,
    voiceTrigger: String
) {
    val isExpanded = expandedTier == index
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tierBadgeColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = badgeText,
                color = tierBadgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (isExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
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
}

@Composable
fun CapabilityRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
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
