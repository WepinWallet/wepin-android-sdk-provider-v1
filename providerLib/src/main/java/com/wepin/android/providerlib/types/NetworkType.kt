package com.wepin.android.providerlib.types

object NetworkType {
    fun isEVMNetwork(identifier: String): Boolean {
        return identifier.startsWith("evm") ||
                identifier == "ethereum"
    }
}