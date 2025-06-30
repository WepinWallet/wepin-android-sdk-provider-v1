package com.wepin.sample.network

import com.wepin.sample.WepinProviderViewModel


val ethMethodList = arrayListOf(
    "eth_blockNumber",
    "eth_accounts",
    "eth_requestAccounts",
    "eth_getBalance",
    "eth_gasPrice",
    "eth_estimateGas",
    "eth_signTransaction",
    "eth_sendTransaction",
    "eth_call",
    "eth_sign",
    "personal_sign",
    "eth_signTypedData_v1",
    "eth_signTypedData_v3",
    "eth_signTypedData_v4"
)

val ethMethodParamSpecs = mapOf(
    "eth_sendTransaction" to mapOf(
        "from" to "0x000000000000000000000000000000000000000",
        "to" to "0x000000000000000000000000000000000000000",
        "gas" to "",
        "gasPrice" to "",
        "value" to "0x03e8",
        "data" to ""
    ),
    "eth_signTransaction" to mapOf(
        "from" to "0x000000000000000000000000000000000000000",
        "to" to "0x000000000000000000000000000000000000000",
        "gas" to "",
        "gasPrice" to "",
        "value" to "0x03e8",
        "data" to ""
    ),
    "personal_sign" to mapOf(
        "message" to "Hello, World!",
        "address" to "0x000000000000000000000000000000000000000"
    ),
    "eth_sign" to mapOf(
        "address" to "0x000000000000000000000000000000000000000",
        "message" to "Hello, World!"
    ),
    "eth_signTypedData_v1" to mapOf(
        "address" to "0x000000000000000000000000000000000000000",
        "typedData" to listOf(
            mapOf(
                "type" to "string",
                "name" to "message",
                "value" to "Hello, world!"
            )
        )
    ),
    "eth_signTypedData_v3" to mapOf(
        "address" to "0x000000000000000000000000000000000000000",
        "typedData" to mapOf(
            "types" to mapOf(
                "EIP712Domain" to listOf(
                    mapOf("name" to "name", "type" to "string"),
                    mapOf("name" to "version", "type" to "string"),
                    mapOf("name" to "chainId", "type" to "uint256"),
                    mapOf("name" to "verifyingContract", "type" to "address")
                ),
                "Person" to listOf(
                    mapOf("name" to "name", "type" to "string"),
                    mapOf("name" to "wallet", "type" to "address")
                ),
                "Mail" to listOf(
                    mapOf("name" to "from", "type" to "Person"),
                    mapOf("name" to "to", "type" to "Person"),
                    mapOf("name" to "contents", "type" to "string")
                )
            ),
            "primaryType" to "Mail",
            "domain" to mapOf(
                "name" to "Ether Mail",
                "version" to "1",
                "chainId" to 1,
                "verifyingContract" to "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
            ),
            "message" to mapOf(
                "from" to mapOf(
                    "name" to "Alice",
                    "wallet" to "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
                ),
                "to" to mapOf(
                    "name" to "Bob",
                    "wallet" to "0xA0cf798816D4b9b9866b5330EEa46a18382f251e"
                ),
                "contents" to "Hello, Bob!"
            )
        )
    ),
    "eth_signTypedData_v4" to mapOf(
        "address" to "0x000000000000000000000000000000000000000",
        "typedData" to mapOf(
            "types" to mapOf(
                "EIP712Domain" to listOf(
                    mapOf("name" to "name", "type" to "string"),
                    mapOf("name" to "version", "type" to "string"),
                    mapOf("name" to "chainId", "type" to "uint256"),
                    mapOf("name" to "verifyingContract", "type" to "address")
                ),
                "Person" to listOf(
                    mapOf("name" to "name", "type" to "string"),
                    mapOf("name" to "wallet", "type" to "address")
                ),
                "Mail" to listOf(
                    mapOf("name" to "from", "type" to "Person"),
                    mapOf("name" to "to", "type" to "Person"),
                    mapOf("name" to "contents", "type" to "string")
                )
            ),
            "primaryType" to "Mail",
            "domain" to mapOf(
                "name" to "Ether Mail",
                "version" to "1",
                "chainId" to 1,
                "verifyingContract" to "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
            ),
            "message" to mapOf(
                "from" to mapOf(
                    "name" to "Alice",
                    "wallet" to "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
                ),
                "to" to mapOf(
                    "name" to "Bob",
                    "wallet" to "0xA0cf798816D4b9b9866b5330EEa46a18382f251e"
                ),
                "contents" to "Hello, Bob!"
            )
        )
    )
)

val defaultJsonExampleGenerators = mapOf<String, (WepinProviderViewModel) -> String>(
    "eth_blockNumber" to { "[]" },
    "eth_accounts" to { "[]" },
    "eth_requestAccounts" to { "[]" },
    "eth_gasPrice" to { "[]" },

    "eth_getBalance" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "$account",
          "latest"
        ]"""
    },

    "eth_call" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          {
            "to": "$account",
            "data": "0xabcdef"
          },
          "latest"
        ]"""
    },

    "eth_estimateGas" to { _ ->
        """[
          {
            "from": "0xabcdef1234567890abcdef1234567890abcdef12",
            "to": "0xabcdef1234567890abcdef1234567890abcdef12",
            "data": "0xabcdef"
          }
        ]"""
    },

    "eth_signTransaction" to { _ ->
        """[
          {
            "from": "0xabcdef1234567890abcdef1234567890abcdef12",
            "to": "0xabcdef1234567890abcdef1234567890abcdef12",
            "gas": "0x5208",
            "gasPrice": "0x3b9aca00",
            "value": "0x0",
            "data": "0x"
          }
        ]"""
    },

    "eth_sendTransaction" to { _ ->
        """[
          {
            "from": "0xabcdef1234567890abcdef1234567890abcdef12",
            "to": "0xabcdef1234567890abcdef1234567890abcdef12",
            "gas": "0x5208",
            "gasPrice": "0x3b9aca00",
            "value": "0x0",
            "data": "0x"
          }
        ]"""
    },

    "eth_sign" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "$account",
          "0x68656c6c6f20776f726c64"
        ]"""
    },

    "personal_sign" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "0x68656c6c6f20776f726c64",
          "$account"
        ]"""
    },

    "eth_signTypedData_v1" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "$account",
          [
            {
              "type": "string",
              "name": "message",
              "value": "Hello, world!"
            }
          ]
        ]"""
    },

    "eth_signTypedData_v3" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "$account",
          {
            "types": {
              "EIP712Domain": [
                { "name": "name", "type": "string" },
                { "name": "version", "type": "string" },
                { "name": "chainId", "type": "uint256" },
                { "name": "verifyingContract", "type": "address" }
              ],
              "Mail": [
                { "name": "from", "type": "Person" },
                { "name": "to", "type": "Person" },
                { "name": "contents", "type": "string" }
              ],
              "Person": [
                { "name": "name", "type": "string" },
                { "name": "wallet", "type": "address" }
              ]
            },
            "primaryType": "Mail",
            "domain": {
              "name": "Ether Mail",
              "version": "1",
              "chainId": 1,
              "verifyingContract": "$account"
            },
            "message": {
              "from": {
                "name": "Alice",
                "wallet": "$account"
              },
              "to": {
                "name": "Bob",
                "wallet": "$account"
              },
              "contents": "Hello, Bob!"
            }
          }
        ]"""
    },

    "eth_signTypedData_v4" to { viewModel ->
        val account = viewModel.connectedAccount ?: "0xabcdef1234567890abcdef1234567890abcdef12"
        """[
          "$account",
          {
            "types": {
              "EIP712Domain": [
                { "name": "name", "type": "string" },
                { "name": "version", "type": "string" },
                { "name": "chainId", "type": "uint256" },
                { "name": "verifyingContract", "type": "address" }
              ],
              "Person": [
                { "name": "name", "type": "string" },
                { "name": "wallet", "type": "address" }
              ],
              "Mail": [
                { "name": "from", "type": "Person" },
                { "name": "to", "type": "Person" },
                { "name": "contents", "type": "string" }
              ]
            },
            "primaryType": "Mail",
            "domain": {
              "name": "Ether Mail",
              "version": "1",
              "chainId": 1,
              "verifyingContract": "$account"
            },
            "message": {
              "from": {
                "name": "Alice",
                "wallet": "$account"
              },
              "to": {
                "name": "Bob",
                "wallet": "$account"
              },
              "contents": "Hello, Bob!"
            }
          }
        ]"""
    }
)
