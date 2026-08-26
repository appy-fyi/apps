package fyi.appy.inksend.giladkutiel.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import fyi.appy.inksend.giladkutiel.service.TextMonitorAccessibilityService

object PermissionUtils {

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
        val enabledServices =
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo.packageName == context.packageName &&
                serviceInfo.resolveInfo.serviceInfo.name == TextMonitorAccessibilityService::class.java.name
        }
    }
}
