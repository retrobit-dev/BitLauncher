package dev.retrobit.bitlauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for installed / launchable apps.
 *
 * Uses [LauncherApps] instead of PackageManager so that:
 *  - work profile (multi-user) apps are included and badged,
 *  - package change callbacks are delivered without polling,
 *  - activities declared as launcher entries are resolved correctly.
 */
class LauncherRepository(private val context: Context) {

    private val launcherApps: LauncherApps? =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var started = false

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()

        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()

        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
    }

    fun start() {
        if (started) return
        started = true
        runCatching { launcherApps?.registerCallback(callback) }
        refresh()
    }

    fun stop() {
        runCatching { launcherApps?.unregisterCallback(callback) }
        started = false
    }

    fun refresh() {
        val launcher = launcherApps ?: run {
            _loading.value = false
            return
        }
        scope.launch {
            _loading.value = true
            val myUser = Process.myUserHandle()
            val profiles = runCatching { launcher.profiles }.getOrDefault(listOf(myUser))
            val result = ArrayList<LaunchableApp>()
            profiles.forEach { user ->
                runCatching { launcher.getActivityList(null, user) }
                    .getOrDefault(emptyList())
                    .forEach { activityInfo ->
                        if (activityInfo.componentName.packageName == context.packageName) return@forEach
                        result += LaunchableApp(
                            info = activityInfo,
                            isWorkProfile = user != myUser,
                        )
                    }
            }
            _apps.value = result.sortedWith(compareBy({ it.sortKey }, { it.component.flattenToString() }))
            _loading.value = false
        }
    }

    fun launch(app: LaunchableApp, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps?.startMainActivity(app.component, app.user, sourceBounds, null)
        }
    }

    fun shortcuts(app: LaunchableApp): List<ShortcutInfo> {
        val launcher = launcherApps ?: return emptyList()
        return runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            launcher.getShortcuts(query, app.user)
        }.getOrNull().orEmpty()
    }

    fun launchShortcut(app: LaunchableApp, shortcut: ShortcutInfo) {
        runCatching {
            launcherApps?.startShortcut(app.packageName, shortcut.id, null, null, app.user)
        }
    }
}
