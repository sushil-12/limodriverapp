import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect

fun Bitmap.cropToFrame(
    frameRect: Rect,
    containerWidth: Int,
    containerHeight: Int
): Bitmap {
    // 1. Calculate the scale difference between the screen (container) and the actual image (bitmap)
    val widthScale = this.width.toFloat() / containerWidth
    val heightScale = this.height.toFloat() / containerHeight

    // 2. Determine the crop coordinates on the actual Bitmap
    val cropLeft = (frameRect.left * widthScale).toInt().coerceAtLeast(0)
    val cropTop = (frameRect.top * heightScale).toInt().coerceAtLeast(0)

    // 3. Determine width/height, ensuring we don't go outside the bitmap bounds
    val cropWidth = (frameRect.width * widthScale).toInt().coerceAtMost(this.width - cropLeft)
    val cropHeight = (frameRect.height * heightScale).toInt().coerceAtMost(this.height - cropTop)

    // 4. Create the cropped bitmap
    return Bitmap.createBitmap(this, cropLeft, cropTop, cropWidth, cropHeight)
}