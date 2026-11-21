package com.humanatlas.android.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewAssetLoader
import com.humanatlas.android.bridge.AnatomyJsBridge
import com.humanatlas.android.bridge.AnatomyWebController
import com.humanatlas.android.model.SelectedStructure
import java.io.ByteArrayInputStream

private const val ATLAS_DOMAIN = "appassets.androidplatform.net"
private const val ATLAS_ENTRY_URL = "https://$ATLAS_DOMAIN/assets/atlas/index.html"

/**
 * Custom WebView subclass that keeps multi-touch 3D orbit/pinch gestures smooth
 * by preventing parent Compose scroll/sheet containers from intercepting pointers.
 */
private class MultiTouchAtlasWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AtlasWebView(
    controller: AnatomyWebController,
    onStructureSelected: (SelectedStructure) -> Unit,
    onSelectionCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val webView = remember {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(ATLAS_DOMAIN)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(initialContext.applicationContext))
            .build()

        val bridge = AnatomyJsBridge(
            onSelected = onStructureSelected,
            onCleared = onSelectionCleared
        )

        MultiTouchAtlasWebView(initialContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F3F4F4"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // Offline-only: disable arbitrary file/content URI access
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
                // Let Three.js OrbitControls own pinch-to-zoom smoothly
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
            }

            addJavascriptInterface(bridge, AnatomyJsBridge.BRIDGE_NAME)

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    // Strictly serve local offline assets via WebViewAssetLoader; block any external host
                    if (request.url.host != ATLAS_DOMAIN) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            403,
                            "Offline Only",
                            emptyMap(),
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    // Keep navigation strictly locked inside the offline atlas origin
                    return request.url.host != ATLAS_DOMAIN
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    controller.markPageReady()
                }
            }

            controller.attach(this)
            loadUrl(ATLAS_ENTRY_URL)
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.detach(webView)
            webView.removeJavascriptInterface(AnatomyJsBridge.BRIDGE_NAME)
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.testTag("atlas_3d_webview")
    )
}
