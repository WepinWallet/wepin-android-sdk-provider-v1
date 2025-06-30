package com.wepin.android.providerlib.utils

import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.providerlib.types.StandardTransactionParams

fun parseStandardTransactionParams(map: Map<String, Any>): StandardTransactionParams {
    return StandardTransactionParams(
        from = map.getString("from")!!,
        to = map.getString("to", required = false),
        gas = map.getString("gas", required = false),
        gasPrice = map.getString("gasPrice", required = false),
        value = map.getString("value", required = false),
        data = map.getString("data", required = false),
        nonce = map.getString("nonce", required = false)
    )
}

fun Map<String, Any>.getString(key: String, required: Boolean = true): String? {
    val value = this[key]
        ?: return if (required) throw WepinError.INVALID_PARAMETER else null
    return when (value) {
        is String -> value
        is Number -> value.toString()
        else -> if (required) throw WepinError.INVALID_PARAMETER else null
    }
}