package com.wepin.android.providerlib.provider

import com.wepin.android.providerlib.WepinProvider
import com.wepin.android.providerlib.utils.failedFuture
import java.util.concurrent.CompletableFuture

/**
 * MiddlewareProvider - 요청을 실제 Provider로 라우팅하는 중계자
 * 사용자는 이 객체를 BaseProvider로 받아서 사용
 * 체인 변경 시에도 동일한 객체로 새로운 네트워크 기능 사용 가능
 */
internal class MiddlewareProvider(
    private val wepinProvider: WepinProvider,
    private val targetNetwork: String
) : BaseProvider {

    private val TAG = this.javaClass.name

    /**
     * 현재 활성화된 실제 Provider 가져오기
     */
    private fun getCurrentRealProvider(): BaseProvider? {
        return wepinProvider.getCurrentActiveRealProvider()
            ?: wepinProvider.getRealProviderForNetwork(targetNetwork)
    }

    // BaseProvider 인터페이스 구현 - 모든 요청을 실제 Provider로 위임

    override fun request(method: String, params: List<Any>?): CompletableFuture<Any> {
        return getCurrentRealProvider()?.request(method, params)
            ?: failedFuture(Exception("No active provider for $method"))
    }

    override fun switchChain(chainId: String): CompletableFuture<Any> {
        return getCurrentRealProvider()?.switchChain(chainId)
            ?: failedFuture(Exception("No active provider"))
    }

    override fun switchNetwork(network: String): CompletableFuture<Any> {
        return getCurrentRealProvider()?.switchNetwork(network)
            ?: failedFuture(Exception("No active provider"))
    }

    override fun send(method: String, params: List<Any>): CompletableFuture<Any> {
        return getCurrentRealProvider()?.send(method, params)
            ?: failedFuture(Exception("No active provider"))
    }

    override fun getNetwork(): String {
        return getCurrentRealProvider()?.getNetwork() ?: targetNetwork
    }

    override fun getNetworkFamily(): String {
        return getCurrentRealProvider()?.getNetworkFamily() ?: "unknown"
    }
}