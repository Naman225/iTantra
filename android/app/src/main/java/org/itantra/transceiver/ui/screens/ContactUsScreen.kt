package org.itantra.transceiver.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Top App Bar / Title Header
        TopAppBar(
            title = {
                Text(
                    text = "Contact Us",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Header Card
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
                        text = "Get in Touch",
                        color = PrimaryBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Have questions, feedback, or need technical support regarding the iTantra Smart Voice Transceiver? Reach out using any of the methods below.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Section 1: Contact Methods
            Text(
                text = "CONTACT METHODS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Email Card
            ContactActionCard(
                icon = Icons.Default.Mail,
                iconTint = PrimaryBlue,
                iconBackground = PrimaryBlue.copy(alpha = 0.12f),
                label = "Email Support",
                value = "namantiwari2384@gmail.com",
                hint = "Tap to compose an email",
                onClick = {
                    openEmail(context, "namantiwari2384@gmail.com")
                }
            )

            // Phone Card
            ContactActionCard(
                icon = Icons.Default.Phone,
                iconTint = AccentGreen,
                iconBackground = AccentGreen.copy(alpha = 0.12f),
                label = "Phone Number",
                value = "XXXXXXXXXX",
                hint = "Tap to call or dial",
                onClick = {
                    openPhone(context, "XXXXXXXXXX")
                }
            )

            // GitHub Card
            ContactActionCard(
                icon = Icons.Default.Code,
                iconTint = PrimaryBlue,
                iconBackground = PrimaryBlue.copy(alpha = 0.12f),
                label = "GitHub Repository",
                value = "https://github.com/Naman225/iTantra",
                hint = "Tap to view open-source repository",
                trailingIcon = Icons.Default.OpenInNew,
                onClick = {
                    openUrl(context, "https://github.com/Naman225/iTantra")
                }
            )

            // Section 2: Project Details
            Text(
                text = "PROJECT & CREDITS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            // Made By Card
            ProjectInfoCard(
                icon = Icons.Default.Person,
                iconTint = PrimaryBlue,
                iconBackground = PrimaryBlue.copy(alpha = 0.12f),
                title = "Made by",
                value = "Naman Tiwari",
                description = "Lead Developer & System Architect"
            )

            // Project Card
            ProjectInfoCard(
                icon = Icons.Default.Code,
                iconTint = AccentGreen,
                iconBackground = AccentGreen.copy(alpha = 0.12f),
                title = "Project",
                value = "Smart India Hackathon 2026 — iTantra",
                description = "Ultra-low-bitrate smart voice transceiver for offline disaster & tactical communication"
            )

            // Organization Card
            ProjectInfoCard(
                icon = Icons.Default.Business,
                iconTint = SOSRed,
                iconBackground = SOSRed.copy(alpha = 0.12f),
                title = "Organization",
                value = "ISRO (Indian Space Research Organisation)",
                description = "Supporting Agency & Problem Statement Host"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Clickable card for launching email, phone dialer, or web URLs.
 */
@Composable
private fun ContactActionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    label: String,
    value: String,
    hint: String,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hint,
                    color = PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Informational display card with subtle elevation and iconography.
 */
@Composable
private fun ProjectInfoCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    value: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$email"))
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "No email application found", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openPhone(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No dialer application found", Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No web browser found to open link", Toast.LENGTH_SHORT).show()
    }
}
