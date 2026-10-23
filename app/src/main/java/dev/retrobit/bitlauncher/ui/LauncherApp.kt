package dev.retrobit.bitlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.ui.drawer.AppDrawerScreen
import dev.retrobit.bitlauncher.ui.home.HomeScreen
import dev.retrobit.bitlauncher.ui.settings.SettingsScreen

private enum class Screen { HOME, DRAWER, SETTINGS }

@Composable
fun LauncherApp(viewModel: LauncherViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var focusSearch by remember { mutableStateOf(false) }
    val homeSignal by viewModel.homeRequests.collectAsStateWithLifecycle()

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

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            viewModel = viewModel,
            onOpenDrawer = { withSearch ->
                focusSearch = withSearch
                screen = Screen.DRAWER
            },
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        AnimatedVisibility(
            visible = screen == Screen.DRAWER,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
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
