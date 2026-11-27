package com.symphonysimper.launcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.symphonysimper.launcher.ui.Badges

/** Counts each app's notifications for the badges on app icons. */
class BadgeService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = refresh()
    override fun onListenerDisconnected() {
        Badges.counts = emptyMap()
    }

    private fun refresh() {
        val active = try { activeNotifications } catch (_: SecurityException) { null } ?: return
        Badges.counts = active
            .filter { !it.isOngoing && it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            .groupingBy { it.packageName }
            .eachCount()
    }
}
