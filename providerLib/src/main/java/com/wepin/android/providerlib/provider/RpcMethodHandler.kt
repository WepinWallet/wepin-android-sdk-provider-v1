package com.wepin.android.providerlib.provider

import com.wepin.android.core.network.JsonRpcClient
import com.wepin.android.core.types.wepin.JsonRpcUrl
import com.wepin.android.core.utils.Log
import java.util.concurrent.CompletableFuture

class RpcMethodHandler(private val rpc: JsonRpcUrl) {
    private val jsonRpcClient = JsonRpcClient(rpc)

    fun send(method: String, params: List<Any>?): CompletableFuture<Any> {
        Log.d("WepinRpcMethodHandler", "params: $params")
        return jsonRpcClient.call<Any>(method, params)
    }
}