package fyi.appy.steadygridgallery.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** Pure bitmap-transform steps used by the editor export pipeline, kept separate from I/O and progress reporting. */
object ImageEditProcessor {

    fun decodeWithExifOrientation(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }

        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        return if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
    }

    fun crop(bitmap: Bitmap, cropRectPx: Rect): Bitmap {
        val safeRect = Rect(
            cropRectPx.left.coerceIn(0, bitmap.width - 1),
            cropRectPx.top.coerceIn(0, bitmap.height - 1),
            cropRectPx.right.coerceIn(1, bitmap.width),
            cropRectPx.bottom.coerceIn(1, bitmap.height),
        )
        val width = (safeRect.right - safeRect.left).coerceAtLeast(1)
        val height = (safeRect.bottom - safeRect.top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, width, height)
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyFilter(bitmap: Bitmap, filter: EditFilter): Bitmap {
        if (filter == EditFilter.ORIGINAL) return bitmap
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrixFor(filter))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun colorMatrixFor(filter: EditFilter): ColorMatrix = when (filter) {
        EditFilter.ORIGINAL -> ColorMatrix()
        EditFilter.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
        EditFilter.SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        EditFilter.HIGH_CONTRAST -> {
            val contrast = 1.6f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
    }
}
