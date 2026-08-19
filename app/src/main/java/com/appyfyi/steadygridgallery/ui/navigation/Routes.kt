package com.appyfyi.steadygridgallery.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val PERMISSIONS = "permissions"
    const val FOLDERS = "folders"
    const val MEDIA_GRID = "folder/{folderKey}"
    const val VIEWER = "viewer/{mediaId}"
    const val EDITOR = "editor/{mediaId}"
    const val RECYCLE_BIN = "recycle_bin"
    const val HIDDEN_UNLOCK = "hidden_unlock?pendingHideFolderKey={pendingHideFolderKey}"
    const val HIDDEN_FOLDERS = "hidden_folders"
    const val SETTINGS = "settings"
    const val PURCHASE = "purchase"

    const val ARG_FOLDER_KEY = "folderKey"
    const val ARG_MEDIA_ID = "mediaId"
    const val ARG_PENDING_HIDE_FOLDER_KEY = "pendingHideFolderKey"

    /** folderKey embeds ':' and '/' (see MediaStoreRepository.computeFolderKey), so it must be encoded as one path segment. */
    fun mediaGrid(folderKey: String): String = "folder/${encode(folderKey)}"

    fun viewer(mediaId: String): String = "viewer/${encode(mediaId)}"

    /**
     * [pendingHideFolderKey] carries the folder a "hide folder" tap was for when no PIN exists yet,
     * so it can be hidden once PIN setup finishes instead of losing the original intent.
     */
    fun hiddenUnlock(pendingHideFolderKey: String? = null): String =
        "hidden_unlock?pendingHideFolderKey=${encode(pendingHideFolderKey ?: "")}"

    fun editor(mediaId: String): String = "editor/${encode(mediaId)}"

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
