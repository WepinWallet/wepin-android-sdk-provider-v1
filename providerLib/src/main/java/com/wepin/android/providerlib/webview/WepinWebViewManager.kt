package com.wepin.android.providerlib.webview

//import android.util.Log
import android.content.Context
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.core.utils.Log
import com.wepin.android.modal.WepinModal
import com.wepin.android.providerlib.types.WepinRequestMessage
import com.wepin.android.providerlib.webview.JSProcessor.processRequest
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

internal class WepinWebViewManager(platformType: String, widgetUrl: String) {
    private val TAG = this.javaClass.name
    private var _wepinModal: WepinModal = WepinModal(platformType)
    private var _widgetUrl: String = widgetUrl
    private var _currentWepinRequest: Map<String, Any?>? = null
    var _responseDeferred: CompletableDeferred<String>? = null

    //    var _responseWepinUserDeferred: CompletableDeferred<Boolean>? = null
    private var _requestQueue: MutableList<WepinRequestMessage> = mutableListOf()
    private val responseMap = mutableMapOf<String, CompletableFuture<Any>>()

    private var _isClosing: Boolean = false

    fun getResponseDeferred(): CompletableDeferred<String>? {
        return _responseDeferred
    }

    fun getCurrentWepinRequest(): Map<String, Any?>? {
        return _currentWepinRequest
    }

    fun enqueueRequest(request: WepinRequestMessage): CompletableFuture<Any> {
        val future = CompletableFuture<Any>()
        val requestId = request.header.id.toString()
        responseMap[requestId] = future
        _requestQueue.add(request)
        return future
    }

    fun handleResponse(requestId: String, response: JSONObject) {
        val future = responseMap[requestId] ?: return

        try {
            val bodyObject = response.getJSONObject("body")
            when (val state = bodyObject.getString("state")) {
                "SUCCESS" -> {
                    val data = bodyObject.get("data")
                    future.complete(data)
                }

                "ERROR" -> {
                    val errorMessage = bodyObject.optString("data", "Unknown error")
                    Log.e(TAG, "errorMassage: $errorMessage")
                    if (errorMessage.contains("User Cancel")) {
                        future.completeExceptionally(
                            WepinError(
                                WepinError.USER_CANCELED.code,
                                errorMessage
                            )
                        )
                    } else {
                        Log.d(TAG, "exception")
                        future.completeExceptionally(WepinError(errorMessage))
                    }
                }

                else -> future.completeExceptionally(WepinError("Unknown state: $state"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "erererer")
            future.completeExceptionally(e)
        }

        responseMap.remove(requestId)
    }

    fun dequeueRequest(): Map<String, Any?>? {
        return if (_requestQueue.isNotEmpty()) {
            _requestQueue.removeAt(0).toMap()
        } else {
            _isClosing = true
            null
        }
    }

    fun openWidget(context: Context) {
        _wepinModal.openModal(context, _widgetUrl, ::processRequest)
    }

    fun initQueue() {
        _requestQueue.clear()
        responseMap.clear()
    }

    // closeWidget() 메서드에 추가
    fun closeWidget() {
        _currentWepinRequest = null
        _wepinModal.closeModal()
        _isClosing = false
    }

    fun isClosing(): Boolean {
        return _isClosing
    }
}