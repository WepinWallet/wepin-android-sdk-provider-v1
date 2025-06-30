package com.wepin.android.providerlib.types

data class WepinResponseMessage(
    val header: WepinResponseMessageHeader,
    val body: WepinResponseMessageBody
)

data class WepinResponseMessageHeader(
    val response_from: String = "web",
    val response_to: String = "wepin_widget",
    val id: Long
)

data class WepinResponseMessageBody(
    val command: String,
    val state: String,
    val data: Any? = null
)