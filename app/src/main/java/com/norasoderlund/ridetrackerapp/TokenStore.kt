package com.norasoderlund.ridetrackerapp

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class TokenStore {
    private val context: Context;
    private val queue: RequestQueue;

    constructor(context: Context) {
        this.context = context;

        queue = Volley.newRequestQueue(context);
    }

    internal fun deleteKey() {
        try {
            File(context.filesDir, "token.txt").delete();
        }
        catch(exception: Exception) {

        }
    }

    internal fun readKey(): String? {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

        val encryptedFile = EncryptedFile.Builder(
            File(context.filesDir, "token.txt"),
            context.applicationContext,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        try {
            val inputStream = encryptedFile.openFileInput()
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte: Int = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            val plaintext: ByteArray = byteArrayOutputStream.toByteArray()

            if(plaintext.isEmpty())
                return null;

            return plaintext.toString();
        }
        catch (exception: IOException) {
            return null;
        }
    }

    internal fun putKey(key: String) {
        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)

        // Create a file with this name or replace an entire existing file
        // that has the same name. Note that you cannot append to an existing file,
        // and the filename cannot contain path separators.
        val encryptedFile = EncryptedFile.Builder(
            File(context.filesDir, "token.txt"),
            context.applicationContext,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val fileContent = key.toByteArray(StandardCharsets.UTF_8);

        encryptedFile.openFileOutput().apply {
            write(fileContent)
            flush()
            close()
        }
    }
}