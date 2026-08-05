package com.secureehr.app.ui.components

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.secureehr.app.data.local.TokenManager

private enum class BiometricGateState { CHECKING, AUTHENTICATED, BLOCKED }

/**
 * Wraps a sensitive screen so it only renders after a successful fingerprint check,
 * whenever the user has turned "Fingerprint Authentication" on in Settings.
 */
@Composable
fun BiometricGate(
    navController: NavController,
    screenName: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var biometricEnabled by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        tokenManager.biometricEnabled.collect { biometricEnabled = it }
    }

    when (biometricEnabled) {
        null -> Unit // preference not loaded yet — avoid a flash of content before we know
        false -> content()
        true -> {
            var state by remember { mutableStateOf(BiometricGateState.CHECKING) }

            LaunchedEffect(screenName) {
                val activity = context as? FragmentActivity
                if (activity == null) {
                    state = BiometricGateState.BLOCKED
                    navController.popBackStack()
                    return@LaunchedEffect
                }

                when (BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        val prompt = BiometricPrompt(
                            activity,
                            ContextCompat.getMainExecutor(activity),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    state = BiometricGateState.AUTHENTICATED
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    state = BiometricGateState.BLOCKED
                                    navController.popBackStack()
                                }
                            }
                        )
                        prompt.authenticate(
                            BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Fingerprint Authentication")
                                .setSubtitle("Verify it's you to access $screenName")
                                .setNegativeButtonText("Cancel")
                                .build()
                        )
                    }
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                        Toast.makeText(
                            activity,
                            "No fingerprint enrolled — please add one in device settings",
                            Toast.LENGTH_LONG
                        ).show()
                        state = BiometricGateState.BLOCKED
                        navController.popBackStack()
                    }
                    else -> {
                        Toast.makeText(
                            activity,
                            "Fingerprint authentication is unavailable on this device",
                            Toast.LENGTH_LONG
                        ).show()
                        state = BiometricGateState.BLOCKED
                        navController.popBackStack()
                    }
                }
            }

            if (state == BiometricGateState.AUTHENTICATED) {
                content()
            }
        }
    }
}
