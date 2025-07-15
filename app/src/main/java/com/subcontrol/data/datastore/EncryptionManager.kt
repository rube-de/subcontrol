package com.subcontrol.data.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages AES-256-GCM encryption for app data.
 * 
 * This class handles encryption and decryption of sensitive data using
 * the Android Keystore for secure key management.
 */
object EncryptionManager {

    private const val KEY_ALIAS = "SubControlDataKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16

    /**
     * Encrypts data using AES-256-GCM.
     * 
     * @param data Data to encrypt
     * @return Encrypted data with IV prepended
     * @throws Exception if encryption fails
     */
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Prepend IV to encrypted data
        return ByteBuffer.allocate(iv.size + encryptedData.size)
            .put(iv)
            .put(encryptedData)
            .array()
    }

    /**
     * Decrypts data using AES-256-GCM.
     * 
     * @param encryptedDataWithIv Encrypted data with IV prepended
     * @return Decrypted data
     * @throws Exception if decryption fails
     */
    fun decrypt(encryptedDataWithIv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        
        // Extract IV and encrypted data
        val buffer = ByteBuffer.wrap(encryptedDataWithIv)
        val iv = ByteArray(GCM_IV_LENGTH)
        buffer.get(iv)
        val encryptedData = ByteArray(buffer.remaining())
        buffer.get(encryptedData)
        
        // Initialize cipher with IV
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
        
        return cipher.doFinal(encryptedData)
    }

    /**
     * Gets or creates the secret key for encryption.
     * 
     * @return SecretKey instance
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        // Check if key already exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set to true if you want to require device authentication
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the encryption key from the keystore.
     * This should only be used when resetting the app or for security purposes.
     * 
     * @return true if key was deleted successfully
     */
    fun deleteKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            keyStore.deleteEntry(KEY_ALIAS)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the encryption key exists.
     * 
     * @return true if key exists
     */
    fun keyExists(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
}