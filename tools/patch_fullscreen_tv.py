import io

path = 'app/src/main/java/tv/iptv/app/MainActivity.kt'
text = io.open(path, encoding='utf-8').read()

old = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()
        setContent { IptvApp() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }
"""

new = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TVs have no status bar to hide, and the immersive flags black out the
        // surface on some boxes, so only phones get the fullscreen treatment.
        if (!isTelevision()) goFullscreen()
        setContent { IptvApp() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isTelevision()) goFullscreen()
    }

    private fun isTelevision(): Boolean =
        packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)
"""

if old not in text:
    print('pattern missing')
else:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('patched MainActivity')
