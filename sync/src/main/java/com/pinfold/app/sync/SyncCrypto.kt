package com.pinfold.app.sync

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** PBKDF2-HMAC-SHA256 fallback, documented in README, until Argon2id is pinned. */
object SyncCrypto {
    private const val ITERATIONS = 310_000
    private const val SALT_BYTES = 16
    private const val GCM_BITS = 128
    private val ALPHABET = "ABCDFGHJKLMNPQRSTUVWXYZ23456789"

    data class Sealed(val salt: String, val iv: String, val ciphertext: String)

    fun newDek(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun recoveryPhrase(): String = buildString {
        repeat(12) { if (it > 0) append(' '); append(ALPHABET[SecureRandom().nextInt(ALPHABET.length)]) }
    }

    fun wrapDek(dek: ByteArray, passphrase: CharArray): Sealed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return aesSeal(dek, derive(passphrase, salt), "savelin:dek:v1")
            .copy(salt = salt.b64())
    }

    fun unwrapDek(sealed: Sealed, passphrase: CharArray): ByteArray =
        aesOpen(sealed.copy(salt = ""), derive(passphrase, sealed.salt.fromB64()), "savelin:dek:v1")

    fun encrypt(plain: ByteArray, dek: ByteArray, objectId: String, type: String, version: Int = 1): Sealed =
        aesSeal(plain, dek, "savelin:$type:v$version:$objectId")

    fun decrypt(sealed: Sealed, dek: ByteArray, objectId: String, type: String, version: Int = 1): ByteArray =
        aesOpen(sealed, dek, "savelin:$type:v$version:$objectId")

    private fun derive(passphrase: CharArray, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase, salt, ITERATIONS, 256))
            .encoded

    private fun aesSeal(plain: ByteArray, key: ByteArray, aad: String): Sealed {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_BITS, iv))
        cipher.updateAAD(aad.toByteArray())
        return Sealed("", iv.b64(), cipher.doFinal(plain).b64())
    }

    private fun aesOpen(sealed: Sealed, key: ByteArray, aad: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_BITS, sealed.iv.fromB64()))
        cipher.updateAAD(aad.toByteArray())
        return cipher.doFinal(sealed.ciphertext.fromB64())
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
