package com.kwazzar.lockruna.data.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultKey
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * The vault key lives encrypted under a Keystore key that requires a strong
 * biometric authentication and is invalidated when a new biometric is enrolled.
 *
 * The prompt is shown without a crypto object: the Keystore only accepts a
 * crypto object for strong sensors, and a plain prompt keeps the flow working
 * for every sensor the app supports. A successful prompt authorises the
 * Keystore key for [AUTH_VALIDITY_SECONDS], so the key is read right after the
 * prompt without asking the user twice.
 */
class AndroidBiometricVault(
    private val context: Context,
    private val activityProvider: ActivityProvider,
    private val storage: SecureStorage,
) : BiometricGate,
    SecureKeyStore {

    override suspend fun isAvailable(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

    override suspend fun authenticate(reason: String): Boolean {
        val activity = activityProvider.current() ?: return false

        return suspendCancellableCoroutine { continuation ->
            val prompt = AtomicReference<BiometricPrompt?>()
            continuation.invokeOnCancellation { prompt.get()?.cancelAuthentication() }

            // The prompt belongs to the main thread.
            Handler(Looper.getMainLooper()).post {
                if (!continuation.isActive) return@post
                val created = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(context),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            continuation.resume(true)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            continuation.resume(false)
                        }

                        // A rejected finger keeps the prompt open; the terminal callback is onAuthenticationError.
                    },
                )
                prompt.set(created)
                created.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(PROMPT_TITLE)
                        .setSubtitle(reason)
                        .setAllowedAuthenticators(BIOMETRIC_STRONG)
                        .setNegativeButtonText(PROMPT_CANCEL)
                        .build(),
                )
            }
        }
    }

    override suspend fun saveVaultKey(key: VaultKey) {
        // shortcut: the keystore key is authorised for AUTH_VALIDITY_SECONDS after the prompt,
        // so a save that comes later than that window throws; upgrade to a per-use key with a
        // crypto object when the use case may save without a fresh prompt.
        val cipher = encryptCipher()
        storage.write(VAULT_KEY, cipher.iv + cipher.doFinal(key.bytes))
    }

    override suspend fun loadVaultKey(): VaultKey? {
        val stored = storage.read(VAULT_KEY) ?: return null
        if (stored.size <= IV_SIZE) return null
        val iv = stored.copyOfRange(0, IV_SIZE)
        val ciphertext = stored.copyOfRange(IV_SIZE, stored.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            VaultKey.from(cipher.doFinal(ciphertext))
        } catch (ignored: KeyPermanentlyInvalidatedException) {
            // The biometric set changed: the key is dead, the password is the way in.
            dropVaultKey()
            null
        } catch (ignored: UnrecoverableKeyException) {
            dropVaultKey()
            null
        } catch (ignored: UserNotAuthenticatedException) {
            // The authorisation window expired: the caller has to authenticate again.
            null
        } catch (ignored: AEADBadTagException) {
            dropVaultKey()
            null
        }
    }

    override suspend fun clear() {
        storage.delete(VAULT_KEY)
    }

    private fun encryptCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        } catch (ignored: KeyPermanentlyInvalidatedException) {
            // The biometric set changed: the old key is dead, a new one takes its place.
            dropKeyAlias()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        }
        return cipher
    }

    /** Forgets the vault key: the stored ciphertext and the keystore key it belongs to. */
    private suspend fun dropVaultKey() {
        storage.delete(VAULT_KEY)
        dropKeyAlias()
    }

    private fun dropKeyAlias() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setUserAuthenticationParameters(AUTH_VALIDITY_SECONDS, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "lockruna.vault"
        const val VAULT_KEY = "vault_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val TAG_SIZE_BITS = 128
        const val IV_SIZE = 12
        const val AUTH_VALIDITY_SECONDS = 300
        const val PROMPT_TITLE = "PassKeeper"
        const val PROMPT_CANCEL = "Cancel"
    }
}
