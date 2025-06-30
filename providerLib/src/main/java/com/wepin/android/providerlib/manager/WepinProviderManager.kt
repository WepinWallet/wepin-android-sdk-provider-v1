package com.wepin.android.providerlib.manager

import android.content.Context
import com.wepin.android.commonlib.WepinCommon
import com.wepin.android.core.WepinCoreManager
import com.wepin.android.core.network.WepinNetwork
import com.wepin.android.core.session.WepinSessionManager
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.providerlib.types.WepinProviderAttributes
import com.wepin.android.providerlib.types.WepinProviderParams
import com.wepin.android.providerlib.utils.getVersionMetaDataValue
import com.wepin.android.providerlib.webview.WepinWebViewManager
import java.util.concurrent.CompletableFuture

internal class WepinProviderManager {
    private val TAG = this.javaClass.name
    private var _appContext: Context? = null
    var appId: String? = null
    var appKey: String? = null
    var packageName: String? = null
    val version: String = getVersionMetaDataValue()
    lateinit var sdkType: String
    var wepinAttributes: WepinProviderAttributes? = null
    var wepinSessionManager: WepinSessionManager? = null
    var wepinNetwork: WepinNetwork? = null
    var wepinWebViewManager: WepinWebViewManager? = null
    var loginLib: WepinLogin? = null

    companion object {
        @Volatile
        private var instance: WepinProviderManager? = null

        fun getInstance(): WepinProviderManager =
            instance ?: synchronized(this) {
                instance ?: WepinProviderManager().also { instance = it }
            }

        fun clearInstance() {
            instance = null
        }
    }

    fun getResponseDeferred() = wepinWebViewManager?.getResponseDeferred()
    fun getCurrentWepinRequest() = wepinWebViewManager?.getCurrentWepinRequest()
    fun getWepinRequest() = wepinWebViewManager?.dequeueRequest()

    fun initialize(
        wepinPinParams: WepinProviderParams,
        attributes: WepinProviderAttributes?,
        platform: String = "android"
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        _appContext = wepinPinParams.context
        appId = wepinPinParams.appId
        appKey = wepinPinParams.appKey
        packageName = wepinPinParams.context.packageName
        sdkType = "$platform-provider"
        wepinAttributes = WepinProviderAttributes(
            defaultLanguage = attributes?.defaultLanguage,
            defaultCurrency = attributes?.defaultCurrency
        )

        WepinCoreManager.initialize(
            context = _appContext!!,
            appId = appId!!,
            appKey = appKey!!,
            platformType = platform,
            sdkType = sdkType
        )
            .thenApply {
                wepinNetwork = WepinCoreManager.getNetwork()
                wepinSessionManager = WepinCoreManager.getSession()

                val urlInfo = WepinCommon.getWepinSdkUrl(appKey!!)
                wepinWebViewManager =
                    WepinWebViewManager("$platform-$sdkType", urlInfo["wepinWebview"] ?: "")
                wepinWebViewManager?.initQueue()

                future.complete(true)
            }.exceptionally { throwable ->
                future.completeExceptionally(throwable)
                null
            }
        return future
    }

    fun setLogin(login: WepinLogin?) {
        loginLib = login
    }

    fun clear() {
        WepinCoreManager.clear()
        wepinWebViewManager?.initQueue()
        wepinNetwork = null
        wepinSessionManager = null
        wepinWebViewManager?.closeWidget()
        wepinWebViewManager = null
    }

    fun closeWebview() {
        wepinWebViewManager?.closeWidget()
    }
}