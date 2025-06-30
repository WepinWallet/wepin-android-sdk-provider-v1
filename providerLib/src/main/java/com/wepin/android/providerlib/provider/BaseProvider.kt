package com.wepin.android.providerlib.provider

import java.util.concurrent.CompletableFuture

interface BaseProvider {
    // Common methods for all networks
    fun request(method: String, params: List<Any>? = listOf()): CompletableFuture<Any>
    fun switchChain(chainId: String): CompletableFuture<Any>
    fun switchNetwork(network: String): CompletableFuture<Any>

    // Generic RPC methods
    fun send(method: String, params: List<Any>): CompletableFuture<Any>

    fun getNetwork(): String
    fun getNetworkFamily(): String
}

// printEVM specific methods
interface EVMProviderInterface : BaseProvider {
    fun requestAccounts(): CompletableFuture<Any>

    fun sendTransaction(transaction: Map<String, Any>): CompletableFuture<Any>
    fun signTransaction(transaction: Map<String, Any>): CompletableFuture<Any>
    fun sign(data: String, address: String): CompletableFuture<Any>
    fun signTypedDataV1(
        data: List<Map<String, Any>>,
        address: String
    ): CompletableFuture<Any>

    fun signTypedDataV3(
        data: Map<String, Any>,
        address: String
    ): CompletableFuture<Any>

    fun signTypedDataV4(
        data: Map<String, Any>,
        address: String
    ): CompletableFuture<Any>
}

interface KaiaProviderInterface : EVMProviderInterface
