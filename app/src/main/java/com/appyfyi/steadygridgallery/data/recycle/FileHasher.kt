package com.appyfyi.steadygridgallery.data.recycle

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

data class CopyResult(val sha256Hex: String, val sizeBytes: Long)

/**
 * Copies [input] to [destination] while computing a SHA-256 digest over the bytes written,
 * so the digest reflects exactly what landed on disk (not what was read from the source).
 */
object FileHasher {

    fun copyWithSha256(input: InputStream, destination: File): CopyResult {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        destination.outputStream().use { out: OutputStream ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                out.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                totalBytes += read
            }
            out.flush()
        }
        return CopyResult(sha256Hex = digest.digest().toHexString(), sizeBytes = totalBytes)
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexString()
    }

    /** Re-reads [file] from disk and compares its hash and size against the values recorded at copy time. */
    fun verifyCopy(file: File, expectedSha256Hex: String, expectedSizeBytes: Long): Boolean {
        if (!file.exists()) return false
        if (file.length() != expectedSizeBytes) return false
        return sha256(file) == expectedSha256Hex
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }
}
