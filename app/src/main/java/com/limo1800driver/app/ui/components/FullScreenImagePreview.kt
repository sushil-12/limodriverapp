package com.limo1800driver.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * A reusable full-screen image preview component with zoom and pan support.
 * 
 * Supports both Bitmap and URL image sources.
 * 
 * @param isVisible Whether the preview dialog is visible
 * @param onDismiss Callback when the preview is dismissed
 * @param imageBitmap Optional Bitmap image to display
 * @param imageUrl Optional URL string for remote image
 * @param contentDescription Optional content description for accessibility
 */
@Composable
fun FullScreenImagePreview(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    imageBitmap: Bitmap? = null,
    imageUrl: String? = null,
    contentDescription: String? = "Full screen image preview"
) {
    if (!isVisible) return

    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Reset zoom and pan when dialog is shown
    LaunchedEffect(isVisible) {
        if (isVisible) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Transformable state for pinch zoom
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        
        if (newScale > 1f) {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Image container with zoom and pan (composed first)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                // Double tap to zoom in/out
                                if (scale > 1f) {
                                    // Reset zoom
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    // Zoom to 2x at tap location
                                    val newScale = 2f
                                    val imageCenterX = size.width / 2f
                                    val imageCenterY = size.height / 2f
                                    val tapX = tapOffset.x - imageCenterX
                                    val tapY = tapOffset.y - imageCenterY

                                    scale = newScale
                                    offsetX = -tapX * (newScale - 1f)
                                    offsetY = -tapY * (newScale - 1f)
                                }
                            },
                            onTap = {
                                // Optional: Dismiss on single tap (e.g., on background)
                                onDismiss()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .transformable(state = transformableState)
                ) {
                    // Display image
                    when {
                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = contentDescription,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        imageUrl != null -> {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = contentDescription,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Close button - composed last to be on top
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
