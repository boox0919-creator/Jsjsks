package com.humanatlas.android.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.humanatlas.android.model.SelectedStructure
import org.json.JSONObject

class AnatomyJsBridge(
    private val onSelected: (SelectedStructure) -> Unit,
    private val onCleared: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onStructureSelected(payloadJson: String) {
        val parsed = runCatching {
            val obj = JSONObject(payloadJson)
            val id = obj.optString("id").trim()
            val name = obj.optString("name").trim()
            val system = obj.optString("system").trim()
            if (id.isNotEmpty() && name.isNotEmpty()) {
                SelectedStructure(id = id, name = name, system = system)
            } else {
                null
            }
        }.getOrNull() ?: return

        mainHandler.post { onSelected(parsed) }
    }

    @JavascriptInterface
    fun onSelectionCleared() {
        mainHandler.post { onCleared() }
    }

    companion object {
        const val BRIDGE_NAME = "AnatomyBridge"
    }
}

class AnatomyWebController {
    private var webView: WebView? = null
    private var pendingFocusStructureId: String? = null
    private var isPageLoaded: Boolean = false

    fun attach(view: WebView) {
        webView = view
    }

    fun detach(view: WebView) {
        if (webView === view) {
            webView = null
        }
    }

    fun markPageReady() {
        isPageLoaded = true
        pendingFocusStructureId?.let { id ->
            pendingFocusStructureId = null
            focusStructure(id)
        }
    }

    fun focusStructure(structureId: String) {
        val view = webView
        if (view == null || !isPageLoaded) {
            pendingFocusStructureId = structureId
            return
        }
        val escapedId = JSONObject.quote(structureId)
        val script = "window.focusStructure && window.focusStructure($escapedId);"
        view.post {
            view.evaluateJavascript(script, null)
        }
    }

    fun resetCamera() {
        val view = webView ?: return
        view.post {
            view.evaluateJavascript("window.resetCamera && window.resetCamera();", null)
        }
    }
}
