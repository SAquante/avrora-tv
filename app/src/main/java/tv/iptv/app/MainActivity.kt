package tv.iptv.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import tv.iptv.app.ui.IptvApp

class MainActivity : ComponentActivity() {

    /** Set by the player screen so it gets media keys even when Compose has no focus. */
    var keyHandler: ((Int, KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyHandler?.invoke(event.keyCode, event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IptvApp() }
    }
}
