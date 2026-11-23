package com.pinfold.app.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
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

/**
 * Android OAuth client: identified by package name + signing certificate, no client id in the app.
 *
 * The launcher is registered by the host Activity before it is STARTED (see [DriveSignInHost]). One
 * sign-in attempt owns one callback; a result is dispatched exactly once, so a late launcher result
 * or an intent delivered to onNewIntent/onResume can never fire a stale callback.
 */
class GoogleDriveSignIn(private val activity: FragmentActivity) {
    private val scope = Scope("https://www.googleapis.com/auth/drive.appdata")
    private var callback: ((SignInResult) -> Unit)? = null
    private var generation = 0
    private var awaiting = false

    private val launcher: ActivityResultLauncher<IntentSenderRequest>? = runCatching {
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            onLauncherResult(result.resultCode, result.data)
        }
    }.getOrNull()

    private fun dispatch(result: SignInResult) {
        val cb = callback ?: return
        callback = null
        awaiting = false
        cb(result)
    }

    fun signIn(onResult: (SignInResult) -> Unit) {
        generation++
        callback = onResult
        awaiting = true
        val target = launcher
        if (target == null) {
            dispatch(SignInResult.Failure("Sign-in is unavailable in this context", -1))
            return
        }
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(scope))
            .build()
        val mine = generation
        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { authorization ->
                if (mine != generation || !awaiting) return@addOnSuccessListener
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) {
                        target.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        dispatch(SignInResult.Failure("Sign-in resolution was empty", -1))
                    }
                } else {
                    val token = authorization.accessToken.orEmpty()
                    if (token.isBlank()) dispatch(SignInResult.Failure("Google did not return an access token", -1))
                    else dispatch(SignInResult.Success(token))
                }
            }
            .addOnFailureListener { error ->
                if (mine != generation) return@addOnFailureListener
                val code = (error as? ApiException)?.statusCode ?: -1
                dispatch(SignInResult.Failure(error.localizedMessage ?: "Google sign-in failed", code))
            }
    }

    private fun onLauncherResult(resultCode: Int, data: Intent?) {
        if (!awaiting) return
        if (resultCode == Activity.RESULT_CANCELED) {
            dispatch(SignInResult.Cancelled)
            return
        }
        try {
            val authorization = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
            val token = authorization.accessToken.orEmpty()
            if (token.isBlank()) dispatch(SignInResult.Failure("Google did not return an access token", -1))
            else dispatch(SignInResult.Success(token))
        } catch (e: ApiException) {
            dispatch(SignInResult.Failure(e.localizedMessage ?: "Google sign-in failed", e.statusCode))
        } catch (e: Exception) {
            dispatch(SignInResult.Failure(e.localizedMessage ?: "Google sign-in failed", -1))
        }
    }

    /** MainActivity is singleTask: a resolution may return through onNewIntent instead of the launcher. */
    fun onNewIntent(intent: Intent?) = inspect(intent)

    /** Also inspect the launching intent on resume, then clear it so it cannot fire twice. */
    fun onResume(intent: Intent?) = inspect(intent)

    private fun inspect(intent: Intent?) {
        if (!awaiting || intent == null) return
        val token = runCatching {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(intent).accessToken
        }.getOrNull()
        if (!token.isNullOrBlank()) dispatch(SignInResult.Success(token))
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
 * STARTED, because result launchers can only be registered that early.
 */
interface DriveSignInHost {
    val driveSignIn: GoogleDriveSignIn
}
