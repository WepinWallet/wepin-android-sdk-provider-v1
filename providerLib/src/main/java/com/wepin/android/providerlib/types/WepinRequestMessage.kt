package com.wepin.android.providerlib.types

data class WepinRequestMessage(
    val header: WepinRequestMessageHeader,
    val body: WepinRequestMessageBody
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "header" to mapOf(
                "request_from" to header.request_from,
                "request_to" to header.request_to,
                "id" to header.id
            ),
            "body" to mapOf(
                "command" to body.command,
                "parameter" to body.parameter
            )
        )
    }
}

data class WepinRequestMessageHeader(
    val request_from: String = "web",
    val request_to: String = "wepin_widget",
    val id: Long
)

data class WepinRequestMessageBody(
    val command: String,
    val parameter: Any? = null
)
