sealed class EVMError(
    val code: Int,
    override val message: String
) : Throwable() {
    // Provider 에러 (EIP-1193)
    object UserRejectedRequest : EVMError(4001, "User rejected the request")
    object Unauthorized :
        EVMError(4100, "The requested method and/or account has not been authorized by the user")

    object UnsupportedMethod : EVMError(4200, "The Provider does not support the requested method")
    object Disconnected : EVMError(4900, "The Provider is disconnected from all chains")
    object ChainDisconnected :
        EVMError(4901, "The Provider is not connected to the requested chain")

    // JSON-RPC 에러 (EIP-1474)
    object ParseError : EVMError(-32700, "Invalid JSON was received by the server")
    object InvalidRequest : EVMError(-32600, "The JSON sent is not a valid Request object")
    object MethodNotFound : EVMError(-32601, "The method does not exist / is not available")
    object InvalidParams : EVMError(-32602, "Invalid method parameter(s)")
    object InternalError : EVMError(-32603, "Internal JSON-RPC error")

    // 트랜잭션 관련 에러
    object InvalidInput : EVMError(-32000, "Missing or invalid parameters")
    object ResourceNotFound : EVMError(-32001, "Requested resource not found")
    object ResourceUnavailable : EVMError(-32002, "Requested resource not available")
    object TransactionRejected : EVMError(-32003, "Transaction creation failed")
    object MethodNotSupported : EVMError(-32004, "Method is not implemented")
    object RequestLimitExceeded : EVMError(-32005, "Request exceeds defined limit")
    object JsonRpcVersionNotSupported :
        EVMError(-32006, "Version of JSON-RPC protocol not supported")
}