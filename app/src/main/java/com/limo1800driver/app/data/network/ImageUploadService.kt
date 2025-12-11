package com.limo1800driver.app.data.network

import android.graphics.Bitmap
import android.util.Base64
import com.limo1800driver.app.data.api.DriverRegistrationApi
import com.limo1800driver.app.data.model.BaseResponse
import com.limo1800driver.app.data.model.registration.ImageUploadRequest
import com.limo1800driver.app.data.model.registration.ImageUploadResponse
import com.limo1800driver.app.data.network.error.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for uploading images
 * Converts Bitmap to Base64 and uploads via API
 */
@Singleton
class ImageUploadService @Inject constructor(
    private val registrationApi: DriverRegistrationApi,
    private val errorHandler: ErrorHandler
) {
    
    companion object {
        private const val TAG = "ImageUploadService"
        private const val JPEG_QUALITY = 90
    }
    
    /**
     * Upload a bitmap image
     * @param bitmap The image to upload
     * @return Result containing image URL or error
     */
    suspend fun uploadImage(bitmap: Bitmap): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Converting bitmap to Base64")
                
                // Convert bitmap to JPEG
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                val imageBytes = outputStream.toByteArray()
                
                // Encode to Base64 and prepend data URI prefix expected by backend
                val base64Raw = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val base64Image = "data:image/jpeg;base64,$base64Raw"
                
                Timber.tag(TAG).d("Image converted, size: ${imageBytes.size} bytes")
                
                // Upload via API
                val request = ImageUploadRequest(image = base64Image)
                val response = registrationApi.uploadSingleImage(request)
                
                if (response.success && response.data != null) {
                    val imageId = response.data.getImageId()
                    if (imageId != null) {
                        Timber.tag(TAG).d("Image uploaded successfully, ID: $imageId")
                        Result.success(imageId.toString())
                    } else {
                        val error = "Image ID not found in response"
                        Timber.tag(TAG).e("Image upload failed: $error")
                        Result.failure(Exception(error))
                    }
                } else {
                    val error = response.message ?: "Failed to upload image"
                    Timber.tag(TAG).e("Image upload failed: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                Timber.tag(TAG).e(e, "Error uploading image")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Upload image and get image ID as Int
     * @param bitmap The image to upload
     * @return Result containing image ID (Int) or error
     */
    suspend fun uploadImageAndGetId(bitmap: Bitmap): Result<Int> {
        return uploadImage(bitmap).fold(
            onSuccess = { imageIdString ->
                val imageId = imageIdString.toIntOrNull()
                if (imageId != null) {
                    Result.success(imageId)
                } else {
                    Result.failure(Exception("Invalid image ID format"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}

