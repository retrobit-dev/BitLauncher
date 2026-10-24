package dev.retrobit.bitlauncher.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Publishes count-only updates whenever the system notification set changes. */
class BitNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationCenter.setConnected(true)
        NotificationCenter.publish(this)
    }

    override fun onListenerDisconnected() {
        NotificationCenter.setConnected(false)
    }

    override fun onNotificationPosted(notification: StatusBarNotification?) {
        NotificationCenter.publish(this)
    }

    override fun onNotificationRemoved(notification: StatusBarNotification?) {
        NotificationCenter.publish(this)
    }
}
