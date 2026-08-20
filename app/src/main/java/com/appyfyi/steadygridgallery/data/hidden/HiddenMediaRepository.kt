package com.appyfyi.steadygridgallery.data.hidden

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.appyfyi.steadygridgallery.data.db.dao.HiddenMediaDao
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaEntity
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaState
import com.appyfyi.steadygridgallery.data.media.MediaItem
import com.appyfyi.steadygridgallery.data.media.MediaKind
import com.appyfyi.steadygridgallery.data.recycle.FileHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID

class HiddenMediaRepository(
    private val context: Context,
    private val hiddenMediaDao: HiddenMediaDao,
) {
    private val hiddenDir: File
        get() = File(context.getExternalFilesDir(null), "HiddenPhotos").apply { mkdirs() }

    fun observeHidden(): Flow<List<HiddenMediaEntity>> = hiddenMediaDao.observeActive()

    suspend fun activeOriginalMediaStoreIds(): List<Long> = hiddenMediaDao.activeOriginalMediaStoreIds()

    /**
     * Copies the source media into the app-private hidden store and verifies the copy byte-for-byte
     * before the original is ever asked to be deleted, exactly like RecycleRepository.copyAndVerify.
     */
    suspend fun copyAndVerify(item: MediaItem): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val destination = File(hiddenDir, "${UUID.randomUUID()}_${item.displayName}")
            val copyResult = context.contentResolver.openInputStream(item.contentUri)?.use { input ->
                FileHasher.copyWithSha256(input, destination)
            } ?: error("Unable to open source media for reading: ${item.contentUri}")

            val verified = FileHasher.verifyCopy(destination, copyResult.sha256Hex, copyResult.sizeBytes)
            if (!verified) {
                destination.delete()
                error("Hide copy verification failed for ${item.displayName}")
            }

            val entity = HiddenMediaEntity(
                originalMediaStoreId = item.mediaStoreId,
                kind = item.kind.name,
                displayName = item.displayName,
                mimeType = item.mimeType,
                relativePath = item.relativePath,
                dateTakenMillis = item.dateTakenMillis,
                dateAddedMillis = item.dateAddedMillis,
                hiddenCopyPath = destination.absolutePath,
                sha256 = copyResult.sha256Hex,
                sizeBytes = copyResult.sizeBytes,
                hiddenAt = Instant.now(),
                state = HiddenMediaState.COPIED_PENDING_SYSTEM_DELETE.name,
            )
            hiddenMediaDao.insert(entity)
        }
    }

    /** Builds the system delete-confirmation PendingIntent for the given content URIs (API 30+). */
    fun buildDeleteRequest(uris: List<Uri>): PendingIntent =
        MediaStore.createDeleteRequest(context.contentResolver, uris)

    suspend fun markHidden(id: Long) = withContext(Dispatchers.IO) {
        hiddenMediaDao.setState(id, HiddenMediaState.HIDDEN.name)
    }

    suspend fun unhide(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val item = hiddenMediaDao.getById(id) ?: error("Hidden item $id not found")
            val sourceFile = File(item.hiddenCopyPath)
            if (!sourceFile.exists()) error("Hidden copy missing on disk for ${item.displayName}")

            val isVideo = item.kind == MediaKind.VIDEO.name
            val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, item.relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val newUri = context.contentResolver.insert(collection, values)
                ?: error("MediaStore refused to create an unhide target for ${item.displayName}")

            context.contentResolver.openOutputStream(newUri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Unable to open unhide destination for ${item.displayName}")

            val clearPending = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(newUri, clearPending, null, null)

            val restoredSize = context.contentResolver.openFileDescriptor(newUri, "r")?.use { it.statSize } ?: 0L
            if (restoredSize <= 0) error("Unhidden file for ${item.displayName} has zero size")

            // Unlike RecycleRepository.restore(), the private copy is deleted here: leaving a
            // second copy of a photo the user just asked to un-hide sitting in app storage would
            // defeat the point of a "hidden photos" feature.
            sourceFile.delete()
            hiddenMediaDao.deleteRow(id)
        }.onFailure {
            hiddenMediaDao.setState(id, HiddenMediaState.ERROR.name)
        }
    }
}
