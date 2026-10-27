package tv.iptv.app.ui

import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import tv.iptv.app.data.Cam

@Composable
fun CamPlayerScreen(cam: Cam, country: String) {
    val webView = remember { arrayOfNulls<WebView>(1) }

    DisposableEffect(cam.id) {
        onDispose {
            webView[0]?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webView[0] = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView[0] = this
                    setBackgroundColor(AndroidColor.BLACK)
                    keepScreenOn = true
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    // YouTube refuses the default "...; wv" WebView user agent
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 9; MiTV) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?,
                        ) {
                            android.util.Log.w("IPTV", "wv error ${request?.url}: ${error?.description}")
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            android.util.Log.i("IPTV", "wv page finished $url")
                            // YouTube may not autoplay inside WebView; nudge it
                            view?.evaluateJavascript(
                                "(function(){var v=document.querySelector('video');" +
                                    "if(v){v.muted=false;v.play&&v.play();}return v?'ok':'no-video';})()",
                            ) { value -> android.util.Log.i("IPTV", "wv video=$value") }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.i(
                                "IPTV",
                                "wv console ${message?.messageLevel()} ${message?.message()}",
                            )
                            return true
                        }
                    }
                    // the mobile watch page is the only YouTube surface that plays inside WebView here
                    loadUrl("https://m.youtube.com/watch?v=" + cam.video)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xC9000000), Color(0x00000000))),
                ),
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = cam.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$country · Назад — к камерам",
                    color = Palette.TextDim,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun embedHtml(videoId: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
          iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
        </style>
      </head>
      <body>
        <iframe
          src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1"
          allow="autoplay; encrypted-media; picture-in-picture"
          allowfullscreen></iframe>
      </body>
    </html>
""".trimIndent()
