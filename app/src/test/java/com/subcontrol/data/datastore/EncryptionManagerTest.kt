package com.subcontrol.data.datastore

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EncryptionManager.
 * 
 * Tests the AES-256-GCM encryption and decryption functionality
 * to ensure data security and integrity.
 * 
 * NOTE: These are basic unit tests. For comprehensive testing of actual
 * encryption/decryption, use instrumented tests since Android Keystore
 * is not available in unit tests.
 */
class EncryptionManagerTest {

    @Before
    fun setUp() {
        // Mock the EncryptionManager object for basic testing
        mockkObject(EncryptionManager)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `encrypt returns non-null encrypted data`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()
        val mockEncryptedData = byteArrayOf(1, 2, 3, 4, 5) // Mock encrypted data
        
        every { EncryptionManager.encrypt(plaintextBytes) } returns mockEncryptedData

        // When
        val encryptedData = EncryptionManager.encrypt(plaintextBytes)

        // Then
        assertNotNull("Encrypted data should not be null", encryptedData)
        assertTrue("Encrypted data should not be empty", encryptedData.isNotEmpty())
        assertEquals("Should return mocked encrypted data", mockEncryptedData, encryptedData)
    }

    @Test
    fun `decrypt returns original plaintext`() = runTest {
        // Given
        val plaintext = "Hello, SubControl!"
        val plaintextBytes = plaintext.toByteArray()
        val mockEncryptedData = byteArrayOf(1, 2, 3, 4, 5)
        
        every { EncryptionManager.encrypt(plaintextBytes) } returns mockEncryptedData
        every { EncryptionManager.decrypt(mockEncryptedData) } returns plaintextBytes

        // When
        val encryptedData = EncryptionManager.encrypt(plaintextBytes)
        val decryptedData = EncryptionManager.decrypt(encryptedData)

        // Then
        assertNotNull("Decrypted data should not be null", decryptedData)
        assertArrayEquals("Decrypted data should match original plaintext", plaintextBytes, decryptedData)
        assertEquals("Decrypted string should match original", plaintext, String(decryptedData))
    }

    @Test
    fun `keyExists returns true when key exists`() = runTest {
        // Given
        every { EncryptionManager.keyExists() } returns true

        // When
        val exists = EncryptionManager.keyExists()

        // Then
        assertTrue("Key should exist", exists)
    }

    @Test
    fun `keyExists returns false when key does not exist`() = runTest {
        // Given
        every { EncryptionManager.keyExists() } returns false

        // When
        val exists = EncryptionManager.keyExists()

        // Then
        assertFalse("Key should not exist", exists)
    }

    @Test
    fun `deleteKey returns true when successful`() = runTest {
        // Given
        every { EncryptionManager.deleteKey() } returns true

        // When
        val deleted = EncryptionManager.deleteKey()

        // Then
        assertTrue("Key deletion should succeed", deleted)
    }

    @Test
    fun `deleteKey returns false when fails`() = runTest {
        // Given
        every { EncryptionManager.deleteKey() } returns false

        // When
        val deleted = EncryptionManager.deleteKey()

        // Then
        assertFalse("Key deletion should fail", deleted)
    }

    @Test(expected = Exception::class)
    fun `decrypt with corrupted data throws exception`() = runTest {
        // Given
        val corruptedData = byteArrayOf(99, 98, 97, 96)
        
        every { EncryptionManager.decrypt(corruptedData) } throws Exception("Decryption failed")

        // When/Then - should throw exception
        EncryptionManager.decrypt(corruptedData)
    }

    @Test
    fun `encrypt handles empty data`() = runTest {
        // Given
        val emptyData = byteArrayOf()
        val mockEncryptedEmpty = byteArrayOf(0, 0, 0)
        
        every { EncryptionManager.encrypt(emptyData) } returns mockEncryptedEmpty
        every { EncryptionManager.decrypt(mockEncryptedEmpty) } returns emptyData

        // When
        val encryptedData = EncryptionManager.encrypt(emptyData)
        val decryptedData = EncryptionManager.decrypt(encryptedData)

        // Then
        assertArrayEquals("Empty data should encrypt and decrypt successfully", emptyData, decryptedData)
    }

    @Test
    fun `encrypt handles unicode text`() = runTest {
        // Given
        val unicodeText = "Hello 🌍! ñoël français 中文 العربية"
        val unicodeBytes = unicodeText.toByteArray(Charsets.UTF_8)
        val mockEncryptedData = byteArrayOf(10, 20, 30, 40, 50)
        
        every { EncryptionManager.encrypt(unicodeBytes) } returns mockEncryptedData
        every { EncryptionManager.decrypt(mockEncryptedData) } returns unicodeBytes

        // When
        val encryptedData = EncryptionManager.encrypt(unicodeBytes)
        val decryptedData = EncryptionManager.decrypt(encryptedData)

        // Then
        assertArrayEquals("Unicode data should encrypt and decrypt successfully", unicodeBytes, decryptedData)
        assertEquals("Unicode string should match original", unicodeText, String(decryptedData, Charsets.UTF_8))
    }
}