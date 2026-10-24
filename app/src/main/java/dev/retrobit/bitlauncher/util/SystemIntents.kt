package dev.retrobit.bitlauncher.util

import android.accessibilityservice.AccessibilityService
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.notifications.LauncherAccessibilityService

object SystemIntents {

    fun appInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun uninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!launchSafely(context, intent)) {
            // Work profile apps cannot always be removed with ACTION_DELETE; show details instead.
            appInfo(context, packageName)
        }
    }

    fun openAlarms(context: Context) {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun openWallpaperPicker(context: Context) {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!launchSafely(context, intent)) {
            launchSafely(context, Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openCalendar(context: Context) {
        val uri = android.net.Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
        val viewCalendar = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!launchSafely(context, viewCalendar)) {
            launchSafely(
                context,
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    fun expandNotifications(context: Context) {
        performGestureAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "panel notifikasi")
    }

    fun openQuickSettings(context: Context) {
        performGestureAction(context, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "pengaturan cepat")
    }

    fun openRecents(context: Context) {
        performGestureAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS, "aplikasi terbaru")
    }

    fun lockScreen(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Toast.makeText(context, "Kunci layar memerlukan Android 9 atau lebih baru", Toast.LENGTH_SHORT).show()
            return
        }
        performGestureAction(context, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "kunci layar")
    }

    fun takeScreenshot(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toast.makeText(context, "Screenshot memerlukan Android 11 atau lebih baru", Toast.LENGTH_SHORT).show()
            return
        }
        performGestureAction(context, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "screenshot")
    }

    fun isGestureServiceEnabled(context: Context): Boolean =
        LauncherAccessibilityService.isEnabled(context)

    fun openGestureSettings(context: Context) {
        launchSafely(
            context,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun performGestureAction(context: Context, action: Int, label: String) {
        if (LauncherAccessibilityService.perform(action)) return
        val enabled = LauncherAccessibilityService.isEnabled(context)
        Toast.makeText(
            context,
            if (enabled) "Aksi $label belum siap; coba lagi" else "Aktifkan gestur sistem untuk $label",
            Toast.LENGTH_SHORT,
        ).show()
        if (!enabled) openGestureSettings(context)
    }

    fun openNotificationAccess(context: Context) {
        launchSafely(
            context,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun openHomeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    /** Intent that opens the "choose default home app" system dialog when possible. */
    fun defaultLauncherIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, 0) ?: return false
        return resolved.activityInfo?.packageName == context.packageName
    }

    fun openPlayStore(context: Context, packageName: String) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!launchSafely(context, market)) {
            launchSafely(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun launchSafely(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess
}

/** Human readable label for the profile an app belongs to. */
fun LaunchableApp.profileLabel(): String? = if (isWorkProfile) "Profil kerja" else null
