package fyi.appy.steadygridgallery.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.util.UUID

/**
 * Exports an edited bitmap into MediaStore under Pictures/Steady Gallery, split into a
 * pending-write step and a separate publish step so the caller can report distinct progress
 * checkpoints (90% once bytes are written but still IS_PENDING, 100% once published).
 */
object ImageExporter {

    fun writePending(context: Context, bitmap: Bitmap, sourceMimeType: String): Uri {
        val isPng = sourceMimeType == "image/png"
        val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val extension = if (isPng) "png" else "jpg"
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val displayName = "steady_${UUID.randomUUID()}.$extension"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Steady Gallery")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore refused to create the export destination")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(format, 95, out)) {
                    error("Bitmap compression failed")
                }
            } ?: error("Unable to open export destination for writing")
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }

        return uri
    }

    fun publish(context: Context, uri: Uri) {
        val clearPending = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, clearPending, null, null)
    }

    /**
     * Overwrites the bytes of an existing MediaStore entry in place ("wt" truncates before
     * writing). Caller is responsible for holding write access to [uri] -- e.g. via a granted
     * [android.provider.MediaStore.createWriteRequest] -- since the entry may not have been
     * created by this app.
     */
    fun overwrite(context: Context, uri: Uri, bitmap: Bitmap, mimeType: String) {
        val format = if (mimeType == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            if (!bitmap.compress(format, 95, out)) {
                error("Bitmap compression failed")
            }
        } ?: error("Unable to open destination for writing")
    }

    fun deletePending(context: Context, uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }
}
