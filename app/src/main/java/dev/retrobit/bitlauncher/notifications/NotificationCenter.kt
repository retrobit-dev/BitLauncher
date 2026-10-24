package dev.retrobit.bitlauncher.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local feed of active notification counts.
 *
 * Only package names and counts are exposed to the UI. Notification text, titles, and actions
 * never leave the listener. Android counts are an approximation of unread items: some apps post
 * one notification for several messages, while others post one per conversation.
 */
object NotificationCenter {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val _accessGranted = MutableStateFlow(false)
    val accessGranted: StateFlow<Boolean> = _accessGranted.asStateFlow()

    fun refreshAccess(context: Context) {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _accessGranted.value = granted
        if (!granted) _counts.value = emptyMap()
    }

    fun setConnected(connected: Boolean) {
        _accessGranted.value = connected
        if (!connected) _counts.value = emptyMap()
    }

    fun publish(listener: NotificationListenerService) {
        val active = runCatching { listener.activeNotifications?.toList().orEmpty() }
            .getOrDefault(emptyList())
        val relevant = active.filterNot { notification ->
            notification.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        }
        val regular = relevant.filterNot { notification ->
            notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        }
        val grouped = regular.groupBy { it.packageName }.toMutableMap()

        // Some apps only expose a group summary. Use it as a fallback, never in addition to its
        // children, to avoid counting the same unread items twice.
        relevant.filter { notification ->
            notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        }.forEach { summary ->
            if (summary.packageName !in grouped) {
                grouped[summary.packageName] = listOf(summary)
            }
        }

        _counts.value = grouped.mapNotNull { (packageName, notifications) ->
            val count = notifications.sumOf(::notificationCount).coerceAtLeast(0)
            if (count > 0) packageName to count else null
        }.toMap()
    }

    private fun notificationCount(notification: StatusBarNotification): Int {
        // Notification.number is used by several messaging apps for the unread count in that
        // notification. For ordinary notifications it is zero, meaning one active item.
        return notification.notification.number.coerceAtLeast(1)
    }
}
