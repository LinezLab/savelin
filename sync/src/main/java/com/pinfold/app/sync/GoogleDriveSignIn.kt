package com.pinfold.app.sync

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.security.MessageDigest

sealed interface SignInResult {
    data class Success(val token: String) : SignInResult
    data class Failure(val message: String, val code: Int) : SignInResult
    data object Cancelled : SignInResult
}

/** Android OAuth client: identified by package name + signing certificate, no client id in the app. */
class GoogleDriveSignIn(private val activity: FragmentActivity) {
    private val scope = Scope("https://www.googleapis.com/auth/drive.appdata")
    private var callback: ((SignInResult) -> Unit)? = null

    // Registration is only legal before the Activity is STARTED. The host Activity creates this
    // helper that early; the fallback keeps a late/unsafe construction from crashing the app.
    private val launcher: ActivityResultLauncher<IntentSenderRequest>? = runCatching {
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_CANCELED) {
                callback?.invoke(SignInResult.Cancelled)
                return@registerForActivityResult
            }
            try {
                val authorization = Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data)
                val token = authorization.accessToken.orEmpty()
                callback?.invoke(SignInResult.Success(token))
            } catch (e: ApiException) {
                callback?.invoke(SignInResult.Failure(e.localizedMessage ?: "Google sign-in failed", e.statusCode))
            } catch (e: Exception) {
                callback?.invoke(SignInResult.Failure(e.localizedMessage ?: "Google sign-in failed", -1))
            }
        }
    }.getOrNull()

    fun signIn(onResult: (SignInResult) -> Unit) {
        callback = onResult
        val target = launcher
        if (target == null) {
            onResult(SignInResult.Failure("Sign-in is unavailable in this context", -1))
            return
        }
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(scope))
            .build()
        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) {
                        target.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        onResult(SignInResult.Failure("Sign-in resolution was empty", -1))
                    }
                } else {
                    onResult(SignInResult.Success(authorization.accessToken.orEmpty()))
                }
            }
            .addOnFailureListener { error ->
                val code = (error as? ApiException)?.statusCode ?: -1
                onResult(SignInResult.Failure(error.localizedMessage ?: "Google sign-in failed", code))
            }
    }

    fun playServices(context: Context): Pair<Boolean, String> {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return if (availability == com.google.android.gms.common.ConnectionResult.SUCCESS) {
            true to (try {
                context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName.orEmpty()
            } catch (_: Exception) { "available" })
        } else {
            false to GoogleApiAvailability.getInstance().getErrorString(availability)
        }
    }

    fun applicationId(context: Context): String = context.packageName

    fun sha1(context: Context): String = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures.orEmpty()
        }
        val signature = signatures.firstOrNull() ?: return "Unavailable"
        val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
        digest.joinToString(":") { "%02X".format(it) }
    } catch (_: Exception) {
        "Unavailable"
    }
}

/**
 * Implemented by the host Activity. The sign-in helper is created in the Activity before it is
 * STARTED, because result launchers can only be registered that early. Creating it from a composable
 * (which runs after STARTED) threw IllegalStateException on the main thread.
 */
interface DriveSignInHost {
    val driveSignIn: GoogleDriveSignIn
}
