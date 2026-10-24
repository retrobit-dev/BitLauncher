package dev.retrobit.bitlauncher.notifications

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Performs only the system notification-panel action after an explicit accessibility opt-in.
 * No accessibility events, window content, or screen text are requested or retained.
 */
class NotificationPanelAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: NotificationPanelAccessibilityService? = null

        fun expandNotifications(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, NotificationPanelAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            return enabled.split(':').any { component ->
                ComponentName.unflattenFromString(component) == expected
            }
        }
    }
}
