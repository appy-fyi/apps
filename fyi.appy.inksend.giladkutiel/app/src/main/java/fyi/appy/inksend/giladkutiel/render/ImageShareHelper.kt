package fyi.appy.inksend.giladkutiel.render

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val WHATSAPP_PACKAGE = "com.whatsapp"

object ImageShareHelper {
    /** Saves [bitmap] to the app's FileProvider cache dir and returns a content:// Uri. */
    fun saveToCache(context: Context, bitmap: Bitmap): android.net.Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "styled_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Targets WhatsApp directly when installed (one tap, no chooser); falls back to the
     * system share sheet — never an interstitial or ad screen — when it isn't.
     */
    fun buildSendIntent(context: Context, imageUri: android.net.Uri): Intent {
        val whatsAppIntent = Intent(Intent.ACTION_SEND).apply {
            `package` = WHATSAPP_PACKAGE
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val whatsAppAvailable = whatsAppIntent.resolveActivity(context.packageManager) != null
        if (whatsAppAvailable) return whatsAppIntent

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
