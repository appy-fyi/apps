package com.appyfyi.steadygridgallery.data.recycle

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.appyfyi.steadygridgallery.data.db.dao.RecycleItemDao
import com.appyfyi.steadygridgallery.data.db.entity.DeleteState
import com.appyfyi.steadygridgallery.data.db.entity.RecycleItemEntity
import com.appyfyi.steadygridgallery.data.media.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID

class RecycleRepository(
    private val context: Context,
    private val recycleItemDao: RecycleItemDao,
) {
    private val recycleDir: File
        get() = File(context.getExternalFilesDir(null), "RecycleBin").apply { mkdirs() }

    fun observeActive(): Flow<List<RecycleItemEntity>> = recycleItemDao.observeActive()

    /**
     * Copies the source media to an app-private file and verifies the copy byte-for-byte before
     * ever asking the system to delete the original. Only after verification passes is the
     * RecycleItem row written, with state COPIED_PENDING_SYSTEM_DELETE.
     */
    suspend fun copyAndVerify(item: MediaItem): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val destination = File(recycleDir, "${UUID.randomUUID()}_${item.displayName}")
            val copyResult = context.contentResolver.openInputStream(item.contentUri)?.use { input ->
                FileHasher.copyWithSha256(input, destination)
            } ?: error("Unable to open source media for reading: ${item.contentUri}")

            val verified = FileHasher.verifyCopy(destination, copyResult.sha256Hex, copyResult.sizeBytes)
            if (!verified) {
                destination.delete()
                error("Recycle copy verification failed for ${item.displayName}")
            }

            val entity = RecycleItemEntity(
                originalMediaStoreId = item.mediaStoreId,
                originalUri = item.contentUri.toString(),
                displayName = item.displayName,
                mimeType = item.mimeType,
                relativePath = item.relativePath,
                dateTakenMillis = item.dateTakenMillis,
                trashedCopyPath = destination.absolutePath,
                sha256 = copyResult.sha256Hex,
                sizeBytes = copyResult.sizeBytes,
                deletedAt = Instant.now(),
                deleteState = DeleteState.COPIED_PENDING_SYSTEM_DELETE.name,
            )
            recycleItemDao.insert(entity)
        }
    }

    /** Builds the system delete-confirmation PendingIntent for the given content URIs (API 30+). */
    fun buildDeleteRequest(uris: List<Uri>): PendingIntent =
        MediaStore.createDeleteRequest(context.contentResolver, uris)

    suspend fun markRecycled(recycleItemId: Long) = withContext(Dispatchers.IO) {
        recycleItemDao.setState(recycleItemId, DeleteState.RECYCLED.name)
    }

    suspend fun restore(recycleItemId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val item = recycleItemDao.getById(recycleItemId) ?: error("Recycle item $recycleItemId not found")
            val sourceFile = File(item.trashedCopyPath)
            if (!sourceFile.exists()) error("Recycled copy missing on disk for ${item.displayName}")

            val isVideo = item.mimeType.startsWith("video/")
            val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, item.relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val newUri = context.contentResolver.insert(collection, values)
                ?: error("MediaStore refused to create a restore target for ${item.displayName}")

            context.contentResolver.openOutputStream(newUri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Unable to open restore destination for ${item.displayName}")

            val clearPending = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(newUri, clearPending, null, null)

            val restoredSize = context.contentResolver.openFileDescriptor(newUri, "r")?.use { it.statSize } ?: 0L
            if (restoredSize <= 0) error("Restored file for ${item.displayName} has zero size")

            recycleItemDao.setState(recycleItemId, DeleteState.RESTORED.name)
        }.onFailure {
            recycleItemDao.setState(recycleItemId, DeleteState.ERROR.name)
        }
    }

    suspend fun permanentlyDelete(recycleItemId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val item = recycleItemDao.getById(recycleItemId) ?: error("Recycle item $recycleItemId not found")
            File(item.trashedCopyPath).delete()
            recycleItemDao.setState(recycleItemId, DeleteState.PERMANENTLY_DELETED.name)
        }
    }
}
