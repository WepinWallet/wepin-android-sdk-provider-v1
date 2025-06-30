package com.wepin.android.providerlib.provider

interface ProviderResolver {
    fun getCurrentActiveRealProvider(): BaseProvider?
    fun getRealProviderForNetwork(network: String): BaseProvider?
    fun getCurrentNetworkId(): String?
}