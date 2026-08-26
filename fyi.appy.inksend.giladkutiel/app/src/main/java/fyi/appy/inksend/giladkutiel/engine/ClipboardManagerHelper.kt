package fyi.appy.inksend.giladkutiel.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

object ClipboardManagerHelper {

    fun copyImageToClipboard(context: Context, imageUri: Uri) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newUri(
            context.contentResolver,
            "Styled Text Image",
            imageUri,
        )
        clipboard.setPrimaryClip(clipData)
    }
}
