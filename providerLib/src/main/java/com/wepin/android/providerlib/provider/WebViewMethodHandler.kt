package com.wepin.android.providerlib.provider

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.core.utils.Log
import com.wepin.android.providerlib.info.ProviderNetworkInfo
import com.wepin.android.providerlib.manager.WepinProviderManager
import com.wepin.android.providerlib.types.ProviderAccount
import com.wepin.android.providerlib.types.RequestEnableParams
import com.wepin.android.providerlib.types.SignParams
import com.wepin.android.providerlib.types.SignTransactionParams
import com.wepin.android.providerlib.types.SignTypedDataParams
import com.wepin.android.providerlib.types.SwitchEthChainParams
import com.wepin.android.providerlib.types.WepinRequestMessage
import com.wepin.android.providerlib.types.WepinRequestMessageBody
import com.wepin.android.providerlib.types.WepinRequestMessageHeader
import com.wepin.android.providerlib.utils.failedFuture
import com.wepin.android.providerlib.utils.setSelectedAddress
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class WebViewMethodHandler(private val context: Context) {
    private val TAG = this.javaClass.name
    private val wepinProviderManager = WepinProviderManager.getInstance()

    // 네트워크 변경 콜백
    private var networkChangeCallback: ((String, String) -> Unit)? = null

    fun setNetworkChangeCallback(callback: (currentNetwork: String, newNetwork: String) -> Unit) {
        this.networkChangeCallback = callback
    }

    private fun enqueueAndOpen(request: WepinRequestMessage): CompletableFuture<Any> {
        val webViewManager = wepinProviderManager.wepinWebViewManager
            ?: return failedFuture(WepinError.NOT_INITIALIZED_ERROR)

        val enqueueFuture = webViewManager.enqueueRequest(request)

        if (webViewManager.isClosing()) {
            // 위젯이 닫히는 중이면 잠시 대기 후 위젯 열기
            Log.d(TAG, "Widget is closing, will retry opening after delay")
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Retrying to open widget")
                webViewManager.openWidget(context)
            }, 200) // 200ms 후 재시도
        } else {
            // 정상적으로 위젯 열기
            webViewManager.openWidget(context)
        }

        return enqueueFuture
    }

    fun requestAccounts(network: String): CompletableFuture<Any> {
        Log.d(TAG, "requestAccounts")
        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "request_enable",
                parameter = RequestEnableParams(network = network)
            )
        )

        return enqueueAndOpen(request)
    }

    fun sendTransaction(transaction: SignTransactionParams): CompletableFuture<Any> {
        Log.d(TAG, "sendTransaction")
        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "send_transaction",
                parameter = transaction
            )
        )

        return enqueueAndOpen(request)
    }

    fun signTransaction(transaction: SignTransactionParams): CompletableFuture<Any> {
        Log.d(TAG, "signTransaction")
        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "sign_transaction",
                parameter = transaction
            )
        )

        return enqueueAndOpen(request)
    }

    fun sign(parameter: SignParams): CompletableFuture<Any> {
        Log.d(TAG, "sign")
        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "sign",
                parameter = parameter
            )
        )

        return enqueueAndOpen(request)
    }

    fun signTypedData(
        parameter: SignTypedDataParams
    ): CompletableFuture<Any> {
        Log.d(TAG, "signTypedData")
        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "sign_typed_data",
                parameter = parameter
            )
        )

        return enqueueAndOpen(request)
    }

    fun switchChain(currentNetwork: String, chainId: String): CompletableFuture<Any> {
        Log.d(TAG, "switchChain - currentNetwork: $currentNetwork, chainId: $chainId")

        val request = WepinRequestMessage(
            header = WepinRequestMessageHeader(id = System.currentTimeMillis()),
            body = WepinRequestMessageBody(
                command = "wallet_switchEthereumChain",
                parameter = SwitchEthChainParams(
                    account = ProviderAccount(network = currentNetwork),
                    chainId = chainId
                )
            )
        )

        return enqueueAndOpen(request).thenApply { result ->
            Log.d(TAG, "switchChain completed successfully result: $result")

            // 체인 변경 성공 시 네트워크 변경 처리
            try {
                val accountInfo = when (result) {
                    is String -> {
                        val resultJson = JSONObject(result)
                        val address = resultJson.optString("address")
                        val network = resultJson.optString("network")
                        ProviderAccount(address = address, network = network)
                    }

                    is JSONObject -> {
                        val address = result.optString("address")
                        val network = result.optString("network")
                        ProviderAccount(address = address, network = network)
                    }

                    else -> {
                        Log.d(TAG, "unknown type")
                        null
                    }
                }

                if (accountInfo != null && !accountInfo.address.isNullOrEmpty() && accountInfo.network.isNotEmpty()) {
                    setSelectedAddress(
                        network = accountInfo.network,
                        address = accountInfo.address
                    )
                    handleNetworkChange(currentNetwork, accountInfo.network)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling network change: ${e.message}")
            }

            result
        }.exceptionally { error ->
            Log.e(TAG, "switchChain failed: ${error.message}")
            throw error
        }
    }

    /**
     * 네트워크 변경 처리 로직
     */
    private fun handleNetworkChange(currentNetwork: String, newNetwork: String) {
        Log.d(TAG, "handleNetworkChange - currentNetwork: $currentNetwork, network: $newNetwork")

        // 네트워크가 실제로 변경되었는지 확인
        if (currentNetwork == newNetwork) {
            Log.d(TAG, "Network not changed, staying on: $currentNetwork")
            return
        }

        // 네트워크 패밀리 변경 여부 확인
        val currentFamily = ProviderNetworkInfo.getNetworkFamilyByNetwork(currentNetwork)
        val newFamily = ProviderNetworkInfo.getNetworkFamilyByNetwork(newNetwork)

        Log.d(TAG, "Network family change: $currentFamily -> $newFamily")

        // 네트워크 패밀리가 변경되면 콜백 호출
        if (currentFamily != newFamily) {
            Log.d(TAG, "Network family changed - switching provider")
            networkChangeCallback?.invoke(currentNetwork, newNetwork)
        } else {
            Log.d(TAG, "Same network family - but different network, still switching")
            // 같은 패밀리라도 네트워크가 다르면 콜백 호출 (예: ethereum -> polygon)
            networkChangeCallback?.invoke(currentNetwork, newNetwork)
        }
    }
}