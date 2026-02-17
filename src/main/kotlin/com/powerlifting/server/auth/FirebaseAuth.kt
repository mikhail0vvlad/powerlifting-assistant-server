package com.powerlifting.server.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.powerlifting.server.config.FirebaseConfig
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.Base64

data class FirebaseUserPrincipal(
    val uid: String,
    val email: String? = null,
    val name: String? = null,
)

class FirebaseTokenVerifier(
    private val firebaseAuth: FirebaseAuth,
) {
    fun verify(idToken: String): FirebaseUserPrincipal {
        val decoded: FirebaseToken = firebaseAuth.verifyIdToken(idToken)
        return FirebaseUserPrincipal(
            uid = decoded.uid,
            email = decoded.email,
            name = decoded.name
        )
    }

    companion object {
        fun init(firebaseConfig: FirebaseConfig) {
            // Avoid double init
            if (FirebaseApp.getApps().isNotEmpty()) return

            val credentials = loadCredentials(firebaseConfig)
            val optionsBuilder = FirebaseOptions.builder().setCredentials(credentials)

            firebaseConfig.projectId?.let { optionsBuilder.setProjectId(it) }

            FirebaseApp.initializeApp(optionsBuilder.build())
        }

        private fun loadCredentials(firebaseConfig: FirebaseConfig): GoogleCredentials {
            val path = firebaseConfig.serviceAccountPath?.takeIf { it.isNotBlank() }
            if (path != null) {
                val file = File(path)
                require(file.exists()) { "FIREBASE_SERVICE_ACCOUNT_PATH points to non-existing file: $path" }
                FileInputStream(file).use { fis ->
                    return GoogleCredentials.fromStream(fis)
                }
            }

            val b64 = firebaseConfig.serviceAccountBase64?.takeIf { it.isNotBlank() }
                ?: error(
                    "Firebase service account not configured. Set FIREBASE_SERVICE_ACCOUNT_PATH or FIREBASE_SERVICE_ACCOUNT_BASE64"
                )

            val bytes = Base64.getDecoder().decode(b64)
            ByteArrayInputStream(bytes).use { bis ->
                return GoogleCredentials.fromStream(bis)
            }
        }

        fun createVerifier(): FirebaseTokenVerifier {
            return FirebaseTokenVerifier(FirebaseAuth.getInstance())
        }
    }
}
