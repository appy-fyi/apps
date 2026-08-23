package fyi.appy.steadygridgallery.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val PERMISSIONS = "permissions"
    const val FOLDERS = "folders"
    const val MEDIA_GRID = "folder/{folderKey}"
    const val VIEWER = "viewer/{mediaId}"
    const val EDITOR = "editor/{mediaId}"
    const val RECYCLE_BIN = "recycle_bin"
    const val HIDDEN_UNLOCK = "hidden_unlock?forHide={forHide}"
    const val HIDDEN_PHOTOS = "hidden_photos"
    const val SETTINGS = "settings"
    const val PURCHASE = "purchase"

    const val ARG_FOLDER_KEY = "folderKey"
    const val ARG_MEDIA_ID = "mediaId"
    const val ARG_FOR_HIDE = "forHide"

    /** folderKey embeds ':' and '/' (see MediaStoreRepository.computeFolderKey), so it must be encoded as one path segment. */
    fun mediaGrid(folderKey: String): String = "folder/${encode(folderKey)}"

    fun viewer(mediaId: String): String = "viewer/${encode(mediaId)}"

    /**
     * [forHide] distinguishes "unlock to hide the current selection" (returns to the caller once
     * a PIN exists/is verified) from "unlock to browse Hidden Photos" (navigates to that screen).
     */
    fun hiddenUnlock(forHide: Boolean = false): String = "hidden_unlock?forHide=$forHide"

    fun editor(mediaId: String): String = "editor/${encode(mediaId)}"

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
