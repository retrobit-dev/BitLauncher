package dev.retrobit.bitlauncher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/**
 * One launchable activity as reported by [android.content.pm.LauncherApps].
 *
 * We keep the [LauncherActivityInfo] around instead of copying every field because it is the
 * handle used to start the activity and to load the badged icon (multi-profile aware).
 */
data class LaunchableApp(
    val info: LauncherActivityInfo,
    val isWorkProfile: Boolean,
) {
    val label: String get() = info.label.toString()
    val component: ComponentName get() = info.componentName
    val packageName: String get() = component.packageName
    val className: String get() = component.className
    val user: UserHandle get() = info.user

    /** Stable identity across refreshes, also used for favorites/hidden persistence. */
    val key: String get() = "${component.flattenToString()}#${user.hashCode()}"

    /** Lower-cased label used for sorting and matching. */
    val sortKey: String get() = label.trim().lowercase()

    /** Changes when the app is updated, so cached icons are invalidated automatically. */
    val iconKey: String get() = "$key#${info.applicationInfo.lastUpdateTime}"

    val sectionLetter: Char
        get() = sortKey.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetterOrDigit() } ?: '#'
}
