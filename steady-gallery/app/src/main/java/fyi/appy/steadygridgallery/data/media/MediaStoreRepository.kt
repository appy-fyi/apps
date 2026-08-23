package fyi.appy.steadygridgallery.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import fyi.appy.steadygridgallery.data.db.dao.FolderStateDao
import fyi.appy.steadygridgallery.data.db.dao.HiddenMediaDao
import fyi.appy.steadygridgallery.data.db.dao.RecycleItemDao
import fyi.appy.steadygridgallery.data.db.entity.FolderStateEntity
import fyi.appy.steadygridgallery.data.db.entity.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

private val PROJECTION = arrayOf(
    MediaStore.MediaColumns._ID,
    MediaStore.MediaColumns.VOLUME_NAME,
    MediaStore.MediaColumns.BUCKET_ID,
    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
    MediaStore.MediaColumns.RELATIVE_PATH,
    MediaStore.MediaColumns.DISPLAY_NAME,
    MediaStore.MediaColumns.MIME_TYPE,
    MediaStore.MediaColumns.DATE_TAKEN,
    MediaStore.MediaColumns.DATE_ADDED,
    MediaStore.MediaColumns.WIDTH,
    MediaStore.MediaColumns.HEIGHT,
    MediaStore.MediaColumns.SIZE,
)

/** volumeName + ':' + bucketId + ':' + normalized relativePath, per the spec's folder-identity rule. */
fun computeFolderKey(volumeName: String, bucketId: String, relativePath: String): String {
    val normalized = relativePath.trim('/').lowercase(Locale.US)
    return "$volumeName:$bucketId:$normalized"
}

class MediaStoreRepository(
    private val context: Context,
    private val folderStateDao: FolderStateDao,
    private val recycleItemDao: RecycleItemDao,
    private val hiddenMediaDao: HiddenMediaDao,
) {
    private val resolver: ContentResolver get() = context.contentResolver

    /** Items either in the Recycle Bin or moved into Hidden Photos must never show up as live media. */
    private suspend fun excludedMediaStoreIds(): Set<Long> =
        (recycleItemDao.activeOriginalMediaStoreIds() + hiddenMediaDao.activeOriginalMediaStoreIds()).toSet()

    suspend fun refreshFoldersAndGetVisible(): Result<List<FolderSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val allItems = queryTable(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.IMAGE) +
                queryTable(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO)

            val excluded = excludedMediaStoreIds()
            val visibleItems = allItems.filterNot { it.mediaStoreId in excluded }

            val now = Instant.now()
            val byFolder = visibleItems.groupBy { it.folderKey }
            val folderEntities = byFolder.map { (folderKey, items) ->
                val sample = items.first()
                FolderStateEntity(
                    folderKey = folderKey,
                    displayName = sample.relativePath.trim('/').substringAfterLast('/').ifBlank { sample.relativePath },
                    relativePath = sample.relativePath,
                    sortMode = folderStateDao.getByKey(folderKey)?.sortMode ?: SortMode.DATE_DESC.name,
                    updatedAt = now,
                )
            }
            // Only touched when the query above succeeded, so a failed query never wipes
            // previously-seen folders (see acceptance criteria for folder browsing).
            folderStateDao.upsertAll(folderEntities)

            val allStates = folderEntities.associateBy { it.folderKey }

            byFolder.entries
                .map { (folderKey, items) ->
                    val sample = items.first()
                    val isCamera = sample.relativePath.contains("DCIM/Camera", ignoreCase = true)
                    val cover = items.maxByOrNull { it.dateTakenMillis.takeIf { d -> d > 0 } ?: it.dateAddedMillis }
                    FolderSummary(
                        folderKey = folderKey,
                        displayName = allStates[folderKey]?.displayName ?: sample.relativePath,
                        relativePath = sample.relativePath,
                        itemCount = items.size,
                        coverUri = cover?.contentUri,
                        isCameraFolder = isCamera,
                    )
                }
                .sortedWith(
                    compareByDescending<FolderSummary> { it.isCameraFolder }
                        .thenBy { it.displayName.lowercase(Locale.US) }
                )
        }
    }

    suspend fun loadMediaInFolder(folderKey: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val allItems = queryTable(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.IMAGE) +
                queryTable(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO)
            val excluded = excludedMediaStoreIds()
            allItems
                .filter { it.folderKey == folderKey }
                .filterNot { it.mediaStoreId in excluded }
                .sortedByDescending { it.dateTakenMillis.takeIf { d -> d > 0 } ?: it.dateAddedMillis }
        }
    }

    suspend fun findMediaItem(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        val (kind, id) = MediaIdCodec.decode(mediaId) ?: return@withContext null
        val baseUri = when (kind) {
            MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        queryTable(baseUri, kind, selection = "${MediaStore.MediaColumns._ID} = ?", selectionArgs = arrayOf(id.toString()))
            .firstOrNull()
    }

    private fun queryTable(
        baseUri: Uri,
        kind: MediaKind,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): List<MediaItem> {
        val results = mutableListOf<MediaItem>()
        resolver.query(baseUri, PROJECTION, selection, selectionArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val volumeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val relativePathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val volumeName = cursor.getStringOrNull(volumeCol) ?: MediaStore.VOLUME_EXTERNAL
                val bucketId = cursor.getStringOrNull(bucketIdCol) ?: ""
                val relativePath = cursor.getStringOrNull(relativePathCol) ?: ""
                val folderKey = computeFolderKey(volumeName, bucketId, relativePath)
                val contentUri = ContentUris.withAppendedId(baseUri, id)
                results += MediaItem(
                    mediaId = MediaIdCodec.encode(kind, id),
                    mediaStoreId = id,
                    kind = kind,
                    contentUri = contentUri,
                    folderKey = folderKey,
                    displayName = cursor.getStringOrNull(displayNameCol) ?: "",
                    mimeType = cursor.getStringOrNull(mimeTypeCol) ?: "",
                    relativePath = relativePath,
                    dateTakenMillis = cursor.getLong(dateTakenCol),
                    dateAddedMillis = cursor.getLong(dateAddedCol) * 1000L,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                )
            }
        }
        return results
    }
}
