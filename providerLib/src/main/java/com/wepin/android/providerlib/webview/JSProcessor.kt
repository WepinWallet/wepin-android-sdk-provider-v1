package com.wepin.android.providerlib.webview

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.JSResponse
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.utils.convertJsonToLocalStorageData
import com.wepin.android.providerlib.manager.WepinProviderManager
import org.json.JSONObject

interface Command {
    companion object {
        /**
         * Commands for JS processor
         */
        const val CMD_READY_TO_WIDGET: String = "ready_to_widget"

        //        const val CMD_GET_SDK_REQUEST: String = "get_sdk_request"
        const val CMD_CLOSE_WEPIN_WIDGET: String = "close_wepin_widget"
        const val CMD_SET_LOCAL_STORAGE: String = "set_local_storage"

        /**
         * Commands for Provider
         */
        const val CMD_DEQUEUE_REQUEST: String = "dequeue_request"
        const val CMD_REQUEST_INFO: String = "request_info"

        const val CMD_REQUEST_ENABLE: String = "request_enable"
        const val CMD_SIGN_TRANSACTION: String = "sign_transaction"
        const val CMD_SEND_TRANSACTION: String = "send_transaction"
        const val CMD_SIGN: String = "sign"
        const val CMD_SIGN_TYPED_DATA: String = "sign_typed_data"
        const val CMD_WALLET_SWITCH_ETHEREUM_CHAIN: String = "wallet_switchEthereumChain"
        const val CMD_SIGN_ALL_TRANSACTIONS: String = "sign_all_transactions"
    }
}

object JSProcessor {
        private val TAG = this.javaClass.name

    fun processRequest(request: String, callback: (response: String) -> Any) {
//        Log.d(TAG, "processRequest : $request")
        try {
            val objectMapper = ObjectMapper()
            // 메시지를 JSONObject로 변환
            val jsonObject = JSONObject(request)
            val headerObject = jsonObject.getJSONObject("header")
            // "body" 객체를 가져옴
            val bodyObject = jsonObject.getJSONObject("body")

            // "command" 값을 가져옴

            val command = bodyObject.getString("command")
            var jsResponse: JSResponse? = null

            when (command) {
                Command.CMD_READY_TO_WIDGET -> {
                    Log.d(TAG, "CMD_READY_TO_WIDGET")
                    val appKey = WepinProviderManager.getInstance().appKey
                    val appId = WepinProviderManager.getInstance().appId
                    val domain = WepinProviderManager.getInstance().packageName
                    val platform = 2  // android sdk platform number
                    val type = WepinProviderManager.getInstance().sdkType
                    val version = WepinProviderManager.getInstance().version
                    val attributes = WepinProviderManager.getInstance().wepinAttributes
                    var storageData = WepinStorageManager.getAllStorage()
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        headerObject.getString("request_from"),
                        command
                    )
                        .setReadyToWidgetData(
                            appKey = appKey!!,
                            appId = appId!!,
                            domain = domain!!,
                            platform = platform,
                            type = type,
                            version = version,
                            localData = storageData ?: {},
                            attributes = attributes!!
                        ).build()
                }

                Command.CMD_DEQUEUE_REQUEST -> {
                    Log.d(TAG, "CMD_DEQUEUE_REQUEST")

                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        headerObject.getString("request_from"),
                        command
                    )
                        .build()
                    jsResponse.body.data =
                        WepinProviderManager.getInstance().getWepinRequest() ?: "No request"

                }

                Command.CMD_SET_LOCAL_STORAGE -> {
                    Log.d(TAG, "CMD_SET_LOCAL_STORAGE")
                    try {
                        val data = bodyObject.getJSONObject("parameter").getJSONObject("data")

                        val storageDataMap = mutableMapOf<String, Any>()

                        data.keys().forEach { key ->
                            val storageValue = when (val value = data.get(key)) {
                                is JSONObject -> {
                                    val jsonString = value.toString()
                                    convertJsonToLocalStorageData(jsonString)
                                }
                                //is String -> StorageDataType.StringValue(value)
                                is String -> value
                                is Boolean -> value
                                else -> value //throw IllegalArgumentException("Unsupported data type for key: $key")
                            }
                            storageDataMap[key] = storageValue
                        }

                        WepinStorageManager.setAllStorage(storageDataMap)
                        jsResponse = JSResponse.Builder(
                            headerObject.getString("id"),
                            headerObject.getString("request_from"),
                            command
                        ).build()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing JSON data: ${e.message}")
                        throw WepinError.generalUnKnownEx(e.message)
                    }
                }

                Command.CMD_REQUEST_INFO -> {
                    Log.d(TAG, "CMD_DEAQUE_REQUEST")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        "wepin_widget",
                        command
                    ).build()
                    WepinProviderManager.getInstance().getCurrentWepinRequest()
                    WepinProviderManager.getInstance().getResponseDeferred()!!.complete(request)
                }

                Command.CMD_REQUEST_ENABLE,
                Command.CMD_SIGN_TRANSACTION,
                Command.CMD_SEND_TRANSACTION,
                Command.CMD_SIGN,
                Command.CMD_SIGN_TYPED_DATA,
                Command.CMD_WALLET_SWITCH_ETHEREUM_CHAIN,
                Command.CMD_SIGN_ALL_TRANSACTIONS -> {
                    Log.d(TAG, "$command")
                    jsResponse = JSResponse.Builder(
                        headerObject.getString("id"),
                        "wepin_widget",
                        command
                    ).build()
                    val requestId = headerObject.getString("id")
                    WepinProviderManager.getInstance().wepinWebViewManager?.handleResponse(
                        requestId,
                        jsonObject
                    )
                }

                Command.CMD_CLOSE_WEPIN_WIDGET -> {
                    Log.d(TAG, "CMD_CLOSE_WEPIN_WIDGET")
                    jsResponse = null
                    WepinProviderManager.getInstance().closeWebview()
                }
            }
            if (jsResponse == null) {
                Log.d(TAG, "JSProcessor Response is null")
                return
            }

            val response = objectMapper.writeValueAsString(jsResponse)
//            Log.d(TAG, "JSProcessor Response : $response")

            // JSInterface의 onResponse 메서드를 통해 JavaScript로 응답 전송
            callback(response)


        } catch (e: Exception) {
            e.printStackTrace()
            throw WepinError.generalUnKnownEx(e.message)
        }
    }
}