package com.appyfyi.steadygridgallery.ui.screens.mediagrid

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.R
import com.appyfyi.steadygridgallery.data.db.dao.FolderStateDao
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import com.appyfyi.steadygridgallery.data.hidden.HiddenMediaRepository
import com.appyfyi.steadygridgallery.data.media.MediaItem
import com.appyfyi.steadygridgallery.data.media.MediaStoreRepository
import com.appyfyi.steadygridgallery.data.prefs.LockCredentialStore
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import com.appyfyi.steadygridgallery.data.recycle.RecycleRepository
import com.appyfyi.steadygridgallery.ui.common.appContainer
import com.appyfyi.steadygridgallery.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class MediaGridPhase { LOADING, EMPTY, ERROR, POPULATED, SELECTION_ACTIVE }

data class MediaGridUiState(
    val phase: MediaGridPhase = MediaGridPhase.LOADING,
    val folderDisplayName: String = "",
    val folderRelativePath: String = "",
    val items: List<MediaItem> = emptyList(),
    val sortMode: SortMode = SortMode.DATE_DESC,
    val selectedIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

sealed interface MediaGridEvent {
    data class ConfirmSystemDelete(val pendingIntent: PendingIntent, val recycleItemIds: List<Long>) : MediaGridEvent
    data class ConfirmSystemHide(val pendingIntent: PendingIntent, val hiddenItemIds: List<Long>) : MediaGridEvent
    data object RequiresPurchaseToHide : MediaGridEvent
    data object RequiresPinSetupToHide : MediaGridEvent
    data class RecycleFailed(val message: String) : MediaGridEvent
    data class HideFailed(val message: String) : MediaGridEvent
}

class MediaGridViewModel(
    private val folderKey: String,
    private val mediaRepository: MediaStoreRepository,
    private val folderStateDao: FolderStateDao,
    private val recycleRepository: RecycleRepository,
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val purchaseEntitlementStore: PurchaseEntitlementStore,
    private val lockCredentialStore: LockCredentialStore,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaGridUiState())
    val uiState: StateFlow<MediaGridUiState> = _uiState

    private val eventChannel = Channel<MediaGridEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    // Set by hideSelected() when a hide was requested but no PIN exists yet; consumed by load()
    // once the PIN has been created, so hiding resumes automatically after PIN setup completes.
    private var pendingHideMediaIds: Set<String>? = null

    fun load() {
        _uiState.value = _uiState.value.copy(phase = MediaGridPhase.LOADING, errorMessage = null)
        viewModelScope.launch {
            val folderState = folderStateDao.getByKey(folderKey)
            val sortMode = folderState?.sortMode?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
                ?: SortMode.DATE_DESC

            mediaRepository.loadMediaInFolder(folderKey)
                .onSuccess { items ->
                    val sorted = sortItems(items, sortMode)
                    _uiState.value = _uiState.value.copy(
                        phase = if (sorted.isEmpty()) MediaGridPhase.EMPTY else MediaGridPhase.POPULATED,
                        folderDisplayName = folderState?.displayName ?: sorted.firstOrNull()?.relativePath.orEmpty(),
                        folderRelativePath = folderState?.relativePath ?: sorted.firstOrNull()?.relativePath.orEmpty(),
                        items = sorted,
                        sortMode = sortMode,
                        selectedIds = emptySet(),
                    )
                    val pending = pendingHideMediaIds
                    if (pending != null && lockCredentialStore.hasCredential()) {
                        pendingHideMediaIds = null
                        hidePhotos(pending)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        phase = MediaGridPhase.ERROR,
                        errorMessage = error.message ?: appContext.getString(R.string.media_grid_load_error_fallback),
                    )
                }
        }
    }

    fun changeSortMode(sortMode: SortMode) {
        viewModelScope.launch {
            folderStateDao.setSortMode(folderKey, sortMode.name)
            _uiState.value = _uiState.value.copy(
                sortMode = sortMode,
                items = sortItems(_uiState.value.items, sortMode),
            )
        }
    }

    private fun sortItems(items: List<MediaItem>, sortMode: SortMode): List<MediaItem> = when (sortMode) {
        SortMode.DATE_DESC -> items.sortedByDescending { it.dateTakenMillis.takeIf { d -> d > 0 } ?: it.dateAddedMillis }
        SortMode.DATE_ASC -> items.sortedBy { it.dateTakenMillis.takeIf { d -> d > 0 } ?: it.dateAddedMillis }
        SortMode.NAME_ASC -> items.sortedBy { it.displayName.lowercase() }
        SortMode.NAME_DESC -> items.sortedByDescending { it.displayName.lowercase() }
    }

    fun toggleSelection(mediaId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (mediaId in current) current - mediaId else current + mediaId
        _uiState.value = _uiState.value.copy(
            selectedIds = updated,
            phase = if (updated.isEmpty()) MediaGridPhase.POPULATED else MediaGridPhase.SELECTION_ACTIVE,
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet(), phase = MediaGridPhase.POPULATED)
    }

    fun deleteSelectedToRecycle() {
        val selected = _uiState.value.items.filter { it.mediaId in _uiState.value.selectedIds }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val recycleItemIds = mutableListOf<Long>()
            for (item in selected) {
                recycleRepository.copyAndVerify(item)
                    .onSuccess { recycleItemIds += it }
                    .onFailure {
                        eventChannel.send(
                            MediaGridEvent.RecycleFailed(
                                it.message ?: appContext.getString(R.string.media_grid_recycle_failed_format, item.displayName),
                            ),
                        )
                    }
            }
            if (recycleItemIds.isNotEmpty()) {
                val uris = selected.map { it.contentUri }
                val pendingIntent = recycleRepository.buildDeleteRequest(uris)
                eventChannel.send(MediaGridEvent.ConfirmSystemDelete(pendingIntent, recycleItemIds))
            }
        }
    }

    fun onSystemDeleteResult(confirmed: Boolean, recycleItemIds: List<Long>) {
        viewModelScope.launch {
            if (confirmed) {
                recycleItemIds.forEach { recycleRepository.markRecycled(it) }
            }
            // If cancelled, rows stay COPIED_PENDING_SYSTEM_DELETE -- the original remains in the
            // library and the protected copy is already safe in Recycle Bin.
            load()
            clearSelection()
        }
    }

    fun hideSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when {
                !purchaseEntitlementStore.isPurchased.value -> eventChannel.send(MediaGridEvent.RequiresPurchaseToHide)
                !lockCredentialStore.hasCredential() -> {
                    pendingHideMediaIds = ids
                    eventChannel.send(MediaGridEvent.RequiresPinSetupToHide)
                }
                else -> hidePhotos(ids)
            }
        }
    }

    private suspend fun hidePhotos(mediaIds: Set<String>) {
        val selected = _uiState.value.items.filter { it.mediaId in mediaIds }
        if (selected.isEmpty()) return
        val succeeded = mutableListOf<Pair<Long, MediaItem>>()
        for (item in selected) {
            hiddenMediaRepository.copyAndVerify(item)
                .onSuccess { hiddenItemId -> succeeded += hiddenItemId to item }
                .onFailure {
                    eventChannel.send(
                        MediaGridEvent.HideFailed(
                            it.message ?: appContext.getString(R.string.media_grid_hide_failed_format, item.displayName),
                        ),
                    )
                }
        }
        if (succeeded.isNotEmpty()) {
            val uris = succeeded.map { it.second.contentUri }
            val pendingIntent = hiddenMediaRepository.buildDeleteRequest(uris)
            eventChannel.send(MediaGridEvent.ConfirmSystemHide(pendingIntent, succeeded.map { it.first }))
        }
    }

    fun onSystemHideResult(confirmed: Boolean, hiddenItemIds: List<Long>) {
        viewModelScope.launch {
            if (confirmed) {
                hiddenItemIds.forEach { hiddenMediaRepository.markHidden(it) }
            }
            // If cancelled, rows stay COPIED_PENDING_SYSTEM_DELETE -- the original remains in the
            // library and the protected copy is already safe in Hidden Photos.
            load()
            clearSelection()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val encodedFolderKey = savedStateHandle.get<String>(Routes.ARG_FOLDER_KEY).orEmpty()
                val folderKey = Routes.decode(encodedFolderKey)
                val container = appContainer()
                MediaGridViewModel(
                    folderKey = folderKey,
                    mediaRepository = container.mediaStoreRepository,
                    folderStateDao = container.database.folderStateDao(),
                    recycleRepository = container.recycleRepository,
                    hiddenMediaRepository = container.hiddenMediaRepository,
                    purchaseEntitlementStore = container.purchaseEntitlementStore,
                    lockCredentialStore = container.lockCredentialStore,
                    appContext = container.appContext,
                )
            }
        }
    }
}
