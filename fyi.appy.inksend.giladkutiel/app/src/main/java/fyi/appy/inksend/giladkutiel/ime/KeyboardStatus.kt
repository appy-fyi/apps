package fyi.appy.inksend.giladkutiel.ime

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/** Enablement/selection checks shared by the Home banner and the KeyboardSetup screen. */
object KeyboardStatus {
    private fun componentName(context: Context): String = "${context.packageName}/${InkSendIme::class.java.name}"

    fun isEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val target = componentName(context)
        return imm.enabledInputMethodList.any { it.id == target }
    }

    fun isSelected(context: Context): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return current == componentName(context)
    }
}
