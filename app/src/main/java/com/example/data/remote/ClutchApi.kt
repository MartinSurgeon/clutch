package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** A failure the farmer can read. [code] is the server's machine code (or "offline"). */
class ApiException(val code: String, message: String, val status: Int = 0) : Exception(message)

data class ApiUser(val id: Long, val phone: String, val name: String)

data class AuthResult(val token: String, val user: ApiUser)

data class SyncResponse(
    val serverTime: Long,
    val more: Boolean,
    val accepted: Map<String, List<String>>,
    val changes: Map<String, List<JSONObject>>
)

/**
 * Talks to the PHP backend (backend/api). Plain HttpURLConnection + org.json, no extra libraries.
 */
class ClutchApi(private val baseUrl: () -> String) {

    suspend fun register(phone: String, name: String, password: String, device: String): AuthResult {
        val body = JSONObject().put("phone", phone).put("name", name).put("password", password).put("device", device)
        return parseAuth(request("POST", "register.php", body, null))
    }

    suspend fun login(phone: String, password: String, device: String): AuthResult {
        val body = JSONObject().put("phone", phone).put("password", password).put("device", device)
        return parseAuth(request("POST", "login.php", body, null))
    }

    suspend fun logout(token: String) {
        request("POST", "logout.php", JSONObject(), token)
    }

    /** Permanently deletes the account and its backup on the server. */
    suspend fun deleteAccount(token: String, password: String) {
        request("POST", "delete_account.php", JSONObject().put("password", password), token)
    }

    suspend fun sync(token: String, since: Long, changes: Map<String, List<JSONObject>>): SyncResponse {
        val changesJson = JSONObject()
        changes.forEach { (key, rows) -> changesJson.put(key, JSONArray(rows)) }
        val res = request("POST", "sync.php", JSONObject().put("since", since).put("changes", changesJson), token)

        fun stringLists(obj: JSONObject?): Map<String, List<String>> =
            obj?.keys()?.asSequence()?.associateWith { k ->
                val arr = obj.optJSONArray(k) ?: JSONArray()
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyMap()

        fun objectLists(obj: JSONObject?): Map<String, List<JSONObject>> =
            obj?.keys()?.asSequence()?.associateWith { k ->
                val arr = obj.optJSONArray(k) ?: JSONArray()
                (0 until arr.length()).map { arr.getJSONObject(it) }
            } ?: emptyMap()

        return SyncResponse(
            serverTime = res.getLong("serverTime"),
            more = res.optBoolean("more", false),
            accepted = stringLists(res.optJSONObject("accepted")),
            changes = objectLists(res.optJSONObject("changes"))
        )
    }

    private fun parseAuth(res: JSONObject): AuthResult {
        val u = res.getJSONObject("user")
        return AuthResult(
            token = res.getString("token"),
            user = ApiUser(u.getLong("id"), u.getString("phone"), u.getString("name"))
        )
    }

    private suspend fun request(method: String, path: String, body: JSONObject?, token: String?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = URL(baseUrl().trimEnd('/') + "/" + path)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                token?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                    // Many shared hosts (cPanel, LiteSpeed) drop Authorization; the server accepts this too
                    setRequestProperty("X-Clutch-Token", it)
                }
                if (body != null) doOutput = true
            }
            try {
                if (body != null) {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (status !in 200..299) {
                    val err = json?.optJSONObject("error")
                    throw ApiException(
                        code = err?.optString("code").orEmpty().ifBlank { "http_$status" },
                        message = err?.optString("message").orEmpty().ifBlank { "The backup server had a problem ($status). Please try again later." },
                        status = status
                    )
                }
                json ?: throw ApiException("bad_response", "The backup server sent an unexpected reply.", status)
            } catch (e: ApiException) {
                throw e
            } catch (e: UnknownHostException) {
                throw ApiException("offline", "No internet connection. Your records are safe on this phone.")
            } catch (e: SocketTimeoutException) {
                throw ApiException("offline", "The connection is too slow right now. Your records are safe on this phone.")
            } catch (e: IOException) {
                throw ApiException("offline", "Could not reach the backup server. Your records are safe on this phone.")
            } finally {
                conn.disconnect()
            }
        }
}
