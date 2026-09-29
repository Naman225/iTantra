package org.itantra.transceiver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentAmber = Color(0xFFD97706)
private val AccentAmberBg = Color(0xFFFEF3C7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "About",
                    color = TextDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CardWhite
            )
        )

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Branding Header: Logo Icon, Text, Tagline, Version
            AppBrandingHeader()

            // 2. Feature Badges Row
            FeatureBadgesRow()

            // 3. Description Card
            DescriptionCard()

            // 4. "How It Works" Card with 5 steps
            HowItWorksCard()

            // 5. Footer
            AboutFooter()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * App branding header displaying the logo emblem, large iTantra title,
 * tagline "Smart Voice Transceiver", and version badge.
 */
@Composable
private fun AppBrandingHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Emblem
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = "iTantra Logo",
                tint = CardWhite,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large bold iTantra logo text
        Text(
            text = "iTantra",
            color = PrimaryBlue,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Tagline
        Text(
            text = "Smart Voice Transceiver",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Version Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceGray)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Version: 1.0.0",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Feature badges row displaying "100% Offline" (green), "10 Languages" (blue),
 * and "99.9% Bandwidth Saved" (amber).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeatureBadgesRow() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "100% Offline" (green)
        FeatureBadge(
            icon = Icons.Default.CloudOff,
            label = "100% Offline",
            textColor = AccentGreen,
            backgroundColor = AccentGreen.copy(alpha = 0.12f),
            borderColor = AccentGreen.copy(alpha = 0.35f)
        )

        // "10 Languages" (blue)
        FeatureBadge(
            icon = Icons.Default.Language,
            label = "10 Languages",
            textColor = PrimaryBlue,
            backgroundColor = PrimaryBlue.copy(alpha = 0.12f),
            borderColor = PrimaryBlue.copy(alpha = 0.35f)
        )

        // "99.9% Bandwidth Saved" (amber)
        FeatureBadge(
            icon = Icons.Default.Speed,
            label = "99.9% Bandwidth Saved",
            textColor = AccentAmber,
            backgroundColor = AccentAmberBg,
            borderColor = AccentAmber.copy(alpha = 0.35f)
        )
    }
}

/**
 * Individual pill-shaped feature badge with icon and label.
 */
@Composable
private fun FeatureBadge(
    icon: ImageVector,
    label: String,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Description card presenting the core purpose and technical capabilities of iTantra.
 */
@Composable
private fun DescriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Overview",
                color = PrimaryBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "iTantra is an ultra-low-bitrate smart voice transceiver designed for disaster response and tactical communication across 10 Indian languages. It converts speech to text locally, transmits tiny data packets over constrained radio links, and synthesizes natural voice on the receiver — all 100% offline, no internet required.",
                color = TextDark,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * "How It Works" card with 5 numbered steps detailing the voice-to-packet pipeline.
 */
@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "How It Works",
                    color = TextDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 1
            StepItem(
                stepNumber = 1,
                icon = Icons.Default.Mic,
                title = "Speak into your phone",
                description = "Press and hold the PTT walkie-talkie button to capture audio.",
                isLast = false
            )

            // Step 2
            StepItem(
                stepNumber = 2,
                icon = Icons.Default.RecordVoiceOver,
                title = "Speech is converted to text offline",
                description = "On-device speech recognition transcribes spoken words locally in real time.",
                isLast = false
            )

            // Step 3
            StepItem(
                stepNumber = 3,
                icon = Icons.Default.Speed,
                title = "Text is compressed into a tiny packet (~50 bytes)",
                description = "Compact binary encoding strips audio overhead to achieve 99.9% bandwidth reduction.",
                isLast = false
            )

            // Step 4
            StepItem(
                stepNumber = 4,
                icon = Icons.Default.Wifi,
                title = "Packet is sent via WiFi Hotspot or Bluetooth",
                description = "Direct peer-to-peer ad-hoc transmission over local WiFi or Bluetooth without routers.",
                isLast = false
            )

            // Step 5
            StepItem(
                stepNumber = 5,
                icon = Icons.Default.VolumeUp,
                title = "Receiver's phone speaks the message out loud",
                description = "The target device decodes the packet and synthesizes voice playback using offline TTS.",
                isLast = true
            )
        }
    }
}

/**
 * Individual step item with step number circle, connector line, icon, and title/description.
 */
@Composable
private fun StepItem(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Step Number Badge and vertical connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$stepNumber",
                    color = CardWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(PrimaryBlue.copy(alpha = 0.25f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Step Text and Subtitle
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Footer displaying credits and hackathon affiliation.
 */
@Composable
private fun AboutFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Made in India",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "SIH 2026 • ISRO",
            color = TextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}
