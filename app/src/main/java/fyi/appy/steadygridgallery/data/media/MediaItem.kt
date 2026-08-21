package fyi.appy.steadygridgallery.data.media

import android.net.Uri

enum class MediaKind { IMAGE, VIDEO }

/**
 * [mediaId] is a synthetic, app-wide unique id ("image_123" / "video_45") because MediaStore's
 * Images and Video tables each have their own independently-numbered _ID column.
 */
data class MediaItem(
    val mediaId: String,
    val mediaStoreId: Long,
    val kind: MediaKind,
    val contentUri: Uri,
    val folderKey: String,
    val displayName: String,
    val mimeType: String,
    val relativePath: String,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)

data class FolderSummary(
    val folderKey: String,
    val displayName: String,
    val relativePath: String,
    val itemCount: Int,
    val coverUri: Uri?,
    val isCameraFolder: Boolean,
)

object MediaIdCodec {
    fun encode(kind: MediaKind, id: Long): String = "${kind.name.lowercase()}_$id"

    fun decode(mediaId: String): Pair<MediaKind, Long>? {
        val separatorIndex = mediaId.lastIndexOf('_')
        if (separatorIndex <= 0) return null
        val kind = when (mediaId.substring(0, separatorIndex)) {
            "image" -> MediaKind.IMAGE
            "video" -> MediaKind.VIDEO
            else -> return null
        }
        val id = mediaId.substring(separatorIndex + 1).toLongOrNull() ?: return null
        return kind to id
    }
}

