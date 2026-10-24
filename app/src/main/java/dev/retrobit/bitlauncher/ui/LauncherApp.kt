package dev.retrobit.bitlauncher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.data.ThemeMode
import dev.retrobit.bitlauncher.ui.drawer.AppDrawerScreen
import dev.retrobit.bitlauncher.ui.drawer.FolioDrawerScreen
import dev.retrobit.bitlauncher.ui.home.FolioHomeScreen
import dev.retrobit.bitlauncher.ui.home.HomeScreen
import dev.retrobit.bitlauncher.ui.settings.SettingsScreen

private enum class Screen { HOME, DRAWER, SETTINGS }

@Composable
fun LauncherApp(viewModel: LauncherViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var focusSearch by remember { mutableStateOf(false) }
    val homeSignal by viewModel.homeRequests.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val folio = settings.style == LauncherStyle.FOLIO

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            screen = Screen.HOME
            focusSearch = false
        }
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }
    BackHandler(enabled = screen == Screen.HOME) {
        // Launchers stay on the home screen when the user presses back.
    }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.KALEIDO_NIGHT -> true
        ThemeMode.KALEIDO -> false
    }
    // The minimal home sits on the wallpaper and always needs light icons. Folio home is printed
    // paper, so its icons follow the theme.
    val lightSystemBarIcons = if (screen == Screen.HOME && !folio) true else !darkTheme
    val view = LocalView.current
    val activity = view.context.findActivity()
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightSystemBarIcons
            isAppearanceLightNavigationBars = !lightSystemBarIcons
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (folio) {
            FolioHomeScreen(
                viewModel = viewModel,
                onOpenDrawer = { withSearch ->
                    focusSearch = withSearch
                    screen = Screen.DRAWER
                },
                onOpenSettings = { screen = Screen.SETTINGS },
            )
        } else {
            HomeScreen(
                viewModel = viewModel,
                onOpenDrawer = { withSearch ->
                    focusSearch = withSearch
                    screen = Screen.DRAWER
                },
                onOpenSettings = { screen = Screen.SETTINGS },
            )
        }

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = if (folio) fadeIn() else slideInVertically(initialOffsetY = { it }),
            exit = if (folio) fadeOut() else slideOutVertically(targetOffsetY = { it }),
        ) {
            if (folio) {
                FolioDrawerScreen(
                    viewModel = viewModel,
                    focusSearch = focusSearch,
                    onClose = {
                        screen = Screen.HOME
                        focusSearch = false
                    },
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
            } else {
                AppDrawerScreen(
                    viewModel = viewModel,
                    focusSearch = focusSearch,
                    onClose = {
                        screen = Screen.HOME
                        focusSearch = false
                    },
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
            }
        }

        AnimatedVisibility(
            visible = screen == Screen.SETTINGS,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    screen = Screen.HOME
                    focusSearch = false
                },
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
