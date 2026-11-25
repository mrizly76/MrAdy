package com.sitephotos.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sitephotos.app.ui.LockScreen
import com.sitephotos.app.ui.SecurityScreen
import com.sitephotos.app.ui.SetPinScreen
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.sitephotos.app.ui.CameraScreen
import com.sitephotos.app.ui.HomeScreen
import com.sitephotos.app.ui.ReviewScreen

private sealed interface Screen {
    data object Home : Screen
    data class Camera(val project: Project, val building: Building) : Screen
    data class Review(val project: Project, val building: Building) : Screen
    data object Security : Screen
    data object ChangePin : Screen
}

class MainActivity : FragmentActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var lock: LockStore

    /** Lock again if the app was in the background longer than this. */
    private val relockAfterMs = 60_000L

    private fun fingerprintAvailable(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showFingerprint() {
        if (!fingerprintAvailable()) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    vm.locked = false
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Site Photos")
            .setSubtitle("Use your fingerprint")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    /** Google Drive scope limited to files this app creates. */
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.file")

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                vm.showError("Google account was not connected. Press Sync and choose your Google account.")
                return@registerForActivityResult
            }
            try {
                val auth = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(result.data)
                val token = auth.accessToken
                if (token != null) vm.sync(token)
                else vm.showError("Google did not return access. Please try Sync again.")
            } catch (e: ApiException) {
                vm.showError("Google authorization failed (code ${e.statusCode}).")
            }
        }

    /** Gets a fresh Drive access token (asks the user to pick an account the first time), then syncs. */
    private fun startSync() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveScope))
            .build()
        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    authLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    val token = result.accessToken
                    if (token != null) vm.sync(token)
                    else vm.showError("Google did not return access. Please try Sync again.")
                }
            }
            .addOnFailureListener { e ->
                vm.showError(
                    "Could not connect to Google Drive: ${e.message}\n" +
                        "If this is the first install, the Google Cloud OAuth setup in the README is still needed."
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = LockStore(this)
        if (!lock.hasPin) vm.needsPinSetup = true
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0B3954),
                    secondary = Color(0xFF1B998B),
                    tertiary = Color(0xFFFFC857),
                    background = Color(0xFFF4F6F8),
                )
            ) {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                val goHome = {
                    vm.refresh()
                    screen = Screen.Home
                }
                BackHandler(enabled = screen != Screen.Home && !vm.locked && !vm.needsPinSetup) { goHome() }

                // Security gate: first launch -> create PIN; otherwise PIN / fingerprint when locked.
                if (vm.needsPinSetup) {
                    SetPinScreen(
                        lock = lock,
                        isChange = false,
                        fingerprintAvailable = fingerprintAvailable(),
                        onDone = { vm.needsPinSetup = false; vm.locked = false },
                        onCancel = null,
                    )
                } else if (vm.locked) {
                    LockScreen(
                        lock = lock,
                        fingerprintAvailable = fingerprintAvailable(),
                        onFingerprint = { showFingerprint() },
                        onUnlocked = { vm.locked = false },
                    )
                } else when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        vm = vm,
                        onOpenCamera = { p, b -> screen = Screen.Camera(p, b) },
                        onReview = { p, b -> screen = Screen.Review(p, b) },
                        onSync = { startSync() },
                        onSecurity = { screen = Screen.Security },
                    )
                    Screen.Security -> SecurityScreen(
                        lock = lock,
                        fingerprintAvailable = fingerprintAvailable(),
                        onChangePin = { screen = Screen.ChangePin },
                        onBack = goHome,
                    )
                    Screen.ChangePin -> SetPinScreen(
                        lock = lock,
                        isChange = true,
                        fingerprintAvailable = fingerprintAvailable(),
                        onDone = { screen = Screen.Security },
                        onCancel = { screen = Screen.Security },
                    )
                    is Screen.Camera -> CameraScreen(
                        project = s.project,
                        building = s.building,
                        onBack = goHome,
                        onReview = { screen = Screen.Review(s.project, s.building) },
                    )
                    is Screen.Review -> ReviewScreen(
                        project = s.project,
                        building = s.building,
                        onBack = goHome,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val away = vm.backgroundSince
        if (away > 0 && System.currentTimeMillis() - away > relockAfterMs && lock.hasPin) vm.locked = true
        vm.backgroundSince = 0
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.backgroundSince = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}
