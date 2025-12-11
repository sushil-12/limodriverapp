package com.limo1800driver.app.ui.components.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Keep Enums exactly as they were
enum class DocumentType {
    DRIVING_LICENSE,
    BUSINESS_CARD,
    VEHICLE_INSURANCE,
    CERTIFICATION
}

enum class DocumentSide {
    FRONT,
    BACK
}

@Composable
fun DocumentCameraScreen(
    documentType: DocumentType,
    side: DocumentSide,
    onImageCaptured: (Bitmap?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // Visual State for capture feedback
    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showFlash) 0.8f else 0f,
        animationSpec = tween(150),
        label = "flash"
    )

    // Used to return the overlay coordinates to the logic
    var overlayFrame by remember { mutableStateOf<android.graphics.Rect?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    // --- Logic Section (Unchanged) ---
    suspend fun initializeCamera() {
        cameraProvider = getCameraProvider(context)
        cameraProvider?.let { provider ->
            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentGranted = checkCameraPermission(context)
        hasPermission = currentGranted
        if (currentGranted) {
            initializeCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) initializeCamera()
    }

    // --- UI Section (Redesigned) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera Preview Layer
        if (hasPermission && cameraProvider != null) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewReady = { previewView ->
                    cameraProvider?.let { provider ->
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }

        // 2. Scanner Overlay (The dark scrim + cutout + corner brackets)
        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            onFrameCalculated = { overlayFrame = it }
        )

        // 3. UI Controls Layer (Top and Bottom bars with Gradients for readability)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // -- TOP SECTION --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = documentType.getTitle(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (side == DocumentSide.FRONT) "Front Side" else "Back Side",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // -- BOTTOM SECTION --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(bottom = 50.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Instruction Text
                Text(
                    text = documentType.getInstruction(side),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )

                // Modern Shutter Button
                CameraShutterButton(
                    onClick = {
                        // Flash effect trigger
                        showFlash = true
                        
                        imageCapture?.let { capture ->
                            scope.launch {
                                captureImage(
                                    imageCapture = capture,
                                    overlayFrame = overlayFrame,
                                    context = context,
                                    onImageCaptured = { bitmap ->
                                        onImageCaptured(bitmap)
                                        // Reset flash state quickly after capture starts processing
                                        showFlash = false 
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }

        // 4. Flash Effect Layer (White screen overlay)
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha)
                    .background(Color.White)
            )
            // Auto reset flash state
            LaunchedEffect(showFlash) {
                if (showFlash) {
                    kotlinx.coroutines.delay(100)
                    showFlash = false
                }
            }
        }
    }
}

// --- Custom UI Components ---

@Composable
private fun CameraShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate size when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f, 
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        // Inner Circle
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier,
    onFrameCalculated: (android.graphics.Rect) -> Unit
) {
    Canvas(modifier = modifier) {
        val screenWidth = size.width
        val screenHeight = size.height
        
        // Use the original target sizing logic
        val targetWidth = 320.dp.toPx()
        val targetHeight = 200.dp.toPx()

        val scale = minOf(
            screenWidth * 0.85f / targetWidth, // Increased width usage slightly
            screenHeight * 0.6f / targetHeight
        )
        val overlayWidth = targetWidth * scale
        val overlayHeight = targetHeight * scale
        val overlayX = (screenWidth - overlayWidth) / 2
        val overlayY = (screenHeight - overlayHeight) / 2

        val overlayRect = androidx.compose.ui.geometry.Rect(
            offset = Offset(overlayX, overlayY),
            size = Size(overlayWidth, overlayHeight)
        )

        // 1. Draw the darkened background with a "hole"
        val path = Path().apply {
            // Full screen rect
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            // Subtract the rounded rect
            val cutout = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = overlayRect,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
            }
            op(this, cutout, PathOperation.Difference)
        }
        
        drawPath(path = path, color = Color.Black.copy(alpha = 0.6f))

        // 2. Draw Corner Brackets (Visual Guide)
        val strokeWidth = 3.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val cornerColor = Color.White

        drawContext.canvas.withSave {
            // Top Left
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.left - strokeWidth/2, overlayRect.top),
                end = Offset(overlayRect.left + cornerLength, overlayRect.top),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.left, overlayRect.top),
                end = Offset(overlayRect.left, overlayRect.top + cornerLength),
                strokeWidth = strokeWidth
            )

            // Top Right
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.right + strokeWidth/2, overlayRect.top),
                end = Offset(overlayRect.right - cornerLength, overlayRect.top),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.right, overlayRect.top),
                end = Offset(overlayRect.right, overlayRect.top + cornerLength),
                strokeWidth = strokeWidth
            )

            // Bottom Left
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.left - strokeWidth/2, overlayRect.bottom),
                end = Offset(overlayRect.left + cornerLength, overlayRect.bottom),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.left, overlayRect.bottom),
                end = Offset(overlayRect.left, overlayRect.bottom - cornerLength),
                strokeWidth = strokeWidth
            )

            // Bottom Right
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.right + strokeWidth/2, overlayRect.bottom),
                end = Offset(overlayRect.right - cornerLength, overlayRect.bottom),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(overlayRect.right, overlayRect.bottom),
                end = Offset(overlayRect.right, overlayRect.bottom - cornerLength),
                strokeWidth = strokeWidth
            )
        }

        // Report frame
        onFrameCalculated(
            android.graphics.Rect(
                overlayX.toInt(),
                overlayY.toInt(),
                (overlayX + overlayWidth).toInt(),
                (overlayY + overlayHeight).toInt()
            )
        )
    }
}

// --- Helpers & Logic (Unchanged but included for completeness) ---

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewReady: (PreviewView) -> Unit
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).also { previewView ->
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                onPreviewReady(previewView)
            }
        },
        modifier = modifier
    )
}

private suspend fun captureImage(
    imageCapture: ImageCapture,
    overlayFrame: android.graphics.Rect?,
    context: Context,
    onImageCaptured: (Bitmap) -> Unit
) = suspendCoroutine<Unit> { continuation ->
    val photoFile = File.createTempFile(
        "capture",
        ".jpg",
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    )

    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val file = output.savedUri?.path?.let { File(it) } ?: photoFile
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                val croppedBitmap = if (overlayFrame != null && bitmap != null) {
                    cropBitmapToOverlay(bitmap, overlayFrame)
                } else {
                    bitmap
                }

                croppedBitmap?.let { onImageCaptured(it) }
                continuation.resume(Unit)
            }
            
            override fun onError(exception: ImageCaptureException) {
                continuation.resume(Unit)
            }
        }
    )
}

private fun cropBitmapToOverlay(
    bitmap: Bitmap,
    overlayFrame: android.graphics.Rect
): Bitmap? {
    // Note: This logic assumes the preview fills the screen center. 
    // In a real production app, coordinate mapping between PreviewView and Bitmap is more complex.
    // Keeping logic as per original request.
    
    val overlayWidth = overlayFrame.width().coerceAtLeast(1)
    val overlayHeight = overlayFrame.height().coerceAtLeast(1)

    val scaleX = bitmap.width.toFloat() / overlayWidth
    val scaleY = bitmap.height.toFloat() / overlayHeight
    
    val cropX = (overlayFrame.left * scaleX).toInt().coerceIn(0, bitmap.width)
    val cropY = (overlayFrame.top * scaleY).toInt().coerceIn(0, bitmap.height)
    val cropWidth = (overlayWidth * scaleX).toInt().coerceIn(0, bitmap.width - cropX)
    val cropHeight = (overlayHeight * scaleY).toInt().coerceIn(0, bitmap.height - cropY)

    if (cropWidth <= 0 || cropHeight <= 0) return bitmap
    
    return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
}

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(context)
            )
        }
    }

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun DocumentType.getTitle(): String {
    return when (this) {
        DocumentType.DRIVING_LICENSE -> "Driving License"
        DocumentType.BUSINESS_CARD -> "Business Card"
        DocumentType.VEHICLE_INSURANCE -> "Vehicle Insurance"
        DocumentType.CERTIFICATION -> "Certification"
    }
}

private fun DocumentType.getInstruction(side: DocumentSide): String {
    return when (this) {
        DocumentType.DRIVING_LICENSE -> {
            if (side == DocumentSide.FRONT) "Position front of license in frame"
            else "Position back of license in frame"
        }
        DocumentType.BUSINESS_CARD -> {
            if (side == DocumentSide.FRONT) "Position front of card in frame"
            else "Position back of card in frame"
        }
        DocumentType.VEHICLE_INSURANCE -> "Position insurance doc in frame"
        DocumentType.CERTIFICATION -> "Position certification in frame"
    }
}