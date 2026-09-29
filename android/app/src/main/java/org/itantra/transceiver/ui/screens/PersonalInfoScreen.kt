package org.itantra.transceiver.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream

private const val PREFS_NAME = "itantra_profile"
private const val PHOTO_FILE_NAME = "profile_photo.jpg"

@Composable
fun PersonalInfoScreen(
    onBackClick: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Loaded profile values
    var name by remember { mutableStateOf(prefs.getString("name", "Naman Tiwari") ?: "Naman Tiwari") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "XXXXXXXXXX") ?: "XXXXXXXXXX") }
    var dob by remember { mutableStateOf(prefs.getString("dob", "15/08/2002") ?: "15/08/2002") }
    val radioId = remember { prefs.getString("radio_id", "ITANTRA-7249") ?: "ITANTRA-7249" }
    var hasPhoto by remember { mutableStateOf(prefs.getBoolean("has_photo", false)) }

    // Edit mode toggle
    var isEditing by remember { mutableStateOf(false) }

    // Temporary editing values
    var editName by remember { mutableStateOf(name) }
    var editPhone by remember { mutableStateOf(phone) }
    var editDob by remember { mutableStateOf(dob) }

    val photoFile = remember { File(context.filesDir, PHOTO_FILE_NAME) }
    var photoUri by remember { mutableStateOf<Uri?>(if (photoFile.exists()) Uri.fromFile(photoFile) else null) }
    var cropImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cropImageUri = it
        }
    }

    if (cropImageUri != null) {
        ImageCropDialog(
            sourceUri = cropImageUri!!,
            onCropSuccess = { croppedFile ->
                photoUri = Uri.fromFile(croppedFile)
                hasPhoto = true
                prefs.edit().putBoolean("has_photo", true).apply()
                cropImageUri = null
                Toast.makeText(context, "Profile photo cropped and saved", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                cropImageUri = null
            }
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "Edit Profile" else "Personal Info",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            if (!isEditing) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        editName = name
                        editPhone = phone
                        editDob = dob
                        isEditing = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", color = PrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar Profile Photo
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryBlue, CircleShape)
                    .background(SurfaceGray)
                    .clickable { imagePickerLauncher.launch("image/*") },
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
                        contentDescription = "Default Avatar",
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // Camera upload icon overlay
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Upload Photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AUTO-ASSIGNED RADIO ID BADGE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryLight)
                .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RADIO ID: $radioId",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isEditing) {
            // VIEW MODE
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Active Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACTIVE OPERATOR",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Profile Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OPERATOR DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone Number",
                        value = if (phone.startsWith("+91")) phone else "+91 $phone"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGray)

                    ProfileDetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date of Birth",
                        value = dob
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGray)

                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Assigned Radio ID",
                        value = radioId
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceGray)

                    ProfileDetailRow(
                        icon = Icons.Default.Wifi,
                        label = "Mesh Frequency",
                        value = "UDP 5005 Airlink"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button with Touch Interaction
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "edit_scale")

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editName = name
                    editPhone = phone
                    editDob = dob
                    isEditing = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .scale(scale),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile Details", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logout / Switch User Button to easily access Login Screen
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLogout?.invoke()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SOSRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SOSRed)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = SOSRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out / Switch Account", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SOSRed)
            }
        } else {
            // EDIT MODE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EDIT PROFILE INFORMATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val editCalendar = remember { java.util.Calendar.getInstance() }
                    val editDatePickerDialog = remember {
                        android.app.DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                editDob = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
                            },
                            2002, 6, 15
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editDatePickerDialog.show() }
                    ) {
                        OutlinedTextField(
                            value = editDob,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date of Birth") },
                            placeholder = { Text("Tap to select date from calendar") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue) },
                            trailingIcon = {
                                IconButton(onClick = { editDatePickerDialog.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Open Calendar", tint = PrimaryBlue)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Save and Cancel Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                ) {
                    Text("Cancel", color = TextDark)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        name = editName.trim()
                        phone = editPhone.trim()
                        dob = editDob.trim()

                        prefs.edit()
                            .putString("name", name)
                            .putString("phone", phone)
                            .putString("dob", dob)
                            .apply()

                        isEditing = false
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}
