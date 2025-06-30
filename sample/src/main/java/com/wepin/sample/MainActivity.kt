package com.wepin.sample

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.android.providerlib.WepinProvider
import com.wepin.android.providerlib.provider.BaseProvider
import com.wepin.android.providerlib.types.WepinProviderParams
import com.wepin.sample.network.defaultJsonExampleGenerators
import com.wepin.sample.network.ethMethodList
import com.wepin.sample.network.ethMethodParamSpecs
import com.wepin.sample.ui.theme.WepinAndroidSDKTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WepinAndroidSDKTheme {
                WepinLoginTestScreen()
            }
        }
    }
}

class WepinProviderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WepinProviderModel"
    private val context: Context
        get() = getApplication<Application>().applicationContext
    var connectedAccount by mutableStateOf<String?>(null)
        private set

    private var _status = mutableStateOf("Not Initialized")
    val status: State<String> = _status
    private var _lastAction = mutableStateOf("No action yet")
    val lastAction: State<String> = _lastAction

    private var wepinLogin: WepinLogin? = null
    private var wepinProvider: WepinProvider? = null

    private var provider: BaseProvider? = null

    private var wepinUser: WepinUser? = null

    private var _oauthResult = mutableStateOf<LoginOauthResult?>(null)
    val oauthResult: State<LoginOauthResult?> = _oauthResult

    private var _idTokenResult = mutableStateOf<LoginResult?>(null)
    val idTokenResult: State<LoginResult?> = _idTokenResult

    private var _wepinUserResult = mutableStateOf<WepinUser?>(null)
    val wepinUserResult: State<WepinUser?> = _wepinUserResult

    private var _oauthError = mutableStateOf<String?>(null)
    val oauthError: State<String?> = _oauthError

    private var _idTokenError = mutableStateOf<String?>(null)
    val idTokenError: State<String?> = _idTokenError

    private var _wepinLoginError = mutableStateOf<String?>(null)
    val wepinLoginError: State<String?> = _wepinLoginError

    fun isProviderInitialized(): Boolean {
        return provider != null
    }

    fun initializeWepinLogin(context: Context, appId: String, appKey: String) {
        wepinLogin = WepinLogin(WepinLoginOptions(context, appId, appKey))
        wepinProvider = WepinProvider(
            WepinProviderParams(
                context = context,
                appId = appId,
                appKey = appKey
            )
        )
    }

    fun initialize() {
        val result = mutableStateOf<String>("")
        val spacer = mutableStateOf<String>("")
        val initTasks = mutableListOf<CompletableFuture<Void>>()

        if (wepinLogin?.isInitialized() != true) {
            val loginInit = wepinLogin?.init()?.thenApply {
                spacer.value = "\n"
                result.value += if (it) "WepinLogin initialized" else "WepinLogin init() fail"
            }?.exceptionally { error ->
                Log.e(TAG, "WepinLogin init error: $error")
                spacer.value = "\n"
                result.value += "WepinLogin exception: $error"
                null
            }
            loginInit?.let { initTasks.add(it.thenAccept {}) }
        }

        if (wepinProvider?.isInitialized() != true) {
            val providerInit = wepinProvider?.initialize()?.thenApply {
                wepinLogin = wepinProvider?.login
                result.value += spacer.value
                result.value += if (it) "WepinPin initialized" else "WepinPin initialize fail"
            }?.exceptionally { error ->
                Log.e(TAG, "WepinProvider error: $error")
                result.value += spacer.value + "WepinProvider exception: $error"
                null
            }

            providerInit?.let { initTasks.add(it.thenAccept {}) }
        }

        // 모든 비동기 작업 완료 후 updateStatus 실행 (성공/실패 모두 처리)
        CompletableFuture.allOf(*initTasks.toTypedArray())
            .thenRun {
                updateStatus("initialize", result.value)
            }
            .exceptionally { error ->
                Log.e(TAG, "Initialize allOf error: $error")
                result.value += "\nOverall initialization error: $error"
                updateStatus("initialize", result.value)
                null
            }
    }

    fun checkInitializationStatus() {
        var result = ""
        if (wepinLogin?.isInitialized() == true) {
            result = "WepinLogin is initialized"
        } else {
            result = "WepinLogin is not initialized"
        }
        result += "\n"
        if (wepinProvider?.isInitialized() == true)
            result += "WepinProvider is Initialized"
        else
            result += "WepinProvider is not Initialized"

        updateStatus("checkInitializationStatus", result)
    }

    fun signUpWithEmail() {
        Log.d(TAG, "signUpWithEmail")
        val params = LoginWithEmailParams(
            email = "email@address.com",
            password = "password",
            locale = "ko"
        )
        wepinLogin?.signUpWithEmailAndPassword(params)?.whenComplete { result, error ->
            Log.d(TAG, "signUpWithEmail End")
            if (error != null) {
                updateStatus("signUpWithEmail", "$error")
            } else {
                updateStatus("signUpWithEmail", "$result")
            }
        }
    }
//    fun loginWepin(type: String) {
//        when (type) {
//            "email" -> {
//                loginWepinWithEmail()
//            }
//
//            "oauth" -> {
//                loginWepinWithOauth("google")
//            }
//        }
//    }

    fun loginWepinWithEmail(email: String, password: String) {
        val params = LoginWithEmailParams(
            email,
            password
        )
        wepinLogin?.loginWithEmailAndPassword(params)?.thenApply { loginResult ->
            wepinLogin?.loginWepin(loginResult)?.thenApply { wepinUser ->
                updateStatus("loginWepinWithEmail", "$wepinUser")
            }?.exceptionally {
                updateStatus("loginWepinWithEmail", "$it")
            }
        }?.exceptionally {
            updateStatus("loginWepinWithEmail", "$it")
            null
        }
    }

    fun loginWepinWithOauth(provider: String) {
        _oauthError.value = null
        _oauthResult.value = null
        val params = LoginOauth2Params(
            provider = provider,
            clientId = when (provider) {
                "google" -> context.getString(R.string.default_google_web_client_id)
                "apple" -> context.getString(R.string.default_apple_client_id)
                "discord" -> context.getString(R.string.default_discord_client_id)
                "naver" -> context.getString(R.string.default_naver_client_id)
                "facebook" -> context.getString(R.string.default_facebook_client_id)
                "line" -> context.getString(R.string.default_line_client_id)
                else -> ""
            }
        )
        wepinLogin?.loginWithOauthProvider(params)?.thenApply { result ->
            _oauthResult.value = result
            updateStatus("loginWithOauthProvider", "$result")
        }?.exceptionally {
            _oauthError.value = it.toString()
            updateStatus("loginWithOauthProvider", "$it")
            null
        }
    }

    fun loginWithToken() {
        Log.d(TAG, "loginWithToken type: ${_oauthResult.value?.type}")
        _idTokenResult.value = null
        when (_oauthResult.value?.type) {
            OauthTokenType.ID_TOKEN -> loginWithIdToken()
            OauthTokenType.ACCESS_TOKEN -> loginWithAccessToken()
            null -> updateStatus("loginWithToken", "type is null")
        }
    }

    private fun loginWithAccessToken() {
        _idTokenError.value = null
        val accesstokenParams = LoginOauthAccessTokenRequest(
            provider = _oauthResult.value?.provider ?: "",
            accessToken = _oauthResult.value?.token ?: ""
        )
        wepinLogin?.loginWithAccessToken(accesstokenParams)?.thenApply { result ->
            _idTokenResult.value = result
            updateStatus("loginWithAccessToken", "$result")
        }?.exceptionally {
            _idTokenError.value = it.toString()
            updateStatus("loginWithAccessToken", "$it")
            null
        }
    }

    fun loginWithIdToken() {
        _idTokenError.value = null
        val idTokenParams = LoginOauthIdTokenRequest(
            idToken = _oauthResult.value?.token ?: ""
        )
        wepinLogin?.loginWithIdToken(idTokenParams)?.thenApply { result ->
            _idTokenResult.value = result
            updateStatus("loginWithIdToken", "$result")
        }?.exceptionally {
            _idTokenError.value = it.toString()
            updateStatus("loginWithIdToken", "$it")
            null
        }
    }

    fun loginWepin() {
        _wepinLoginError.value = null
        _wepinUserResult.value = null
        _idTokenResult.value?.let { loginResult ->
            wepinLogin?.loginWepin(loginResult)?.thenApply { user ->
                wepinUser = user
                _wepinUserResult.value = wepinUser

                updateStatus("loginWepin", "$wepinUser")
            }?.exceptionally {
                _wepinLoginError.value = it.toString()
                updateStatus("loginWepin", "$it")
            }
        }
    }

    //
    fun getWepinUser() {
        Log.d(TAG, "getWepinUser")
        wepinLogin?.getCurrentWepinUser()?.thenApply {
            Log.d(TAG, "getWepinUser: $it")
            wepinUser = it
            updateStatus("wepinUser", "$it")
        }?.exceptionally {
            Log.e(TAG, "getWepinUserError: $it")
            updateStatus("wepinUser", "$it")
        }
        Log.d(TAG, "getWepinUser end")
    }

    fun logoutWepin() {
        wepinLogin?.logoutWepin()?.thenApply {
            _status.value = "$it"
        }?.exceptionally {
            _status.value = "$it"
        }
    }

    private fun <T, U> execFun(
        method: String,
        params: T? = null,
        generateFunc: (T?) -> CompletableFuture<U>,
        onSuccess: (U) -> Unit
    ) {
        generateFunc(params).thenApply { result ->
            updateStatus(method, "$result")
            onSuccess(result)
        }.exceptionally {
            updateStatus(method, "$it")
        }
    }

    fun finalize(sdk: String? = "provider") {
        try {
            if (sdk == "login")
                wepinLogin?.finalize()
            else {
                wepinProvider?.finalize()
//            wepinLogin = null
            }
            updateStatus("finalize", "")
        } catch (error: Exception) {
            updateStatus("finalize", "$error")
        }
    }

    fun getProvider(network: String) {
        provider = wepinProvider?.getProvider(network)
        if (provider != null) {
            updateStatus("getProvider", "$network provider get!")
        }
    }

    fun sendRequest(method: String, params: List<Any>?) {
        if (provider == null) {
            updateStatus("sendRequest", "provider is null")
        }

        provider?.request(method, params)?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("sendRequest", "error is $error")
            } else {
                Log.d(TAG, "sendRequest responses: $result")
                updateStatus("sendRequest", "$result")

                // 연결된 address 저장
                if (method == "eth_accounts" || method == "eth_requestAccounts") {
                    try {
                        val jsonArray = JSONArray(result.toString())
                        if (jsonArray.length() > 0) {
                            connectedAccount = jsonArray.getString(0)
                            Log.d(TAG, "connectedAccount saved: $connectedAccount")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing connected account: $e")
                    }
                }
            }
        }
    }

    private fun updateStatus(action: String, message: String) {
        _lastAction.value = action
        _status.value = message
    }
}

@Composable
fun WepinLoginTestScreen(
    viewModel: WepinProviderViewModel = viewModel()
) {
    val context = LocalContext.current
    val appId = remember { context.getString(R.string.wepin_app_id) }
    val appKey = remember { context.getString(R.string.wepin_app_key) }

    LaunchedEffect(Unit) {
        viewModel.initializeWepinLogin(context, appId, appKey)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp)
    ) {
        HeaderSection()
        ButtonSection(
            modifier = Modifier.weight(1f),
            onInitialize = { viewModel.initialize() },
            onCheckStatus = { viewModel.checkInitializationStatus() },
            onSignUp = { viewModel.signUpWithEmail() },
//            onRefreshToken = { refresh ->
//                viewModel.getRefreshToken(refresh)
//            },
            onLogout = { viewModel.logoutWepin() },
            onGetUser = { viewModel.getWepinUser() },
            onGetProvider = { network -> viewModel.getProvider(network) },
//            onSendRequest = { method, params -> viewModel.sendRequest(method, params) },
            onSwitchChain = { chainId ->
                viewModel.sendRequest(
                    "wallet_switchEthereumChain",
                    listOf(mapOf("chainId" to chainId))
                )
            },
            onFinalize = { viewModel.finalize() },
            isProviderInitialized = viewModel.isProviderInitialized(),
        )
        ResultSection(
            modifier = Modifier.weight(1f),
            action = viewModel.lastAction.value,
            status = viewModel.status.value
        )
    }
}

@Composable
private fun HeaderSection() {
    Text("Wepin Login Test", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier,
    onInitialize: () -> Unit,
    onCheckStatus: () -> Unit,
    onSignUp: () -> Unit,
    onLogout: () -> Unit,
    onGetUser: () -> Unit,
    onGetProvider: (String) -> Unit,
    onSwitchChain: (String) -> Unit,
    onFinalize: () -> Unit,
    isProviderInitialized: Boolean,
) {
    var showLoginScreen by remember { mutableStateOf(false) }
    var showMethodList by remember { mutableStateOf(false) }
    var showProviderError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WepinButton(text = "Initialize", onClick = onInitialize)
        WepinButton(text = "Check Initialization Status", onClick = onCheckStatus)
        WepinButton(text = "Login", onClick = { showLoginScreen = true })
        WepinButton(text = "Sign Up", onClick = onSignUp)
        WepinButton(text = "Wepin Logout", onClick = onLogout)
        WepinButton(text = "Get Wepin User", onClick = onGetUser)
        WepinButton(
            text = "GetProvider(Oasys Testnet)",
            onClick = { onGetProvider("evmoasys-games-testnet") })
        WepinButton(
            text = "Send Request",
            onClick = {
                if (!isProviderInitialized) {
                    showProviderError = true
                } else {
                    showMethodList = true
                }
            })
        WepinButton(text = "switchChain", onClick = { onSwitchChain("0x2019") })
        WepinButton(text = "WepinLogin finalize", onClick = onFinalize)
    }

    if (showLoginScreen) {
        LoginScreen(
            onLogin = { type ->
                showLoginScreen = false
                // Don't call onLogin here since we've already completed the login process
            },
            onDismiss = { showLoginScreen = false },
            viewModel = viewModel()
        )
    }
    if (showMethodList) {
        ProviderMethodButtons(
            onDismiss = { showMethodList = false },
            viewModel = viewModel()
        )
    }
    if (showProviderError) {
        LoginScreen(
            onLogin = { type ->
                showLoginScreen = false
                // Don't call onLogin here since we've already completed the login process
            },
            onDismiss = { showProviderError = false },
            viewModel = viewModel()
        )
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: WepinProviderViewModel = viewModel()
) {
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailLogin by remember { mutableStateOf(false) }
    var loginStep by remember { mutableStateOf(0) } // 0: provider selection, 1: email login, 2: oauth login
    var oauthStep by remember { mutableStateOf(0) } // 0: not started, 1: oauth provider, 2: id token

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.9f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loginStep == 0) {
                    Text("Select Login Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Login Button
                    WepinButton(
                        text = "Login with Email",
                        onClick = {
                            selectedProvider = "email"
                            showEmailLogin = true
                            loginStep = 1
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // OAuth Provider Buttons
                    WepinButton(
                        text = "Login with Google",
                        onClick = {
                            selectedProvider = "google"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Apple",
                        onClick = {
                            selectedProvider = "apple"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Discord",
                        onClick = {
                            selectedProvider = "discord"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Naver",
                        onClick = {
                            selectedProvider = "naver"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Facebook",
                        onClick = {
                            selectedProvider = "facebook"
                            loginStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    WepinButton(
                        text = "Login with Line",
                        onClick = {
                            selectedProvider = "line"
                            loginStep = 2
                        }
                    )
                } else if (loginStep == 1 && showEmailLogin) {
                    // Email Login Form
                    Text("Email Login", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    WepinButton(
                        text = "Login",
                        onClick = {
                            viewModel.loginWepinWithEmail(email, password)
                            onDismiss()
                        }
                    )
                } else if (loginStep == 2 && selectedProvider != null) {
                    when (oauthStep) {
                        0 -> {
                            Text(
                                "Login with ${selectedProvider?.replaceFirstChar { it.titlecase() }}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Proceed",
                                onClick = {
                                    selectedProvider?.let { provider ->
                                        viewModel.loginWepinWithOauth(provider)
                                        oauthStep = 1
                                    }
                                }
                            )
                        }

                        1 -> {
                            Text(
                                "OAuth Provider Result",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(viewModel.oauthResult.value?.toString() ?: "No result yet")
                                    if (viewModel.oauthError.value != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Error: ${viewModel.oauthError.value}",
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Next",
                                onClick = {
                                    viewModel.loginWithToken()
                                    oauthStep = 2
                                }
                            )
                        }

                        2 -> {
                            Text("ID Token Result", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        viewModel.idTokenResult.value?.toString() ?: "No result yet"
                                    )
                                    if (viewModel.idTokenError.value != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Error: ${viewModel.idTokenError.value}",
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            WepinButton(
                                text = "Complete",
                                onClick = {
                                    viewModel.loginWepin()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                WepinButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun WepinButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ResultSection(
    modifier: Modifier = Modifier,
    action: String,
    status: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 10.dp)
            .background(Color.White)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = action, style = MaterialTheme.typography.bodyLarge)
                Text(text = status, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ProviderMethodButtons(
    onDismiss: () -> Unit,
    viewModel: WepinProviderViewModel = viewModel()
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var jsonInput by remember { mutableStateOf("") }

    if (selectedMethod == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.9f))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("EVM Method List", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    ethMethodList.forEach { method ->
                        WepinButton(
                            text = method,
                            onClick = {
                                selectedMethod = method
                                fieldValues.clear()

                                val defaultFields = ethMethodParamSpecs[method]
                                if (defaultFields != null) {
                                    fieldValues.putAll(
                                        defaultFields.mapValues { (_, v) ->
                                            when (v) {
                                                is String -> v
                                                else -> jacksonObjectMapper().writeValueAsString(v)
                                            }
                                        }
                                    )
                                } else {
                                    jsonInput =
                                        defaultJsonExampleGenerators[method]?.invoke(viewModel)
                                            ?: ""
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        val fields = ethMethodParamSpecs[selectedMethod]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Enter Parameters for $selectedMethod",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (fields != null) {
                fields.keys.forEach { field ->
                    if (fieldValues[field].isNullOrBlank()) {
                        fieldValues[field] = fields[field].toString()
                    }

                    OutlinedTextField(
                        value = fieldValues[field] ?: "",
                        onValueChange = { newValue -> fieldValues[field] = newValue },
                        label = { Text(field) },
                        placeholder = { Text(fields[field].toString()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("JSON Array Input (ex: [ {...} ])") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WepinButton(
                    text = "Send",
                    onClick = {
                        val params: List<Any> = try {
                            if (fields != null) {
                                when (selectedMethod) {
                                    "eth_sendTransaction", "eth_signTransaction" -> {
                                        listOf(fieldValues.filterValues { it.isNotBlank() })
                                    }

                                    "eth_sign", "personal_sign" -> {
                                        fields.keys.map { field -> fieldValues[field] ?: "" }
                                    }

                                    "eth_signTypedData_v1" -> {
                                        val address = fieldValues["address"] ?: ""
                                        val typedDataList = try {
                                            JSONArray(fieldValues["typedData"] ?: "[]").toList()
                                        } catch (e: Exception) {
                                            emptyList()
                                        }
                                        listOf(address, typedDataList)
                                    }

                                    "eth_signTypedData_v3", "eth_signTypedData_v4" -> {
                                        val address = fieldValues["address"] ?: ""
                                        val typedDataMap = try {
                                            jacksonObjectMapper().readValue(
                                                fieldValues["typedData"] ?: "{}",
                                                object : TypeReference<Map<String, Any>>() {}
                                            )
                                        } catch (e: Exception) {
                                            emptyMap()
                                        }
                                        listOf(address, typedDataMap)
                                    }

                                    else -> {
                                        fields.keys.map { field -> fieldValues[field] ?: "" }
                                    }
                                }
                            } else {
                                JSONArray(jsonInput).toList()
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }

                        viewModel.sendRequest(selectedMethod ?: "", params)
                        onDismiss()
                    }
                )

                WepinButton(
                    text = "Cancel",
                    onClick = {
                        selectedMethod = null
                        fieldValues.clear()
                        jsonInput = ""
                    }
                )
            }
        }
    }
}

fun JSONArray.toList(): List<Any> {
    val list = mutableListOf<Any>()
    for (i in 0 until this.length()) {
        val value = this.get(i)
        list.add(
            when (value) {
                is JSONArray -> value.toList()
                is JSONObject -> value.toMap()
                else -> value
            }
        )
    }
    return list
}

fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    for (key in keys()) {
        val value = this.get(key)
        map[key] = when (value) {
            is JSONArray -> value.toList()
            is JSONObject -> value.toMap()
            else -> value
        }
    }
    return map
}