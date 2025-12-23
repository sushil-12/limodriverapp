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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VehicleCameraScreen(
    vehicleTypeTitle: String, // e.g., "Mid-Size Sedan"
    overlayResId: Int,        // The specific ghost image ID
    onImageCaptured: (Bitmap?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // Flash animation state
    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(targetValue = if (showFlash) 0.8f else 0f, label = "flash")

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasPermission = it }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    val bitmap = decodeBitmapFromUri(context, uri)
                    onImageCaptured(bitmap)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        post {
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                val captureUseCase = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()

                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        captureUseCase
                                    )
                                    imageCapture = captureUseCase
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    }
                }
            )
        }

        // --- THE GHOST OVERLAY ---
        // Separate from document logic: Just a centered image
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = overlayResId),
                contentDescription = "Vehicle Overlay",
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Take up 90% of screen width
                    .aspectRatio(1.33f) // Standard photo aspect ratio
                    .alpha(0.6f),       // Semi-transparent
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.Fit
            )
        }

        // --- UI OVERLAYS (Top & Bottom Bars) ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = vehicleTypeTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Align vehicle with outline",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp, start = 30.dp, end = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                }

                // Shutter Button
                VehicleShutterButton(onClick = {
                    showFlash = true
                    imageCapture?.let { capture ->
                        val photoFile = File.createTempFile("vehicle", ".jpg", context.cacheDir)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    // Vehicle images don't need cropping like documents
                                    // Just load the full bitmap
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    // Optional: Fix rotation here if needed, usually Coil/Glide handles it for display
                                    onImageCaptured(bitmap)
                                }
                                override fun onError(ex: ImageCaptureException) {
                                    // Handle error
                                }
                            }
                        )
                    }
                })

                // Spacer for balance
                Spacer(modifier = Modifier.size(50.dp))
            }
        }

        // Flash Effect
        if (flashAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().alpha(flashAlpha).background(Color.White))
            LaunchedEffect(showFlash) {
                if(showFlash) {
                    kotlinx.coroutines.delay(100)
                    showFlash = false
                }
            }
        }
    }
}

@Composable
private fun VehicleShutterButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.9f else 1.0f

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White, style = Stroke(4.dp.toPx()))
        }
        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White))
    }
}

// Reuse your existing decodeBitmapFromUri helper here
private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) { null }
}