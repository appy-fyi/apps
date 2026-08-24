package fyi.appy.inksend.giladkutiel.font

import android.content.Context
import android.graphics.Typeface
import java.io.File

/**
 * Resolves a [fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity.fontFamily] value —
 * either a [BundledFont.id] or a compiled handwriting font's file path — into a
 * loadable [Typeface], used by both the Compose style editor and the plain-Canvas
 * [fyi.appy.inksend.giladkutiel.render.TextImageRenderer].
 */
object FontResolver {
    fun resolve(context: Context, fontFamily: String): Typeface {
        val bundled = BundledFont.byId(fontFamily)
        if (bundled != null) {
            return Typeface.createFromAsset(context.assets, bundled.assetPath)
        }
        val file = File(fontFamily)
        if (file.exists()) {
            return runCatching { Typeface.createFromFile(file) }.getOrDefault(Typeface.DEFAULT)
        }
        return Typeface.DEFAULT
    }
}
