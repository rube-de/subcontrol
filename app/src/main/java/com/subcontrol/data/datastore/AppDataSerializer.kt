package com.subcontrol.data.datastore

import androidx.datastore.core.Serializer
import com.subcontrol.data.model.proto.AppData
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer for AppData Protocol Buffer with encryption.
 * 
 * This serializer handles reading and writing of AppData proto messages
 * with AES-256-GCM encryption for enhanced security.
 */
object AppDataSerializer : Serializer<AppData> {

    override val defaultValue: AppData = AppData.getDefaultInstance()

    /**
     * Reads and decrypts AppData from the input stream.
     * 
     * @param input Input stream containing encrypted data
     * @return Decrypted AppData instance
     */
    override suspend fun readFrom(input: InputStream): AppData {
        return try {
            val encryptedData = input.readBytes()
            if (encryptedData.isEmpty()) {
                defaultValue
            } else {
                val decryptedData = EncryptionManager.decrypt(encryptedData)
                AppData.parseFrom(decryptedData)
            }
        } catch (exception: Exception) {
            // If decryption fails, return default instance and log error
            // In production, this might indicate corruption or key change
            defaultValue
        }
    }

    /**
     * Encrypts and writes AppData to the output stream.
     * 
     * @param t AppData instance to serialize
     * @param output Output stream to write encrypted data
     */
    override suspend fun writeTo(t: AppData, output: OutputStream) {
        try {
            val serializedData = t.toByteArray()
            val encryptedData = EncryptionManager.encrypt(serializedData)
            output.write(encryptedData)
        } catch (exception: Exception) {
            // Log error and rethrow - this is a critical failure
            throw exception
        }
    }
}