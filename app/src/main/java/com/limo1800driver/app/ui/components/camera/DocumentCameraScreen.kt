package com.limo1800driver.app.ui.components.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import cropToFrame // Ensure this extension function exists in your project
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
    // --- NEW PARAMETER ADDED HERE ---
    overlayResId: Int? = null,
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

    // Gallery picker
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

    // --- Logic Section ---
    LaunchedEffect(Unit) {
        val currentGranted = checkCameraPermission(context)
        hasPermission = currentGranted
        if (!currentGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                cameraProvider = null
                imageCapture = null
            } catch (e: Exception) {
                android.util.Log.e("DocumentCamera", "Error cleaning up camera", e)
            }
        }
    }

    // --- UI Section ---
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
                            if (previewView.surfaceProvider == null) {
                                kotlinx.coroutines.delay(100)
                                if (previewView.surfaceProvider == null) return@launch
                            }

                            val provider = getCameraProvider(context)
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCaptureUseCase
                            )

                            cameraProvider = provider
                            imageCapture = imageCaptureUseCase
                            cameraError = null

                        } catch (e: Exception) {
                            cameraProvider = null
                            imageCapture = null
                            cameraError = "Failed to access camera. Please try again."
                        }
                    }
                }
            )
        }

        // 2. Scanner Overlay (Dark Scrim + Cutout)
        ScannerOverlay(
            modifier = Modifier.fillMaxSize(),
            onFrameCalculated = { overlayFrame = it },
            onScreenSizeChanged = { width, height ->
                screenWidth = width
                screenHeight = height
            }
        )

        // 3. --- GHOST OVERLAY IMAGE (NEW) ---
        // We render this in the center to align with the scanner cutout
        if (overlayResId != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = overlayResId),
                    contentDescription = "Alignment Overlay",
                    modifier = Modifier
                        // Match width scaling logic of ScannerOverlay (approx 85% width)
                        .fillMaxWidth(0.85f)
                        // Maintain standard aspect ratio roughly matching the cutout (320/200 = 1.6)
                        .aspectRatio(1.6f)
                        .alpha(0.5f), // Semi-transparent ghost effect
                    colorFilter = ColorFilter.tint(Color.White), // Force white outline
                    contentScale = ContentScale.Fit
                )
            }
        }

        // 4. Error display overlay
        cameraError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Close, "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                    Text(error, color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = { cameraError = null }) { Text("Retry") }
                }
            }
        }

        // 5. UI Controls Layer
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
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = documentType.getTitle(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = if (side == DocumentSide.FRONT) "Front Side" else "Back Side",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
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
                Text(
                    text = documentType.getInstruction(side),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                    }

                    CameraShutterButton(
                        onClick = {
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
                                            showFlash = false
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    )

                    Box(modifier = Modifier.size(56.dp))
                }
            }
        }

        // 6. Flash Effect Layer
        if (flashAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().alpha(flashAlpha).background(Color.White))
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
private fun CameraShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "button_scale")

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color.White, style = Stroke(width = 4.dp.toPx()))
        }
        Box(modifier = Modifier.size(64.dp).background(Color.White, CircleShape))
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } ?: return null

        // Correct orientation based on EXIF data
        val corrected = correctImageOrientation(context, uri, raw)

        val maxDim = maxOf(corrected.width, corrected.height)
        val target = 2000
        if (maxDim <= target) corrected
        else {
            val scale = target.toFloat() / maxDim.toFloat()
            val w = (corrected.width * scale).toInt().coerceAtLeast(1)
            val h = (corrected.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(corrected, w, h, true)
        }
    } catch (e: Exception) { null }
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
        onScreenSizeChanged(currentScreenWidth.toInt(), currentScreenHeight.toInt())

        val targetWidth = 320.dp.toPx()
        val targetHeight = 200.dp.toPx()

        val scale = minOf(
            currentScreenWidth * 0.85f / targetWidth,
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

        val path = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            val cutout = Path().apply {
                addRoundRect(RoundRect(rect = overlayRect, cornerRadius = CornerRadius(16.dp.toPx())))
            }
            op(this, cutout, PathOperation.Difference)
        }

        drawPath(path = path, color = Color.Black.copy(alpha = 0.6f))

        val strokeWidth = 3.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val cornerColor = Color.White

        drawContext.canvas.withSave {
            // Draw corners logic (simplified for brevity, logic remains same as provided code)
            val corners = listOf(
                overlayRect.topLeft to listOf(Offset(cornerLength, 0f), Offset(0f, cornerLength)),
                overlayRect.topRight to listOf(Offset(-cornerLength, 0f), Offset(0f, cornerLength)),
                overlayRect.bottomLeft to listOf(Offset(cornerLength, 0f), Offset(0f, -cornerLength)),
                overlayRect.bottomRight to listOf(Offset(-cornerLength, 0f), Offset(0f, -cornerLength))
            )

            // Re-implementing exact drawing logic for accuracy
            // Top Left
            drawLine(cornerColor, Offset(overlayRect.left - strokeWidth/2, overlayRect.top), Offset(overlayRect.left + cornerLength, overlayRect.top), strokeWidth)
            drawLine(cornerColor, Offset(overlayRect.left, overlayRect.top), Offset(overlayRect.left, overlayRect.top + cornerLength), strokeWidth)

            // Top Right
            drawLine(cornerColor, Offset(overlayRect.right + strokeWidth/2, overlayRect.top), Offset(overlayRect.right - cornerLength, overlayRect.top), strokeWidth)
            drawLine(cornerColor, Offset(overlayRect.right, overlayRect.top), Offset(overlayRect.right, overlayRect.top + cornerLength), strokeWidth)

            // Bottom Left
            drawLine(cornerColor, Offset(overlayRect.left - strokeWidth/2, overlayRect.bottom), Offset(overlayRect.left + cornerLength, overlayRect.bottom), strokeWidth)
            drawLine(cornerColor, Offset(overlayRect.left, overlayRect.bottom), Offset(overlayRect.left, overlayRect.bottom - cornerLength), strokeWidth)

            // Bottom Right
            drawLine(cornerColor, Offset(overlayRect.right + strokeWidth/2, overlayRect.bottom), Offset(overlayRect.right - cornerLength, overlayRect.bottom), strokeWidth)
            drawLine(cornerColor, Offset(overlayRect.right, overlayRect.bottom), Offset(overlayRect.right, overlayRect.bottom - cornerLength), strokeWidth)
        }

        onFrameCalculated(android.graphics.Rect(overlayX.toInt(), overlayY.toInt(), (overlayX + overlayWidth).toInt(), (overlayY + overlayHeight).toInt()))
    }
}

// --- Helpers ---

@Composable
private fun CameraPreview(modifier: Modifier = Modifier, onPreviewReady: (PreviewView) -> Unit) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                post { onPreviewReady(this) }
            }
        },
        modifier = modifier
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
    val photoFile = File.createTempFile("capture", ".jpg", context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES))
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val file = output.savedUri?.path?.let { File(it) } ?: photoFile
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                // Correct orientation based on EXIF data
                val orientedBitmap = bitmap?.let { correctImageOrientation(file.absolutePath, it) } ?: bitmap
                val croppedBitmap = if (overlayFrame != null && orientedBitmap != null) {
                    val composeRect = Rect(overlayFrame.left.toFloat(), overlayFrame.top.toFloat(), overlayFrame.right.toFloat(), overlayFrame.bottom.toFloat())
                    orientedBitmap.cropToFrame(composeRect, screenWidth, screenHeight)
                } else orientedBitmap
                croppedBitmap?.let { onImageCaptured(it) }
                continuation.resume(Unit)
            }
            override fun onError(exception: ImageCaptureException) { continuation.resume(Unit) }
        }
    )
}

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { continuation ->
    try {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try { continuation.resume(future.get()) }
            catch (e: Exception) { continuation.resumeWith(kotlin.Result.failure(e)) }
        }, ContextCompat.getMainExecutor(context))
    } catch (e: Exception) { continuation.resumeWith(kotlin.Result.failure(e)) }
}

private fun checkCameraPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

private fun DocumentType.getTitle(): String = when (this) {
    DocumentType.DRIVING_LICENSE -> "Driving License"
    DocumentType.BUSINESS_CARD -> "Business Card"
    DocumentType.VEHICLE_INSURANCE -> "Vehicle Insurance"
    DocumentType.CERTIFICATION -> "Certification"
}

private fun DocumentType.getInstruction(side: DocumentSide): String = when (this) {
    DocumentType.DRIVING_LICENSE -> if (side == DocumentSide.FRONT) "Position front of license in frame" else "Position back of license in frame"
    DocumentType.BUSINESS_CARD -> if (side == DocumentSide.FRONT) "Position front of card in frame" else "Position back of card in frame"
    DocumentType.VEHICLE_INSURANCE -> "Position insurance doc in frame"
    DocumentType.CERTIFICATION -> "Position certification in frame"
}

/**
 * Corrects image orientation based on EXIF data.
 * @param context Context for accessing content resolver (when using Uri)
 * @param uri Uri of the image (for gallery images)
 * @param bitmap The bitmap to correct
 * @return Corrected bitmap or original if correction fails
 */
private fun correctImageOrientation(
    context: Context,
    uri: Uri,
    bitmap: Bitmap
): Bitmap {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            val rotated: Bitmap? =
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotateBitmap(bitmap, orientation)
                }

            rotated ?: bitmap   // ✅ FORCE NON-NULL

        } else {
            val filePath = uri.path
            if (filePath != null) {
                correctImageOrientation(filePath, bitmap) ?: bitmap
            } else {
                bitmap
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DocumentCamera", "Error correcting orientation from URI", e)
        bitmap
    }
}


/**
 * Corrects image orientation based on EXIF data from file path.
 * @param filePath Path to the image file
 * @param bitmap The bitmap to correct
 * @return Corrected bitmap or null if correction fails
 */
private fun correctImageOrientation(filePath: String, bitmap: Bitmap): Bitmap? {
    return try {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        rotateBitmap(bitmap, orientation)
    } catch (e: Exception) {
        android.util.Log.e("DocumentCamera", "Error correcting orientation from file", e)
        bitmap
    }
}

/**
 * Rotates a bitmap based on EXIF orientation value.
 * @param bitmap The bitmap to rotate
 * @param orientation EXIF orientation value
 * @return Rotated bitmap or original if no rotation needed
 */
private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_NORMAL -> return bitmap
        else -> return bitmap
    }

    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        android.util.Log.e("DocumentCamera", "Error rotating bitmap", e)
        bitmap
    }
}