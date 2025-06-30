package com.wepin.android.providerlib.types

data class RpcUrlType(
    val type: String,
    val url: String
)

data class NetworkInformation(
    val rpcUrl: Array<RpcUrlType>,
    val chainId: String
)