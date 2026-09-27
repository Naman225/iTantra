package org.itantra.transceiver.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun ImageCropDialog(
    sourceUri: Uri,
    onCropSuccess: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(sourceUri) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)
                
                // Scale down if image is huge (> 2048px)
                var sampleSize = 1
                while (options.outWidth / sampleSize > 2048 || options.outHeight / sampleSize > 2048) {
                    sampleSize *= 2
                }

                context.contentResolver.openInputStream(sourceUri)?.use { secondStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    originalBitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not load image: ${e.message}", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "Crop Profile Photo",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val bitmap = originalBitmap
                            if (bitmap != null) {
                                try {
                                    val croppedFile = cropAndSave(context, bitmap, scale, offset)
                                    onCropSuccess(croppedFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF34A853))
                    }
                }

                Text(
                    text = "Drag to reposition, pinch or use slider to zoom",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Viewfinder Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offset += dragAmount
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = originalBitmap
                    if (bitmap != null) {
                        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

                        // Display movable image
                        androidx.compose.foundation.Image(
                            bitmap = imageBitmap,
                            contentDescription = "To crop",
                            modifier = Modifier
                                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                                .size(
                                    width = (bitmap.width * scale * 0.4f).dp,
                                    height = (bitmap.height * scale * 0.4f).dp
                                )
                        )

                        // Circular Overlay Viewfinder
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension * 0.38f
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)

                            val overlayPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addOval(
                                    Rect(
                                        centerOffset.x - circleRadius,
                                        centerOffset.y - circleRadius,
                                        centerOffset.x + circleRadius,
                                        centerOffset.y + circleRadius
                                    )
                                )
                            }

                            // Dark translucent background outside circular cutout
                            drawPath(overlayPath, color = Color(0xCC000000))

                            // Viewfinder circle border
                            drawCircle(
                                color = Color.White,
                                radius = circleRadius,
                                center = centerOffset,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.8f..3.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF1A6FC4),
                            inactiveTrackColor = Color(0xFF444444)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%.1fx".format(scale),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val bitmap = originalBitmap
                            if (bitmap != null) {
                                try {
                                    val croppedFile = cropAndSave(context, bitmap, scale, offset)
                                    onCropSuccess(croppedFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Crop failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6FC4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Crop & Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Crops square from center of bitmap taking user zoom & pan into account,
 * saves to context.filesDir/profile_photo.jpg and returns the File.
 */
private fun cropAndSave(
    context: Context,
    src: Bitmap,
    scale: Float,
    offset: Offset
): File {
    val srcWidth = src.width
    val srcHeight = src.height
    val minDim = minOf(srcWidth, srcHeight)

    // Calculate crop size based on scale
    val cropSize = (minDim / scale.coerceAtLeast(0.5f)).roundToInt().coerceIn(64, minDim)

    // Offset normalized
    val offsetXInPx = (-offset.x * (srcWidth / 300f)).roundToInt()
    val offsetYInPx = (-offset.y * (srcHeight / 300f)).roundToInt()

    var startX = ((srcWidth - cropSize) / 2) + offsetXInPx
    var startY = ((srcHeight - cropSize) / 2) + offsetYInPx

    startX = startX.coerceIn(0, (srcWidth - cropSize).coerceAtLeast(0))
    startY = startY.coerceIn(0, (srcHeight - cropSize).coerceAtLeast(0))

    val cropped = Bitmap.createBitmap(src, startX, startY, cropSize, cropSize)
    val scaledFinal = Bitmap.createScaledBitmap(cropped, 384, 384, true)

    val targetFile = File(context.filesDir, "profile_photo.jpg")
    FileOutputStream(targetFile).use { out ->
        scaledFinal.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    return targetFile
}
