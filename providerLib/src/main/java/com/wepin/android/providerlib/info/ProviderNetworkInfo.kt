package com.wepin.android.providerlib.info

import com.wepin.android.core.types.wepin.NetworkInfo
import com.wepin.android.core.types.wepin.NetworkInfoResponse
import java.util.Locale

object ProviderNetworkInfo {
    private var _networkInfo: NetworkInfoResponse? = null

    internal fun setNetworkInfo(networkInfo: NetworkInfoResponse) {
        _networkInfo = networkInfo
    }

    internal fun getNetworkInfo(): NetworkInfoResponse? {
        return _networkInfo
    }

    internal fun findNetworkInfoByChainId(chainId: String): NetworkInfo? {
        val numChainId = try {
            when {
                chainId.startsWith("0x", ignoreCase = true) -> {
                    // hex string인 경우
                    chainId.substring(2).toInt(16)
                }

                else -> {
                    // 10진수 문자열인 경우
                    chainId.toInt()
                }
            }
        } catch (e: NumberFormatException) {
            return null // 변환 실패시 null 리턴
        }
        return _networkInfo?.networks?.find {
            it.chainId == numChainId.toString()
        }
    }

    internal fun findNetworkInfoById(id: String): NetworkInfo? {
        return _networkInfo?.networks?.find {
            it.id == id
        }
    }

    fun getNetworkFamilyByNetwork(network: String): String? {
        return if (network.lowercase(Locale.ROOT) == "ethereum" || network.lowercase(Locale.ROOT)
                .startsWith("evm")
        ) {
            "evm"
        } else if (network.lowercase(Locale.ROOT)
                .startsWith("klaytn") || network.lowercase(Locale.ROOT).startsWith("kaia")
        ) {
            "kaia"
        } else {
            null
        }
    }
}