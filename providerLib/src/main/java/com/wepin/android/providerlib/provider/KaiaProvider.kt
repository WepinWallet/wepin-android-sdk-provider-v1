package com.wepin.android.providerlib.provider

import EVMError
import android.content.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.types.storage.StorageDataType
import com.wepin.android.core.types.wepin.JsonRpcUrl
import com.wepin.android.core.utils.Log
import com.wepin.android.providerlib.info.ProviderNetworkInfo
import com.wepin.android.providerlib.types.ProviderAccount
import com.wepin.android.providerlib.types.SignParams
import com.wepin.android.providerlib.types.SignTypedDataParams
import com.wepin.android.providerlib.types.toSignTransactionParams
import com.wepin.android.providerlib.utils.failedFuture
import com.wepin.android.providerlib.utils.getSelectedAddress
import com.wepin.android.providerlib.utils.parseStandardTransactionParams
import com.wepin.android.providerlib.utils.setSelectedAddress
import org.json.JSONArray
import java.util.concurrent.CompletableFuture

class KaiaProvider(
    private val rpc: JsonRpcUrl,
    private val network: String,
    private val context: Context
) : KaiaProviderInterface {
    private val TAG = this.javaClass.name
    private val webViewMethodHandler = WebViewMethodHandler(context)
    private val rpcMethodHandler = RpcMethodHandler(rpc)
    private var _selectedAccount: ProviderAccount? = null
    private var _currentNetwork: String = network

    // 네트워크 변경 콜백
    private var networkChangeCallback: ((String, String) -> Unit)? = null

    /**
     * 네트워크 변경 콜백 설정
     */
    fun setNetworkChangeCallback(callback: (currentNetwork: String, newNetwork: String) -> Unit) {
        this.networkChangeCallback = callback

        // WebViewMethodHandler에도 콜백 설정
        webViewMethodHandler.setNetworkChangeCallback { currentNet, newNet ->
            networkChangeCallback?.invoke(currentNet, newNet)
        }
    }

    internal fun setSelectedAccount(account: ProviderAccount) {
        _selectedAccount = account
    }

    override fun getNetwork(): String {
        return _currentNetwork
    }

    override fun getNetworkFamily(): String {
        return "kaia"
    }

    override fun requestAccounts(): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)

        val selectedAddress = getSelectedAddress(_currentNetwork)
        if (selectedAddress != null) {
            return CompletableFuture.completedFuture(mutableListOf(selectedAddress.address))
        }
        return webViewMethodHandler.requestAccounts(_currentNetwork).thenApply { result ->
            try {
                // result가 JSONArray 문자열이므로 JSONArray로 파싱
                val jsonArray = JSONArray(result.toString())
                val addresses = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    addresses.add(jsonArray.getString(i))
                }

                if (addresses.isNotEmpty()) {
                    val firstAddress = addresses.first().lowercase()

                    _selectedAccount = ProviderAccount(
                        address = firstAddress,
                        network = _currentNetwork,
                    )
                    setSelectedAddress(network = _currentNetwork, address = firstAddress)
                    mutableListOf(firstAddress)
                } else {
                    addresses
                }
            } catch (e: Exception) {
                throw Exception("Failed to parse addresses: ${e.message}")
            }
        }
    }

    override fun switchChain(chainId: String): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
//        return webViewMethodHandler.switchChain(network, chainId)
        return webViewMethodHandler.switchChain(_currentNetwork, chainId).thenApply { result ->
            result
        }
    }

    override fun switchNetwork(network: String): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        var chainId = ProviderNetworkInfo.findNetworkInfoById(network)?.chainId
            ?: return failedFuture(EVMError.InvalidParams)
        if (chainId !is String) {
            chainId = chainId.toString()
        }
        return webViewMethodHandler.switchChain(network, chainId)
    }

    override fun sendTransaction(transaction: Map<String, Any>): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val future = CompletableFuture<Any>()
        val account = _selectedAccount ?: return failedFuture(EVMError.Unauthorized)

        // 현재 네트워크와 계정의 네트워크가 다른 경우
        if (account.network != _currentNetwork) {
            future.completeExceptionally(EVMError.ChainDisconnected)
            return future
        }

        if (!isValidEvmParams(transaction)) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        val params = try {
            parseStandardTransactionParams(transaction).toSignTransactionParams(account)
        } catch (e: Exception) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        webViewMethodHandler.sendTransaction(params)
            .thenApply { result ->
                future.complete(result)
            }
            .exceptionally { error ->
                when (error) {
                    is EVMError -> future.completeExceptionally(error)
                    else -> future.completeExceptionally(error)
                }
                null
            }

        return future
    }

    override fun signTransaction(transaction: Map<String, Any>): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val future = CompletableFuture<Any>()
        val account = _selectedAccount ?: return failedFuture(EVMError.Unauthorized)

        // 현재 네트워크와 계정의 네트워크가 다른 경우
        if (account.network != _currentNetwork) {
            future.completeExceptionally(EVMError.ChainDisconnected)
            return future
        }

        if (!isValidEvmParams(transaction)) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        val params = try {
            parseStandardTransactionParams(transaction).toSignTransactionParams(account)
        } catch (e: Exception) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        webViewMethodHandler.signTransaction(params)
            .thenApply { result ->
                future.complete(result)
            }
            .exceptionally { error ->
                when (error) {
                    is EVMError -> future.completeExceptionally(error)
                    else -> future.completeExceptionally(error)
                }
                null
            }

        return future
    }

    override fun sign(data: String, address: String): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val future = CompletableFuture<Any>()
        val account = _selectedAccount ?: return failedFuture(EVMError.Unauthorized)

        if (account.address?.lowercase() != address.lowercase()) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        // 현재 네트워크와 계정의 네트워크가 다른 경우
        if (account.network != _currentNetwork) {
            future.completeExceptionally(EVMError.ChainDisconnected)
            return future
        }

        val params = try {
            SignParams(
                account = account,
                data = data
            )
        } catch (e: Exception) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        webViewMethodHandler.sign(params)
            .thenApply { result ->
                future.complete(result)
            }
            .exceptionally { error ->
                when (error) {
                    is EVMError -> future.completeExceptionally(error)
                    else -> future.completeExceptionally(error)
                }
                null
            }

        return future
    }

    override fun signTypedDataV1(
        data: List<Map<String, Any>>,
        address: String,
    ): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val objectMapper = jacksonObjectMapper()
        val jsonString = objectMapper.writeValueAsString(data)
        return signTypedData(jsonString, address, "V1")
    }

    override fun signTypedDataV3(
        data: Map<String, Any>,
        address: String
    ): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val objectMapper = jacksonObjectMapper()
        val jsonString = objectMapper.writeValueAsString(data)
        return signTypedData(jsonString, address, "V3")
    }

    override fun signTypedDataV4(
        data: Map<String, Any>,
        address: String
    ): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val objectMapper = jacksonObjectMapper()
        val jsonString = objectMapper.writeValueAsString(data)
        return signTypedData(jsonString, address, "V4")
    }

    private fun signTypedData(
        data: String,
        address: String,
        version: String
    ): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        val future = CompletableFuture<Any>()
        val account = _selectedAccount ?: return failedFuture(EVMError.Unauthorized)

        if (account.address?.lowercase() != address.lowercase()) {
            return failedFuture(EVMError.Unauthorized)
        }

        // 현재 네트워크와 계정의 네트워크가 다른 경우
        if (account.network != _currentNetwork) {
            return failedFuture(EVMError.ChainDisconnected)
        }

        val params = try {
            SignTypedDataParams(
                account = account,
                data = data,
                version = version
            )
        } catch (e: Exception) {
            future.completeExceptionally(EVMError.InvalidParams)
            return future
        }

        webViewMethodHandler.signTypedData(params)
            .thenApply { result ->
                future.complete(result)
            }
            .exceptionally { error ->
                when (error) {
                    is EVMError -> future.completeExceptionally(error)
                    else -> future.completeExceptionally(error)
                }
                null
            }

        return future
    }

    override fun send(method: String, params: List<Any>): CompletableFuture<Any> {
        return rpcMethodHandler.send(method, params)
    }

    override fun request(method: String, params: List<Any>?): CompletableFuture<Any> {
        val userInfo = WepinStorageManager.getStorage<StorageDataType.UserInfo>("user_info")
            ?: return failedFuture(WepinError.INVALID_LOGIN_SESSION)
        return when (method) {
            "eth_requestAccounts", "eth_accounts", "klay_accounts", "klay_requestAccounts", "kaia_accounts", "kaia_requestAccounts" -> {
                requestAccounts()
            }

            "eth_sendTransaction",
            "klay_sendTransaction",
            "kaia_sendTransaction" -> {
                if (params.isNullOrEmpty() || params.firstOrNull() !is Map<*, *>) {
                    return failedFuture(EVMError.InvalidParams)
                }
                sendTransaction(params.first() as Map<String, Any>)
            }

            "eth_signTransaction", "klay_signTransaction", "kaia_signTransaction" -> {
                if (params.isNullOrEmpty() || params.firstOrNull() !is Map<*, *>) {
                    return failedFuture(EVMError.InvalidParams)
                }
                signTransaction(params.first() as Map<String, Any>)
            }

            "eth_signTypedData_v1", "klay_signTypedData_v1" -> {
                if (params.isNullOrEmpty()) {
                    return failedFuture(EVMError.InvalidParams)
                }
                val address = params.getOrNull(0) as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                Log.d(TAG, "address: $address")
                val typedDataList = params.getOrNull(1) as? List<Map<String, Any>>
                    ?: return failedFuture(EVMError.InvalidParams)

                signTypedDataV1(typedDataList, address)
            }

            "eth_signTypedData_v3", "eth_signTypedData_v4", "klay_signTypedData_v3", "klay_signTypedData_v4" -> {
                Log.d(TAG, "typedData: $method")
                Log.d(TAG, "params: $params")
                if (params.isNullOrEmpty()) {
                    return failedFuture(EVMError.InvalidParams)
                }
                val address = params.getOrNull(0) as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                Log.d(TAG, "address: $address")
                val data =
                    params[1] as? Map<String, Any> ?: return failedFuture(EVMError.InvalidParams)
                Log.d(TAG, "data: $data")
                return when (method) {
                    "eth_signTypedData_v3", "klay_signTypedData_v3" -> signTypedDataV3(
                        data,
                        address
                    )

                    "eth_signTypedData_v4", "klay_signTypedData_v4" -> signTypedDataV4(
                        data,
                        address
                    )

                    else -> failedFuture(EVMError.InvalidInput)
                }
            }

            "eth_sign", "klay_sign", "kaia_sign" -> {
                if (params.isNullOrEmpty()) {
                    return failedFuture(EVMError.InvalidParams)
                }
                val data = params[1] as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                val address = params.getOrNull(0) as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                sign(data, address)
            }

            "personal_sign" -> {
                if (params.isNullOrEmpty()) {
                    return failedFuture(EVMError.InvalidParams)
                }
                val data = params[0] as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                val address = params.getOrNull(1) as? String
                    ?: return failedFuture(EVMError.InvalidParams)
                sign(data, address)
            }

            "wallet_switchEthereumChain" -> {
                if (params.isNullOrEmpty()) {
                    return failedFuture(EVMError.InvalidParams)
                }
                val paramMap = params.first() as Map<*, *>
                val chainId = paramMap["chainId"] as? String
                    ?: return failedFuture(EVMError.InvalidParams)

                switchChain(chainId)
            }

            else -> rpcMethodHandler.send(method, params)
        }
    }

    private fun isHexString(value: String?): Boolean {
        return value != null &&
                value.startsWith("0x") &&
                value.length > 2 &&
                value.substring(2).matches(Regex("^[0-9a-fA-F]+$"))
    }

    private fun isValidEvmParams(params: Map<String, Any?>): Boolean {
        for ((key, value) in params) {
            if (value is String && !isHexString(value)) {
                Log.e("ParamChecker", "$key = $value is not a valid hex string")
                return false
            }
        }
        return true
    }
}