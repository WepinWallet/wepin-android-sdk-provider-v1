package com.wepin.android.providerlib

import android.app.Activity
import android.content.Context
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.WepinAttribute
import com.wepin.android.core.utils.Log
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.providerlib.info.ProviderNetworkInfo
import com.wepin.android.providerlib.manager.WepinProviderManager
import com.wepin.android.providerlib.provider.BaseProvider
import com.wepin.android.providerlib.provider.EVMProvider
import com.wepin.android.providerlib.provider.KaiaProvider
import com.wepin.android.providerlib.provider.MiddlewareProvider
import com.wepin.android.providerlib.provider.ProviderResolver
import com.wepin.android.providerlib.types.ProviderAccount
import com.wepin.android.providerlib.types.WepinProviderAttributes
import com.wepin.android.providerlib.types.WepinProviderParams
import com.wepin.android.providerlib.utils.getSelectedAddress
import java.util.concurrent.CompletableFuture

class WepinProvider(wepinProviderParams: WepinProviderParams, platform: String? = "android") :
    ProviderResolver {
    private val TAG = this.javaClass.name
    private var _appContext: Context? = wepinProviderParams.context
    private var _appId: String = wepinProviderParams.appId
    private var _appKey: String = wepinProviderParams.appKey
    private var _isInitialized: Boolean = false
    private var _attributes: WepinAttribute? = null
    private var _wepinProviderManager: WepinProviderManager = WepinProviderManager.getInstance()

    // 실제 Provider들을 관리하는 Map
    private val realProviders = mutableMapOf<String, BaseProvider>()

    // 사용자에게 반환할 MiddlewareProvider들을 관리하는 Map
    private val middlewareProviders = mutableMapOf<String, MiddlewareProvider>()
    private var currentNetworkId: String? = null

    var login: WepinLogin? = null
        private set // 외부에서 변경 불가능하게 설정

    init {
        val wepinLoginOptions = WepinLoginOptions(
            context = _appContext!!,
            appId = wepinProviderParams.appId,
            appKey = wepinProviderParams.appKey,
        )
        login = WepinLogin(wepinLoginOptions)
    }

    fun initialize(attributes: WepinProviderAttributes? = null): CompletableFuture<Boolean> {
        val wepinCompletableFuture = CompletableFuture<Boolean>()

        if (_isInitialized) {
            wepinCompletableFuture.completeExceptionally(WepinError.ALREADY_INITIALIZED_ERROR)
            return wepinCompletableFuture
        }
        _attributes = attributes

        _wepinProviderManager.initialize(
            wepinPinParams = WepinProviderParams(
                _appContext!!,
                _appId,
                _appKey
            ),
            attributes = attributes
        ).thenCompose {
            _wepinProviderManager.wepinNetwork?.let { network ->
                val loginInitFuture = login?.init() ?: CompletableFuture.completedFuture(null)
                val networkInfoFuture = network.getNetworkInformation()

                CompletableFuture.allOf(loginInitFuture, networkInfoFuture)
                    .thenCompose {
                        login?.let { _wepinProviderManager.setLogin(it) }

                        networkInfoFuture.thenApply { networkInfo ->
                            ProviderNetworkInfo.setNetworkInfo(networkInfo)
                        }

                        val loginStatusFuture = if (login != null) {
                            val res =
                                _wepinProviderManager.wepinSessionManager?.checkLoginStatusAndGetLifeCycle()
                                    ?: CompletableFuture.completedFuture(true)
                            res
                        } else {
                            CompletableFuture.completedFuture(true)
                        }

                        loginStatusFuture.thenApply {
                            _isInitialized = true
                            wepinCompletableFuture.complete(true)
                            true
                        }
                    }
            }?.exceptionally { error ->
                _wepinProviderManager.clear()
                _isInitialized = false
                wepinCompletableFuture.completeExceptionally(error)
                null
            }
        }?.exceptionally { error ->
            _isInitialized = false
            wepinCompletableFuture.completeExceptionally(error)
        }

        return wepinCompletableFuture
    }

    fun isInitialized(): Boolean {
        return _isInitialized
    }

    fun getProvider(network: String): BaseProvider {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }

        if (_appContext == null || _appContext !is Activity) {
            throw WepinError.NOT_ACTIVITY
        }

        // 이미 생성된 MiddlewareProvider가 있으면 동일한 객체 반환
        middlewareProviders[network]?.let {
            Log.d(TAG, "Returning cached MiddlewareProvider for $network")
            return it
        }

        // 처음 요청시에만 실제 Provider와 MiddlewareProvider 생성
        ensureRealProviderExists(network)

        // 🔥 MiddlewareProvider 생성 시 this(ProviderResolver)로 전달
        val middlewareProvider = MiddlewareProvider(this, network)
        middlewareProviders[network] = middlewareProvider

        Log.d(TAG, "Created and cached MiddlewareProvider for $network")
        return middlewareProvider
    }

    fun finalize(): Boolean {
        _wepinProviderManager.clear()
        WepinProviderManager.clearInstance()
        login?.finalize()
        _isInitialized = false
        return true
    }

    private fun ensureRealProviderExists(network: String) {
        if (!realProviders.containsKey(network) || currentNetworkId != network) {
            val realProvider = createRealProviderForNetwork(network)
            setupNetworkChangeCallback(realProvider, network)

            realProviders[network] = realProvider
            currentNetworkId = network

            Log.d(TAG, "Created real provider: ${realProvider.javaClass.simpleName} for $network")
        }
    }

    /**
     * 실제 Provider 생성 (private)
     */
    private fun createRealProviderForNetwork(networkId: String): BaseProvider {
        val networkInfo = ProviderNetworkInfo.findNetworkInfoById(networkId)
            ?: throw WepinError(
                WepinError.NOT_SUPPORT_NETWORK.code,
                "Can not resolve network name: $networkId"
            )

        val networkFamily = ProviderNetworkInfo.getNetworkFamilyByNetwork(networkId)
        val rpc = networkInfo.rpcUrl.firstOrNull()
            ?: throw WepinError(
                WepinError.NOT_SUPPORT_NETWORK.code,
                "RPC URL not found for network: $networkId"
            )

        return when (networkFamily) {
            "evm" -> {
                EVMProvider(
                    rpc = rpc,
                    network = networkInfo.id,
                    context = _appContext!!
                )
            }

            "kaia" -> {
                KaiaProvider(
                    rpc = rpc,
                    network = networkInfo.id,
                    context = _appContext!!
                )
            }

            else -> throw WepinError(
                WepinError.NOT_SUPPORT_NETWORK.code,
                "Unsupported network family: $networkFamily"
            )
        }
    }

    /**
     * 네트워크 변경 콜백 설정
     */
    private fun setupNetworkChangeCallback(provider: BaseProvider, providerNetwork: String) {
        val callback: (String, String) -> Unit = { currentNet, newNet ->

            try {
                // 새로운 실제 Provider 생성
                val existingAddress = getSelectedAddress(network = newNet)?.address
                val newRealProvider = createRealProviderForNetwork(newNet)
                setupNetworkChangeCallback(newRealProvider, newNet)

                // Map 업데이트
                realProviders.clear()
                realProviders[newNet] = newRealProvider

                if (existingAddress != null) {
                    val account = ProviderAccount(
                        address = existingAddress,
                        network = newNet
                    )
                    when (newRealProvider) {
                        is EVMProvider -> newRealProvider.setSelectedAccount(account)
                        is KaiaProvider -> newRealProvider.setSelectedAccount(account)
                    }
                }
                currentNetworkId = newNet
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update real provider: ${e.message}")
            }
        }

        when (provider) {
            is EVMProvider -> provider.setNetworkChangeCallback(callback)
            is KaiaProvider -> provider.setNetworkChangeCallback(callback)
        }
    }

    // 🔥 ProviderResolver 인터페이스 구현
    override fun getCurrentActiveRealProvider(): BaseProvider? {
        return currentNetworkId?.let { realProviders[it] }
    }

    override fun getRealProviderForNetwork(network: String): BaseProvider? {
        ensureRealProviderExists(network)
        return realProviders[network]
    }

    override fun getCurrentNetworkId(): String? {
        return currentNetworkId
    }
}