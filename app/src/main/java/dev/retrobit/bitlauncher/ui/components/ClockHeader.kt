package dev.retrobit.bitlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Clock display that refreshes exactly once per minute instead of every second: a launcher should
 * not wake the CPU 60 times a minute just to redraw digits that did not change.
 */
@Composable
fun rememberMinuteTick(): Calendar {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay((61L - now.get(Calendar.SECOND)) * 1000L)
        }
    }
    return now
}

@Composable
fun ClockHeader(
    use24Hour: Boolean,
    showDate: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit = {},
) {
    val now = rememberMinuteTick()
    val locale = Locale.getDefault()
    val timeFormatter = remember(use24Hour, locale) {
        SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm", locale)
    }
    val dateFormatter = remember(locale) {
        SimpleDateFormat("EEEE, d MMMM", locale)
    }
    val shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 10f)

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = timeFormatter.format(now.time),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                shadow = shadow,
            ),
            color = color,
        )
        if (showDate) {
            Text(
                text = dateFormatter.format(now.time),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    shadow = shadow,
                ),
                color = color.copy(alpha = 0.82f),
            )
        }
    }
}
