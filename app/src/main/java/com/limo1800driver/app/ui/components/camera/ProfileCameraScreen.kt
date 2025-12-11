package com.limo1800driver.app.ui.components.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.limo1800driver.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import com.limo1800driver.app.ui.components.RegistrationTopBar

// --- Main Screen ---
@Composable
fun ProfileCameraScreen(
    onImageCaptured: (Bitmap?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var hasPermission by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // Initial Permission Check
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        RegistrationTopBar(onBack = onDismiss)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (hasPermission) {
                CameraContent(
                    onImageCaptureCreated = { imageCapture = it }
                )
                
                CameraOverlay()
                
                CameraControls(
                    onDismiss = onDismiss,
                    onCaptureClick = {
                        imageCapture?.let { capture ->
                            scope.launch {
                                val bitmap = CameraUtils.captureAndCropImage(context, capture)
                                onImageCaptured(bitmap)
                            }
                        }
                    }
                )
            } else {
                Text(
                    text = "Camera permission required",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// --- Components ---

@Composable
private fun CameraContent(
    onImageCaptureCreated: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // 1. Preview UseCase
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                // 2. ImageCapture UseCase
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                onImageCaptureCreated(imageCapture)

                // 3. Bind to Lifecycle
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun CameraOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val screenWidth = size.width
        val screenHeight = size.height

        // Calculate centralized oval 
        // Logic: 90% width, maintain 3:4 aspect ratio, centered
        val overlayWidth = screenWidth * 0.90f
        val overlayHeight = overlayWidth * (4f / 3f)
        
        val left = (screenWidth - overlayWidth) / 2
        val top = (screenHeight - overlayHeight) / 2
        
        // 1. Dim Background
        drawRect(
            color = Color.Black.copy(alpha = 0.7f),
            size = size
        )

        // 2. Cutout (Clear Mode)
        drawOval(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(overlayWidth, overlayHeight),
            blendMode = BlendMode.Clear
        )

        // 3. White Border
        drawOval(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(overlayWidth, overlayHeight),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun CameraControls(
    onDismiss: () -> Unit,
    onCaptureClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Take a selfie",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        // Bottom Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Position your face in the oval",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Button(
                onClick = onCaptureClick,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(0.dp)
            ) {
                // Inner circle for aesthetics
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                )
            }
        }
    }
}

// --- Logic / Utils ---

object CameraUtils {
    
    suspend fun captureAndCropImage(
        context: Context, 
        imageCapture: ImageCapture
    ): Bitmap? = suspendCoroutine { continuation ->
        
        // Create temp file
        val photoFile = File.createTempFile("img_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Switch to background thread for heavy lifting
                    // We use a coroutine here to resume the suspend function
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val bitmap = processImage(photoFile.absolutePath)
                            continuation.resume(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continuation.resume(null)
                        } finally {
                            // cleanup temp file
                            photoFile.delete() 
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    exc.printStackTrace()
                    continuation.resume(null)
                }
            }
        )
    }

    // Runs on IO Dispatcher
    private suspend fun processImage(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeFile(path) ?: return@withContext null

        // 1. Rotate if needed (Selfie cameras are often rotated)
        // Note: In a full prod app, check Exif data here. 
        // For simplicity in this snippet, we assume CameraX handled rotation via metadata, 
        // but often we need a Matrix rotate if the Bitmap is raw. 
        // However, let's focus on the cropping logic.

        // 2. Center Crop to 3:4 Aspect Ratio (matching the UI overlay)
        val width = original.width
        val height = original.height
        val targetRatio = 3f / 4f
        
        val cropWidth: Int
        val cropHeight: Int
        
        if (width.toFloat() / height > targetRatio) {
            // Image is too wide
            cropHeight = height
            cropWidth = (height * targetRatio).toInt()
        } else {
            // Image is too tall
            cropWidth = width
            cropHeight = (width / targetRatio).toInt()
        }

        val cx = (width - cropWidth) / 2
        val cy = (height - cropHeight) / 2

        val centeredBitmap = Bitmap.createBitmap(original, cx, cy, cropWidth, cropHeight)
        
        // 3. Mask to Oval
        return@withContext getCircularBitmap(centeredBitmap)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val color = AndroidColor.RED
        val paint = Paint()
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        
        // Draw the oval mask
        canvas.drawOval(rect, paint)
        
        // SRC_IN means: Keep the source pixels (the photo) only where they overlap with the destination (the oval)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
}