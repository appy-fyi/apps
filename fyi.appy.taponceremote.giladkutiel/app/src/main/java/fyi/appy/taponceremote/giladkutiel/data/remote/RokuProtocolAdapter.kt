package fyi.appy.taponceremote.giladkutiel.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Roku External Control Protocol adapter. `baseUrl` is e.g. "http://192.168.1.20:8060".
 * ECP key names match [RemoteCommand.name] exactly for every command this app sends.
 */
class RokuProtocolAdapter(
    private val baseUrl: String,
    private val client: OkHttpClient = defaultClient,
) : RemoteProtocolAdapter {

    override suspend fun send(command: RemoteCommand): CommandResult =
        postKeypress(command.name)

    override suspend fun sendText(text: String): CommandResult = withContext(Dispatchers.IO) {
        val codePoints = text.codePoints().toArray()
        for (codePoint in codePoints) {
            val char = String(Character.toChars(codePoint))
            val encoded = URLEncoder.encode(char, "UTF-8")
            when (val result = postKeypressBlocking("Lit_$encoded")) {
                is CommandResult.Success -> delay(40)
                else -> return@withContext result
            }
        }
        CommandResult.Success
    }

    private suspend fun postKeypress(key: String): CommandResult = withContext(Dispatchers.IO) {
        postKeypressBlocking(key)
    }

    private fun postKeypressBlocking(key: String): CommandResult {
        val request = Request.Builder()
            .url("$baseUrl/keypress/$key")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) CommandResult.Success
                else CommandResult.Failure("HTTP ${response.code}")
            }
        } catch (e: IOException) {
            CommandResult.Failure(e.message ?: "network error")
        }
    }

    companion object {
        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        suspend fun probeDeviceInfo(baseUrl: String, client: OkHttpClient = defaultClient): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url("$baseUrl/query/device-info").get().build()
                    client.newCall(request).execute().use { it.isSuccessful }
                } catch (e: IOException) {
                    false
                }
            }
    }
}
