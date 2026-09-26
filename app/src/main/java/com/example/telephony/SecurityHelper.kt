package com.example.telephony

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

object SecurityHelper {

    fun isBiometricSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        return keyguardManager?.isKeyguardSecure == true
    }

    fun applyWindowSecurity(activity: Activity, enable: Boolean) {
        // We do not set unconditional FLAG_SECURE on the active window because it blacks out the
        // streaming display and canvas. App Switcher privacy is handled at lifecycle pause level.
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun showBiometricPrompt(
        activity: Activity,
        title: String = "Unlock Salim Messages",
        subtitle: String = "Verify your identity to view messages",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ): CancellationSignal {
        val cancellationSignal = CancellationSignal()
        val executor = Executors.newSingleThreadExecutor()

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton("Cancel", executor, DialogInterface.OnClickListener { _, _ ->
                activity.runOnUiThread { onError("Authentication cancelled") }
            })
            .build()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    activity.runOnUiThread { onSuccess() }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    activity.runOnUiThread { onError(errString?.toString() ?: "Authentication error") }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    activity.runOnUiThread { onError("Biometric not recognized") }
                }
            }
        )

        return cancellationSignal
    }
}
