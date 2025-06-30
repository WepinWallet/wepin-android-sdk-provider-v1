<br/>

<p align="center">
  <a href="https://www.wepin.io/">
      <picture>
        <source media="(prefers-color-scheme: dark)">
        <img alt="wepin logo" src="https://github.com/WepinWallet/wepin-web-sdk-v1/blob/main/assets/wepin_logo_color.png?raw=true" width="250" height="auto">
      </picture>
</a>
</p>

<br>

# Wepin Android SDK Provider Library v1

[![platform - android](https://img.shields.io/badge/platform-Android-3ddc84.svg?logo=android&style=for-the-badge)](https://www.android.com/)
[![SDK Version](https://img.shields.io/jitpack/version/com.github.WepinWallet/wepin-android-sdk-pin-v1.svg?logo=jitpack&style=for-the-badge)](https://jitpack.io/v/com.github.WepinWallet/wepin-android-sdk-pin-v1)

Wepin Provider library for Android. This package is exclusively available for use in Android
environments.

## ⏩ Get App ID and Key

After signing up for [Wepin Workspace](https://workspace.wepin.io/), navigate to the development
tools menu, and enter the required information for each app platform to receive your App ID and App
Key.

Wepin supports providers that return JSON-RPC request responses to connect with blockchain networks
in webs. With Wepin Provider, you can easily connect to various networks supported by Wepin.

The providers supported by Wepin are as follows.

- EVM compatible Networks
- Klaytn Network

## ⏩ Requirements

- **Android**: API version **24** or newer is required.

## ⏩ Install

1. Add JitPack repository in your project-level build gradle file

- kts
  ```kotlin
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
           maven("https://jitpack.io") // <= Add JitPack Repository
       }
   }
  ```

2. Add implementation in your app-level build gradle file

- kts

  ```
  dependencies {
    // ...
    implementation("com.github.WepinWallet:wepin-android-sdk-provider-v1:vX.X.X")
  }
  ```

  > **<span style="font-size: 35px;"> !!Caution!! </span>** We recommend
  >
  using [the latest released version of the SDK](https://github.com/WepinWallet/wepin-android-sdk-provider-v1/releases)

## ⏩ Getting Started

### Config Deep Link

he Deep Link configuration is required for logging into Wepin. Setting up the Deep Link Scheme
allows your app to handle external URL calls.

The format for the Deep Link scheme is `wepin. + Your Wepin App ID`

When a custom scheme is used, the WepinWidget SDK can be easily configured to capture all redirects
using this custom scheme through a manifest placeholder in the `build.gradle (app)` file::

```kotlin
// For Deep Link => RedirectScheme Format: wepin. + Wepin App ID
android.defaultConfig.manifestPlaceholders = [
    'appAuthRedirectScheme': 'wepin.{{YOUR_WEPIN_APPID}}'
]
```

## ⏩ Import SDK

```kotlin
import com.wepin.android.providerlib.WepinProvider;
```

## ⏩ Initialize

```kotlin
val wepinProviderParams = WepinProviderParams(
    context = this,
    appId = "Wepin-App-ID",
    appKey = "Wepin-App-Key"
)
var wepinProvider = WepinProvider(wepinProviderParams)
```

### initialize

```kotlin
wepinProvider.initialize(attributes)
```

#### Parameters

- `attributes` \<WepinProviderAttributes> __optional__
    - `defaultLanguage` \<String> __optional__ - The language to be displayed on the widget (
      default: 'en').
      Currently, only 'ko', 'en', and 'ja' are supported.
    - `defaultCurrency` \<String> __optional__ - The currency to be displayed on the widget.
      Currently, only '
      KRW', 'USD', 'JPY' are suppored.

#### Returns

- CompletableFuture \<Boolean>
    - Returns `true` if success

#### Example

```kotlin
var attributes = WepinProviderAttributes("en", "USD")
var res = wepinProvider.initialize(attributes)
res?.wenComplete { initResponse, error ->
    if (error == null) {
        if (initResponse) {
            //initialize success
        }
    } else {
        //error 
    }
}

```

### isInitialized

```kotlin
wepinProvider.isInitialized()
```

The `isInitialized()` method checks if the Wepin Provider Libarary is initialized.

#### Returns

- \<Boolean> - Returns `true` if Wepin Provider SDK is already initialized, otherwise false.

#### Example

```kotlin
if (wepinProvider?.isInitialized() != true) {
    wepinProvider?.initialize()
}

```

## ⏩ Method

Methods can be used after initialization of Wepin Provider.

### getProvider

```kotlin
wepinProvider.getProvider(network)
```

It returns a Provider by given network

#### Parameters

- network \<String> - The network name (must be lowercase). e.g., "ethereum", "klaytn"

#### Returns

- \<BaseProvider> - A EIP-1193 provider

#### Example

```kotlin
provider = wepinProvider?.getProvider("ethereum")
```

### login

The `login` variable is a Wepin login library that includes various authentication methods, allowing
users to log in using different approaches. It supports email and password login, OAuth provider
login, login using ID tokens or access tokens, and more. For detailed information on each method,
please refer to the official library documentation
at [wepin_android_login_lib](https://github.com/WepinWallet/wepin-android-sdk-login-v1).

#### Available Methods

- `loginWithOauthProvider`
- `signUpWithEmailAndPassword`
- `loginWithEmailAndPassword`
- `loginWithIdToken`
- `loginWithAccessToken`
- `getRefreshFirebaseToken`
- `loginWepin`
- `getCurrentWepinUser`
- `logout`
- `getSignForLogin`

These methods support various login scenarios, allowing you to select the appropriate method based
on your needs.

For detailed usage instructions and examples for each method, please refer to the official library
documentation. The documentation includes explanations of parameters, return values, exception
handling, and more.

#### Example

```kotlin
// Login using an OAuth provider
val oauthResult =
    wepinWidget.login.loginWithOauthProvider(provider: 'google', clientId: 'your-client-id')
oauthResult?.whenComplete { res, error ->
    if (error == null) {
        println(res)
    } else {
        println(error)
    }
}

// Sign up and log in using email and password
val signUpResult =
    wepinWidget.login.signUpWithEmailAndPassword(email: 'example@example.com', password: 'password123')
signUpResult?.whenComplete { res, error ->
    if (error == null) {
        println(res)
    } else {
        println(error)
    }
}

// Log in using an ID token
val idTokenResult = wepinWidget.login.loginWithIdToken(idToken: 'your-id-token', sign: 'your-sign')
idTokenResult?.whenComplete { res, error ->
    if (error == null) {
        println(res)
    } else {
        println(error)
    }
}

// Log in to Wepin
val wepinLoginResult = wepinWidget.login.loginWepin(idTokenResult)
wepinLoginResult?.whenComplete { res, error ->
    if (error == null) {
        println(res)
    } else {
        println(error)
    }
}

// Get the currently logged-in user
val currentUser = wepinWidget.login.getCurrentWepinUser()
currentUser?.whenComplete { res, error ->
    if (error == null) {
        println(res)
    } else {
        println(error)
    }
}

// Logout
wepinWidget.login.logout()

```

For more details on each method and to see usage examples, please visit the
official  [wepin_android_login_lib documentation](https://github.com/WepinWallet/wepin-android-sdk-login-v1).

### finalize

```kotlin
wepinProvider.finalize()
```

The `finalize()` method finalizes the Wepin Provider.

#### Returns

- \<Boolean> - Returns `true` when WepinProvider is finalized

#### Example

```kotlin

wepinProvider.finalize()

```

## ⏩ Provider Method

Once you have obtained a provider using getProvider(), you can use the following methods:

### request

```kotlin
provider.request(method, params)
```

The `request` method sends JSON-RPC requests to the blockchain network. It handles both
Wepin-specific wallet methods (like account requests and transaction signing) and standard
blockchain RPC calls.

#### Parameters

- method \<String> - RPC method name
- params \<List<Any>> - __optional__ Method parameters

#### Returns

- CompletableFuture<Any> - RPC Response

#### Example

```kotlin
val provider = wepinProvider.getProvider("ethereum")
//get connected account
provider.request("eth_requestAccounts", listOf()).whenComplete { result, error ->
    if (error != null) {
        //provider request error
    } eles {
        val jsonArray = JSONArray(result.toString())
        if (jsonArray.length() > 0) {
            connectedAccount = jsonArray.getString(0)
            Log.d(TAG, "connectedAccount saved: $connectedAccount")
        }
    }
}

//sign
provider.request(
    "eth_sign",
    listOf("0x...", "Hello, world!")
)

// Personal sign
provider.request(
    "personal_sign",
    listOf("Hello, World", "0x...")
)

// Sign typed data v1
val typedDataV1 = listOf(
    mapOf("type" to "string", "name" to "message", "value" to "Hello, world!")
)
provider.request(
    "eth_signTypedData_v1",
    listOf("0x...", typedDataV1)
)

// Sign typed data v4  
val typedDataV4 = mapOf(
    "types" to mapOf(
        "EIP712Domain" to listOf(mapOf("name" to "name", "type" to "string")),
        "Mail" to listOf(mapOf("name" to "contents", "type" to "string"))
    ),
    "primaryType" to "Mail",
    "domain" to mapOf("name" to "Ether Mail"),
    "message" to mapOf("contents" to "Hello, Bob!")
)
provider.request(
    "eth_signTypedData_v4",
    listOf("0x...", typedDataV4)
)

// Send transaction
val transaction = mapOf(
    "from" to "0x...",
    "to" to "0x...",
    "value" to "0x0",
    "data" to "0x"
)
provider.request("eth_sendTransaction", listOf(transaction))
```


