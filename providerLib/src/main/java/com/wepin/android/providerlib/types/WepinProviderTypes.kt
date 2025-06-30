package com.wepin.android.providerlib.types

data class RequestEnableParams(
    val network: String
)

data class SignParams(
    val account: ProviderAccount,
    val data: String
)

data class SignTypedDataParams(
    val account: ProviderAccount,
    val data: String,
    val version: String
)

data class SignTransactionParams(
    val account: ProviderAccount,
    val from: String,
    val to: String? = null,
    val gas: String? = null,
    val gasPrice: String? = null,
    val value: String? = null,
    val data: String? = null,
    val nonce: String? = null
)

data class StandardTransactionParams(
    val from: String,
    val to: String? = null,
    val gas: String? = null,
    val gasPrice: String? = null,
    val value: String? = null,
    val data: String? = null,
    val nonce: String? = null
)

fun StandardTransactionParams.toSignTransactionParams(account: ProviderAccount): SignTransactionParams {
    return SignTransactionParams(
        account = account,
        from = this.from,
        to = this.to,
        gas = this.gas,
        gasPrice = this.gasPrice,
        value = this.value,
        data = this.data
    )
}

data class ProviderAccount(
    val address: String? = null,
    val network: String
)

data class SwitchEthChainParams(
    val account: ProviderAccount,
    val chainId: String
)