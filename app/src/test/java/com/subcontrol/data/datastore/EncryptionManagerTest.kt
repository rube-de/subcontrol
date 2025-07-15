package com.subcontrol.data.datastore

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Unit tests for EncryptionManager.
 * 
 * Tests the AES-256-GCM encryption and decryption functionality
 * to ensure data security and integrity.
 */
class EncryptionManagerTest {

    private lateinit var encryptionManager: EncryptionManager
    private lateinit var testKey: SecretKey

    @Before
    fun setUp() {
        // Generate a test key for consistent testing
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        testKey = keyGenerator.generateKey()
        
        encryptionManager = EncryptionManager()
    }

    @Test
    fun `encrypt returns non-null encrypted data`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()

        // When
        val encryptedData = encryptionManager.encrypt(plaintextBytes, testKey)

        // Then
        assertNotNull("Encrypted data should not be null", encryptedData)
        assertTrue("Encrypted data should not be empty", encryptedData.isNotEmpty())
        assertFalse("Encrypted data should not equal plaintext", encryptedData.contentEquals(plaintextBytes))
    }

    @Test
    fun `decrypt returns original plaintext`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()

        // When
        val encryptedData = encryptionManager.encrypt(plaintextBytes, testKey)
        val decryptedData = encryptionManager.decrypt(encryptedData, testKey)

        // Then
        assertNotNull("Decrypted data should not be null", decryptedData)
        assertArrayEquals("Decrypted data should match original plaintext", plaintextBytes, decryptedData)
        assertEquals("Decrypted string should match original", plaintext, String(decryptedData))
    }

    @Test
    fun `encrypt produces different output for same input`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()

        // When
        val encryptedData1 = encryptionManager.encrypt(plaintextBytes, testKey)
        val encryptedData2 = encryptionManager.encrypt(plaintextBytes, testKey)

        // Then
        assertFalse("Two encryptions of same data should produce different ciphertext", 
                    encryptedData1.contentEquals(encryptedData2))
    }

    @Test
    fun `encrypt and decrypt handles empty data`() = runTest {
        // Given
        val emptyData = byteArrayOf()

        // When
        val encryptedData = encryptionManager.encrypt(emptyData, testKey)
        val decryptedData = encryptionManager.decrypt(encryptedData, testKey)

        // Then
        assertArrayEquals("Empty data should encrypt and decrypt successfully", emptyData, decryptedData)
    }

    @Test
    fun `encrypt and decrypt handles large data`() = runTest {
        // Given
        val largeData = ByteArray(10000) { (it % 256).toByte() }

        // When
        val encryptedData = encryptionManager.encrypt(largeData, testKey)
        val decryptedData = encryptionManager.decrypt(encryptedData, testKey)

        // Then
        assertArrayEquals("Large data should encrypt and decrypt successfully", largeData, decryptedData)
    }

    @Test
    fun `encrypt and decrypt handles unicode text`() = runTest {
        // Given
        val unicodeText = "Hello 🌍! ñoël français 中文 العربية"
        val unicodeBytes = unicodeText.toByteArray(Charsets.UTF_8)

        // When
        val encryptedData = encryptionManager.encrypt(unicodeBytes, testKey)
        val decryptedData = encryptionManager.decrypt(encryptedData, testKey)

        // Then
        assertArrayEquals("Unicode data should encrypt and decrypt successfully", unicodeBytes, decryptedData)
        assertEquals("Unicode string should match original", unicodeText, String(decryptedData, Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun `decrypt with wrong key throws exception`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()
        val wrongKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        // When
        val encryptedData = encryptionManager.encrypt(plaintextBytes, testKey)
        
        // Then - should throw exception
        encryptionManager.decrypt(encryptedData, wrongKey)
    }

    @Test(expected = Exception::class)
    fun `decrypt with corrupted data throws exception`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()

        // When
        val encryptedData = encryptionManager.encrypt(plaintextBytes, testKey)
        val corruptedData = encryptedData.copyOf()
        corruptedData[corruptedData.size / 2] = (corruptedData[corruptedData.size / 2] + 1).toByte()

        // Then - should throw exception
        encryptionManager.decrypt(corruptedData, testKey)
    }

    @Test
    fun `generateKey creates valid AES key`() = runTest {
        // When
        val generatedKey = encryptionManager.generateKey()

        // Then
        assertNotNull("Generated key should not be null", generatedKey)
        assertEquals("Generated key should be AES", "AES", generatedKey.algorithm)
        assertEquals("Generated key should be 256 bits", 32, generatedKey.encoded.size)
    }

    @Test
    fun `generateKey creates different keys each time`() = runTest {
        // When
        val key1 = encryptionManager.generateKey()
        val key2 = encryptionManager.generateKey()

        // Then
        assertFalse("Generated keys should be different", key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun `encrypt and decrypt round trip with generated key`() = runTest {
        // Given
        val plaintext = "Test with generated key"
        val plaintextBytes = plaintext.toByteArray()
        val generatedKey = encryptionManager.generateKey()

        // When
        val encryptedData = encryptionManager.encrypt(plaintextBytes, generatedKey)
        val decryptedData = encryptionManager.decrypt(encryptedData, generatedKey)

        // Then
        assertArrayEquals("Data should survive round trip with generated key", plaintextBytes, decryptedData)
    }
}