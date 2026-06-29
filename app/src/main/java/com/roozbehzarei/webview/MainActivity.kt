package com.roozbehzarei.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.roozbehzarei.webview.databinding.ActivityMainBinding
import android.graphics.Color
import android.webkit.JavascriptInterface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
import com.onesignal.OneSignal
import android.app.DownloadManager
import android.os.Environment
import android.webkit.URLUtil


// The URL of the website to be loaded
private const val WEBSITE = "https://kppllive.in"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)   
        binding = ActivityMainBinding.inflate(layoutInflater)

        /**
         * Define and configure [webView]
         */
        webView = binding.webView

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->

            val request = DownloadManager.Request(Uri.parse(url))

            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)

            request.setTitle(
                URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
                )
            )

            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
                )
            )

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
        
        webView.webViewClient = MyWebViewClient()
        webView.webChromeClient = MyWebChromeClient()
        with(webView.settings) {
            // Enable JavaScript execution
            javaScriptEnabled = true
            // Enable DOM storage API
            domStorageEnabled = true
            // Disable support for zooming using webView's on-screen zoom controls and gestures
            setSupportZoom(false)
        }

        val cookieManager = CookieManager.getInstance()

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        CookieManager.getInstance().flush()

        webView.addJavascriptInterface(
        ThemeBridge(),
        "Android"
        )
        
        // If dark theme is turned on, automatically render all web contents using a dark theme
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
                }

                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
                }
            }
        }

        webView.settings.userAgentString =
    webView.settings.userAgentString + " KPPLAPP"
        // Start loading the given website URL
        webView.loadUrl(WEBSITE)

        // Define Swipe-to-refresh behavior
        binding.root.setOnRefreshListener {
            if (webView.url == null) {
                webView.loadUrl(WEBSITE)
            } else {
                webView.reload()
            }
        }

        // Theme Swipe-to-refresh layout
        val spinnerTypedValue = TypedValue()
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, spinnerTypedValue, true
        )
        val spinnerColor = spinnerTypedValue.resourceId
        binding.root.setColorSchemeResources(spinnerColor)

        val backgroundTypedValue = TypedValue()
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryContainer, backgroundTypedValue, true
        )
        val backgroundColor = backgroundTypedValue.resourceId
        binding.root.setProgressBackgroundColorSchemeResource(backgroundColor)

        /**
         * Disable Swipe-to-refresh if [webView] is scrolling
         */
        webView.viewTreeObserver.addOnScrollChangedListener {
            binding.root.isEnabled = webView.scrollY == 0
        }

        /**
         * When navigating back, close the app if there's no previous webpage for [webView] to go back to
         */
        val mCallback = onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
        mCallback.isEnabled = true

        setContentView(binding.root)
        intent?.data?.let { uri ->
            if (uri.scheme == "kppl" && uri.host == "login-success") {

                val email = uri.getQueryParameter("email")

                if (email != null) {
                    webView.loadUrl(
                        "https://kppllive.in/app-login.php?email=$email"
                    )
                }
            }
        }
        
    }

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    setIntent(intent) // important

    intent.data?.let { uri ->
        if (uri.scheme == "kppl" && uri.host == "login-success") {

            val email = uri.getQueryParameter("email")

            if (email != null) {
                webView.loadUrl(
                    "https://kppllive.in/app-login.php?email=$email"
                )
            }
        }

        if (uri.scheme == "kppl" && uri.host == "open") {

            val url = uri.getQueryParameter("url")

            if (!url.isNullOrEmpty()) {
                webView.loadUrl(url)
            }
        }
    }
}

    inner class ThemeBridge {

        @JavascriptInterface
        fun setUser(username: String) {
            OneSignal.login(username)
            OneSignal.User.addTag("logged_in", "true")
        }

        @JavascriptInterface
        fun logoutUser() {
            OneSignal.User.removeTag("logged_in")
            OneSignal.logout()
        }

        @JavascriptInterface
        fun setThemeColor(color: String) {
            runOnUiThread {
                try {
                    window.statusBarColor = Color.parseColor(color)
                    window.navigationBarColor = Color.parseColor(color)

                    val controller = WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    )

                    controller.isAppearanceLightStatusBars = false
                    controller.isAppearanceLightNavigationBars = false

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

private inner class MyWebViewClient : WebViewClient() {

override fun shouldOverrideUrlLoading(
    view: WebView?,
    request: WebResourceRequest?
): Boolean {

    val url = request?.url.toString()

    // Open Google Login in Chrome
    if (url.contains("glogin.php")) {

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(this@MainActivity, Uri.parse(url))
        return true
    }

    if (url.contains("pdf.php")) {
        return false
    }
    // Keep kppllive.in inside WebView
    if (url.startsWith(WEBSITE)) {
        return false
    }

    // Open all other links externally
    val customTabsIntent = CustomTabsIntent.Builder().build()
    customTabsIntent.launchUrl(this@MainActivity, Uri.parse(url))
    return true
}

    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?
    ) {
        super.onPageStarted(view, url, favicon)
        binding.webView.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
        binding.progressIndicator.visibility = View.VISIBLE
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?
    ) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()
        binding.root.isRefreshing = false
        binding.progressIndicator.visibility = View.INVISIBLE
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (request?.isForMainFrame != true) {
            return
        }

        super.onReceivedError(view, request, error)

        binding.webView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.root.isEnabled = false
    }
}

    private inner class MyWebChromeClient : WebChromeClient() {

        // Update the progress of progress indicator when loading a webpage
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressIndicator.progress = newProgress
        }
    }

}
