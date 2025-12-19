package com.limo1800driver.app.ui.components.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.limo1800driver.app.ui.utils.cropToFrame
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
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Visual State for capture feedback
    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showFlash) 0.8f else 0f,
        animationSpec = tween(150),
        label = "flash"
    )

    // Used to return the overlay coordinates to the logic
    var overlayFrame by remember { mutableStateOf<android.graphics.Rect?>(null) }
    var screenWidth by remember { mutableStateOf(0) }
    var screenHeight by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    // Gallery picker (works without storage permissions via the system picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val bitmap = decodeBitmapFromUri(context, uri)
                onImageCaptured(bitmap)
                onDismiss()
            }
        }
    )

    // --- Logic Section (Fixed) ---
    LaunchedEffect(Unit) {
        val currentGranted = checkCameraPermission(context)
        hasPermission = currentGranted
        if (!currentGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cleanup camera resources when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
                imageCapture = null
                android.util.Log.d("DocumentCamera", "Camera resources cleaned up")
            } catch (e: Exception) {
                android.util.Log.e("DocumentCamera", "Error cleaning up camera", e)
            }
        }
    }

    // --- UI Section (Redesigned) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera Preview Layer
        if (hasPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewReady = { previewView ->
                    scope.launch {
                        try {
                            // Check if PreviewView surface is ready
                            if (previewView.surfaceProvider == null) {
                                android.util.Log.w("DocumentCamera", "PreviewView surface provider not ready, retrying...")
                                // Retry after a short delay
                                kotlinx.coroutines.delay(100)
                                if (previewView.surfaceProvider == null) {
                                    android.util.Log.e("DocumentCamera", "PreviewView surface provider still not ready")
                                    return@launch
                                }
                            }

                            // Get camera provider with availability check
                            val provider = getCameraProvider(context)

                            // Build use cases
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                                android.util.Log.d("DocumentCamera", "Preview surface provider set")
                            }

                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Unbind any existing use cases first
                            provider.unbindAll()
                            android.util.Log.d("DocumentCamera", "Unbound existing camera use cases")

                            // Bind to lifecycle with proper error handling
                            val camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCaptureUseCase
                            )

                            // Store references for capture
                            cameraProvider = provider
                            imageCapture = imageCaptureUseCase

                            android.util.Log.d("DocumentCamera", "Camera bound successfully to lifecycle")
                            cameraError = null // Clear any previous errors

                        } catch (e: Exception) {
                            android.util.Log.e("DocumentCamera", "Failed to bind camera", e)
                            // Reset state on failure
                            cameraProvider = null
                            imageCapture = null
                            cameraError = when {
                                e.message?.contains("No camera available") == true ->
                                    "No camera found on this device"
                                e is androidx.camera.core.CameraUnavailableException ->
                                    "Camera is currently unavailable"
                                else ->
                                    "Failed to access camera. Please try again."
                            }
                        }
                    }
                }
            )
        }

        // 2. Scanner Overlay (The dark scrim + cutout + corner brackets)
        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            onFrameCalculated = { overlayFrame = it },
            onScreenSizeChanged = { width, height ->
                screenWidth = width
                screenHeight = height
            }
        )

        // Error display overlay
        cameraError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { cameraError = null }) {
                        Text("Retry")
                    }
                }
            }
        }

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

                // Actions Row: Gallery + Capture (centered)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick from gallery",
                            tint = Color.White
                        )
                    }

                    // Capture
                    CameraShutterButton(
                        onClick = {
                            // Flash effect trigger
                            showFlash = true

                            imageCapture?.let { capture ->
                                scope.launch {
                                    captureImage(
                                        imageCapture = capture,
                                        overlayFrame = overlayFrame,
                                        screenWidth = screenWidth,
                                        screenHeight = screenHeight,
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

                    // Spacer to keep capture button centered
                    Box(modifier = Modifier.size(56.dp))
                }
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

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } ?: return null

        // Basic downscale to avoid OOM on very large images
        val maxDim = maxOf(raw.width, raw.height)
        val target = 2000
        if (maxDim <= target) raw
        else {
            val scale = target.toFloat() / maxDim.toFloat()
            val w = (raw.width * scale).toInt().coerceAtLeast(1)
            val h = (raw.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(raw, w, h, true)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier,
    onFrameCalculated: (android.graphics.Rect) -> Unit,
    onScreenSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    Canvas(modifier = modifier) {
        val currentScreenWidth = size.width
        val currentScreenHeight = size.height

        // Update state variables for use in capture
        onScreenSizeChanged(currentScreenWidth.toInt(), currentScreenHeight.toInt())
        
        // Use the original target sizing logic
        val targetWidth = 320.dp.toPx()
        val targetHeight = 200.dp.toPx()

        val scale = minOf(
            currentScreenWidth * 0.85f / targetWidth, // Increased width usage slightly
            currentScreenHeight * 0.6f / targetHeight
        )
        val overlayWidth = targetWidth * scale
        val overlayHeight = targetHeight * scale
        val overlayX = (currentScreenWidth - overlayWidth) / 2
        val overlayY = (currentScreenHeight - overlayHeight) / 2

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
        val frameRect = android.graphics.Rect(
            overlayX.toInt(),
            overlayY.toInt(),
            (overlayX + overlayWidth).toInt(),
            (overlayY + overlayHeight).toInt()
        )
        android.util.Log.d("DocumentCamera", "Overlay frame calculated: ${frameRect.left},${frameRect.top} ${frameRect.width()}x${frameRect.height()}")
        onFrameCalculated(frameRect)
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
            PreviewView(context).apply {
                // Important: Set scale type for proper display
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // Set implementation mode for better performance
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                // Ensure surface is ready before camera binding
                post {
                    onPreviewReady(this)
                }
            }
        },
        modifier = modifier,
        update = { previewView ->
            // Handle updates if needed
        }
    )
}

private suspend fun captureImage(
    imageCapture: ImageCapture,
    overlayFrame: android.graphics.Rect?,
    screenWidth: Int,
    screenHeight: Int,
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

                // DEBUG: Log bitmap dimensions
                android.util.Log.d("DocumentCamera", "Captured bitmap: ${bitmap?.width}x${bitmap?.height}")

                val croppedBitmap = if (overlayFrame != null && bitmap != null) {
                    // Convert android.graphics.Rect to androidx.compose.ui.geometry.Rect
                    val composeRect = Rect(
                        left = overlayFrame.left.toFloat(),
                        top = overlayFrame.top.toFloat(),
                        right = overlayFrame.right.toFloat(),
                        bottom = overlayFrame.bottom.toFloat()
                    )
                    // Use the proper cropToFrame function
                    bitmap.cropToFrame(composeRect, screenWidth, screenHeight)
                } else {
                    android.util.Log.d("DocumentCamera", "No cropping applied - overlayFrame: $overlayFrame, bitmap: ${bitmap != null}")
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

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        try {
            // Check if camera is available
            val packageManager = context.packageManager
            val hasCamera = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)

            if (!hasCamera) {
                android.util.Log.e("DocumentCamera", "No camera available on this device")
                throw Exception("No camera available")
            }

            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    val provider = future.get()
                    android.util.Log.d("DocumentCamera", "Camera provider obtained successfully")
                    continuation.resume(provider)
                } catch (e: Exception) {
                    android.util.Log.e("DocumentCamera", "Failed to get camera provider", e)
                    continuation.resumeWith(kotlin.Result.failure(e))
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            android.util.Log.e("DocumentCamera", "Error getting camera provider", e)
            continuation.resumeWith(kotlin.Result.failure(e))
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