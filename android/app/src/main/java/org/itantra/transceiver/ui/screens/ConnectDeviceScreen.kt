package org.itantra.transceiver.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.itantra.transceiver.radio.AirlinkPeer

enum class ConnectionTab {
    WIFI,
    BLUETOOTH
}

@Composable
fun ConnectDeviceScreen(
    connectedPeers: List<AirlinkPeer>,
    onScanAirlink: () -> Unit,
    onSendTestChime: () -> Unit,
    btDevices: List<Pair<String, String>>,
    isBtScanning: Boolean,
    connectedBtDevice: String?,
    onScanBt: () -> Unit,
    onConnectBt: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ConnectionTab.WIFI) }
    val context = LocalContext.current

    val wifiSsid = remember(context) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            val ssid = connectionInfo?.ssid?.replace("\"", "")
            if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                ssid
            } else {
                "Local Wi-Fi / Hotspot Mesh"
            }
        } catch (e: Exception) {
            "Local Wi-Fi / Hotspot Mesh"
        }
    }

    LaunchedEffect(Unit) {
        // Automatically ping airlink on screen open to discover any active peers
        onScanAirlink()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {
        // Connection Mode Toggle (Two Tabs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedTab = ConnectionTab.WIFI },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == ConnectionTab.WIFI) PrimaryBlue else CardWhite,
                    contentColor = if (selectedTab == ConnectionTab.WIFI) Color.White else PrimaryBlue
                ),
                border = BorderStroke(1.dp, if (selectedTab == ConnectionTab.WIFI) PrimaryBlue else Color(0xFFD1D5DB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wi-Fi Hotspot", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = { selectedTab = ConnectionTab.BLUETOOTH },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == ConnectionTab.BLUETOOTH) PrimaryBlue else CardWhite,
                    contentColor = if (selectedTab == ConnectionTab.BLUETOOTH) Color.White else PrimaryBlue
                ),
                border = BorderStroke(1.dp, if (selectedTab == ConnectionTab.BLUETOOTH) PrimaryBlue else Color(0xFFD1D5DB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bluetooth", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == ConnectionTab.WIFI) {
            // Wi-Fi Airlink Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Airlink Active (Port 5005)",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                onScanAirlink()
                                Toast.makeText(context, "Scanning for radios on Wi-Fi...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", color = PrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Network: $wifiSsid",
                        color = TextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Zero-infrastructure peer-to-peer walkie-talkie mode",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Send Test Chime
            OutlinedButton(
                onClick = {
                    onSendTestChime()
                    Toast.makeText(context, "Airlink test chime broadcasted!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                border = BorderStroke(1.dp, PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Broadcast Signal Check (Test Chime)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Discovered Peers Header
            Text(
                text = "Connected Radios on Airlink (${connectedPeers.size})",
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (connectedPeers.isEmpty()) {
                // Helpful Connection Guide Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to link two phones:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("1. Turn on 'Mobile Hotspot' on Phone 1", color = TextDark, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. Connect Phone 2 to Phone 1's Wi-Fi", color = TextDark, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3. Open iTantra on both phones — they will auto-connect and talk without internet!", color = TextDark, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(connectedPeers) { peer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = peer.name, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                                        Text(text = "IP: ${peer.ip}", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AccentGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Bluetooth Tab
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (connectedBtDevice != null) "Connected: $connectedBtDevice" else "Bluetooth Serial Link",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 14.sp
                        )

                        Button(
                            onClick = onScanBt,
                            enabled = !isBtScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isBtScanning) "Scanning…" else "Scan", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "For long-range LoRa hardware dongles or direct phone-to-phone Bluetooth serial link.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Nearby Bluetooth Devices (${btDevices.size})",
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (btDevices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isBtScanning) "Scanning for nearby Bluetooth radios…" else "Tap 'Scan' to discover nearby devices",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(btDevices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(device.first, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 14.sp)
                                    Text(device.second, color = TextSecondary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onConnectBt(device.second) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Connect", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
