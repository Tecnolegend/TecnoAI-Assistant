package com.tecnolegend.tecnoai.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {

    // Firebase is optional: builds without google-services.json must still open normally.
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase not configured; cloud sign-in disabled")
            null
        }
    }
    private val credentialManager by lazy { CredentialManager.create(context) }
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    /** True when this build has Firebase and a Google sign-in client ID set up. */
    val isCloudSignInAvailable: Boolean
        get() = auth != null && webClientId.isNotBlank()

    private val webClientId: String
        get() = try {
            com.tecnolegend.tecnoai.BuildConfig.GOOGLE_WEB_CLIENT_ID.takeUnless { it.startsWith("MY_") } ?: ""
        } catch (e: Throwable) {
            ""
        }

    private val _authState = MutableStateFlow<AuthState>(
        auth?.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Idle
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser? get() = auth?.currentUser

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Idle
            }
        }
    }

    suspend fun signInWithGoogle(activity: Activity): Boolean = withContext(Dispatchers.Main) {
        val firebaseAuth = auth
        if (firebaseAuth == null || webClientId.isBlank()) {
            _authState.value = AuthState.Error(
                "Cloud sign-in isn't set up in this build. Add google-services.json and GOOGLE_WEB_CLIENT_ID to enable it."
            )
            return@withContext false
        }
        _authState.value = AuthState.Loading
        try {
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null) {
                    syncUserProfile(user)
                    _authState.value = AuthState.Authenticated(user)
                    return@withContext true
                }
            }

            _authState.value = AuthState.Error("Failed to extract Google credentials")
            false
        } catch (e: GetCredentialCancellationException) {
            _authState.value = auth?.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Idle
            false
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in error: ${e.localizedMessage}")
            _authState.value = AuthState.Error(e.localizedMessage ?: "Google Sign-in failed")
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            _authState.value = AuthState.Idle
        } catch (e: Exception) {
            _authState.value = AuthState.Idle
        }
    }

    private fun syncUserProfile(user: FirebaseUser) {
        try {
            val profile = hashMapOf(
                "uid" to user.uid,
                "displayName" to (user.displayName ?: "Tecno User"),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to System.currentTimeMillis(),
                "device" to android.os.Build.MODEL
            )
            firestore?.collection("users")
                ?.document(user.uid)
                ?.set(profile, SetOptions.merge())
        } catch (e: Exception) {
            // Ignore cloud sync error
        }
    }
}
