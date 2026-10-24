package dev.retrobit.bitlauncher

import android.app.Application
import android.content.pm.ShortcutInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.retrobit.bitlauncher.data.AppLayout
import dev.retrobit.bitlauncher.data.IconPackRepository
import dev.retrobit.bitlauncher.data.IconPackSelection
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.data.LauncherRepository
import dev.retrobit.bitlauncher.data.LauncherSettings
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.data.SettingsRepository
import dev.retrobit.bitlauncher.data.ThemeMode
import dev.retrobit.bitlauncher.notifications.NotificationCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings
    val loading: StateFlow<Boolean> = repository.loading
    val notificationCounts: StateFlow<Map<String, Int>> = NotificationCenter.counts
    val notificationAccess: StateFlow<Boolean> = NotificationCenter.accessGranted

    /** Every launchable app, including hidden ones (needed by the hidden manager). */
    val allApps: StateFlow<List<LaunchableApp>> = repository.apps

    val apps: StateFlow<List<LaunchableApp>> =
        combine(repository.apps, settingsRepository.settings) { apps, settings ->
            apps.filterNot { it.key in settings.hidden }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<LaunchableApp>> =
        combine(repository.apps, settingsRepository.settings) { apps, settings ->
            val byKey = apps.associateBy { it.key }
            settings.favorites.mapNotNull { byKey[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _homeRequests = MutableStateFlow(0)
    val homeRequests: StateFlow<Int> = _homeRequests.asStateFlow()

    init {
        repository.start()
        refreshNotificationAccess()
        viewModelScope.launch {
            repository.apps.collect {
                val selected = settings.value.iconPack
                if (IconPackSelection.isExternal(selected)) {
                    val installed = withContext(Dispatchers.IO) {
                        IconPackRepository.get(application).availableIconPacks()
                    }
                    if (installed.none { it.id == selected }) {
                        updateSettings { current ->
                            if (current.iconPack == selected) {
                                current.copy(iconPack = IconPackSelection.THEME)
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

    fun refreshNotificationAccess() {
        NotificationCenter.refreshAccess(getApplication())
    }

    fun requestHome() {
        _homeRequests.value += 1
    }

    fun refresh() = repository.refresh()

    fun launch(app: LaunchableApp) = repository.launch(app)

    fun shortcuts(app: LaunchableApp): List<ShortcutInfo> = repository.shortcuts(app)

    fun launchShortcut(app: LaunchableApp, shortcut: ShortcutInfo) =
        repository.launchShortcut(app, shortcut)

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) =
        settingsRepository.update(transform)

    fun setTheme(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun setStyle(style: LauncherStyle) = updateSettings { it.copy(style = style) }

    fun setLayout(layout: AppLayout) = updateSettings { it.copy(layout = layout) }

    fun setColumns(columns: Int) = updateSettings {
        it.copy(gridColumns = columns.coerceIn(SettingsRepository.MIN_COLUMNS, SettingsRepository.MAX_COLUMNS))
    }

    fun toggleFavorite(app: LaunchableApp) = settingsRepository.toggleFavorite(app.key)

    fun setHidden(app: LaunchableApp, hidden: Boolean) = settingsRepository.setHidden(app.key, hidden)

    fun selectableColumns(): IntRange =
        SettingsRepository.MIN_COLUMNS..SettingsRepository.MAX_COLUMNS

    fun exportBackup(): String = settingsRepository.exportJson()

    fun importBackup(raw: String): Boolean = settingsRepository.importJson(raw)

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
