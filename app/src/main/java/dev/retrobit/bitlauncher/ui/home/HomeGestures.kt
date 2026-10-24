package dev.retrobit.bitlauncher.ui.home

import android.content.Context
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import dev.retrobit.bitlauncher.data.GestureAction
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.util.SystemIntents
import kotlin.math.abs

/** Stable executor shared by both home styles; callbacks always refer to the latest composition. */
@Composable
fun rememberHomeGestureHandler(
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHomeMenu: () -> Unit,
    onOpenNotes: () -> Unit,
    context: Context = LocalContext.current,
): (GestureAction) -> Unit {
    val currentDrawer by rememberUpdatedState(onOpenDrawer)
    val currentSearch by rememberUpdatedState(onOpenSearch)
    val currentSettings by rememberUpdatedState(onOpenSettings)
    val currentHomeMenu by rememberUpdatedState(onOpenHomeMenu)
    val currentNotes by rememberUpdatedState(onOpenNotes)
    return remember(context) {
        { action: GestureAction ->
            when (action) {
                GestureAction.NONE -> Unit
                GestureAction.OPEN_DRAWER -> currentDrawer()
                GestureAction.OPEN_SEARCH -> currentSearch()
                GestureAction.OPEN_SETTINGS -> currentSettings()
                GestureAction.OPEN_HOME_MENU -> currentHomeMenu()
                GestureAction.OPEN_NOTIFICATIONS -> SystemIntents.expandNotifications(context)
                GestureAction.OPEN_QUICK_SETTINGS -> SystemIntents.openQuickSettings(context)
                GestureAction.OPEN_RECENTS -> SystemIntents.openRecents(context)
                GestureAction.LOCK_SCREEN -> SystemIntents.lockScreen(context)
                GestureAction.TAKE_SCREENSHOT -> SystemIntents.takeScreenshot(context)
                GestureAction.OPEN_CALENDAR -> SystemIntents.openCalendar(context)
                GestureAction.OPEN_NOTES -> currentNotes()
            }
        }
    }
}

@Composable
fun Modifier.homeHorizontalGestures(
    leftAction: GestureAction,
    rightAction: GestureAction,
    onAction: (GestureAction) -> Unit,
): Modifier {
    val currentOnAction by rememberUpdatedState(onAction)
    return pointerInput(leftAction, rightAction) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onDragEnd = {
                when {
                    distance < -70f -> currentOnAction(leftAction)
                    distance > 70f -> currentOnAction(rightAction)
                }
            },
            onHorizontalDrag = { _, delta -> distance += delta },
        )
    }
}

/** Detects a two-finger vertical swipe without taking over ordinary one-finger scrolling. */
@Composable
fun Modifier.homeTwoFingerGestures(
    upAction: GestureAction,
    downAction: GestureAction,
    onAction: (GestureAction) -> Unit,
): Modifier {
    val currentOnAction by rememberUpdatedState(onAction)
    return pointerInput(upAction, downAction) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var tracking = false
            var triggered = false
            var startY = 0f
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size >= 2) {
                    val centerY = pressed.map { it.position.y }.average().toFloat()
                    if (!tracking) {
                        tracking = true
                        startY = centerY
                    }
                    val distance = centerY - startY
                    if (!triggered && abs(distance) > 80f) {
                        val action = if (distance < 0f) upAction else downAction
                        if (action != GestureAction.NONE) {
                            triggered = true
                            currentOnAction(action)
                        }
                    }
                    if (triggered) pressed.forEach { it.consume() }
                }
            }
        }
    }
}

private val COMMON_NOTES_PACKAGES = setOf(
    "com.google.android.keep",
    "com.samsung.android.app.notes",
    "com.miui.notes",
    "com.oppo.notes",
    "com.coloros.note",
    "com.vivo.notes",
    "com.evernote",
    "com.notion.id",
)

fun findNotesApp(apps: List<LaunchableApp>, selectedPackage: String): LaunchableApp? =
    apps.firstOrNull { it.packageName == selectedPackage }
        ?: apps.firstOrNull { it.packageName in COMMON_NOTES_PACKAGES }
        ?: apps.firstOrNull { app ->
            val label = app.label.lowercase()
            "catatan" in label || "notes" in label || "keep" in label
        }
