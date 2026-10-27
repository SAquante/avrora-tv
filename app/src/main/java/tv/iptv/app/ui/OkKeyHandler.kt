package tv.iptv.app.ui

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private const val LONG_PRESS_MS = 550L

/**
 * Handles the OK button at activity level: a short press opens the focused item,
 * holding it toggles a favourite. Compose focus around lazy grids proved unreliable here.
 */
@Composable
fun OkKeyHandler(
    enabled: Boolean,
    focusedId: () -> String?,
    onShortPress: () -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val shortPress by rememberUpdatedState(onShortPress)
    val longPress by rememberUpdatedState(onLongPress)

    DisposableEffect(activity, enabled) {
        if (!enabled || activity == null) return@DisposableEffect onDispose { }
        var longPressed = false
        val handler: (Int, KeyEvent) -> Boolean = { code, event ->
            val isOk = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER
            if (!isOk || focusedId() == null) {
                false
            } else {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        val heldLongEnough = event.repeatCount > 0 &&
                            event.eventTime - event.downTime > LONG_PRESS_MS
                        val flaggedLongPress = event.flags and KeyEvent.FLAG_LONG_PRESS != 0
                        if (!longPressed && (heldLongEnough || flaggedLongPress)) {
                            longPressed = true
                            longPress.invoke()
                        }
                        true
                    }

                    KeyEvent.ACTION_UP -> {
                        if (!longPressed) shortPress.invoke()
                        longPressed = false
                        true
                    }

                    else -> false
                }
            }
        }
        activity.keyHandler = handler
        onDispose { if (activity.keyHandler === handler) activity.keyHandler = null }
    }
}
