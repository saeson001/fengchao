package com.saeson.fengchao.data

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * v2c admin API 客户端。
 * - session cookie 由 CookieJar 保存
 * - 任一请求遇 401 且本地存有凭证时自动重登一次再重放
 */
object Api {
    private val JSON_MT = "application/json; charset=utf-8".toMediaType()
    private val main = Handler(Looper.getMainLooper())
    val gson = Gson()

    lateinit var http: OkHttpClient
        private set

    fun init() {
        http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .cookieJar(SessionCookieJar())
            .build()
    }

    class Resp(val code: Int, val body: String) {
        val ok: Boolean get() = code in 200..299
        val networkError: Boolean get() = code == -1
        fun json(): JsonObject? = try {
            JsonParser.parseString(body).asJsonObject
        } catch (e: Exception) {
            null
        }
        fun errMsg(fallback: String): String =
            json()?.get("error")?.asString ?: (if (body.isNotBlank() && body.length < 200) body else fallback)
    }

    private class SessionCookieJar : CookieJar {
        private var session: Cookie? = null
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            for (c in cookies) if (c.name == "vless2clash_session") session = c
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            session?.let { if (it.matches(url)) listOf(it) else emptyList() } ?: emptyList()

        fun clear() {
            session = null
        }
    }

    private val jar: SessionCookieJar get() = http.cookieJar as SessionCookieJar

    private fun url(path: String): String = Config.baseUrl.trimEnd('/') + path

    private fun enqueue(req: Request, cb: (Resp) -> Unit, retry: Boolean) {
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                main.post { cb(Resp(-1, "网络错误：${e.message ?: e.javaClass.simpleName}")) }
            }

            override fun onResponse(call: Call, resp: Response) {
                val code = resp.code
                val body = try { resp.body?.string() ?: "" } catch (e: Exception) { "" }
                resp.close()
                if (code == 401 && retry && Config.hasCredentials()) {
                    login(Config.username.orEmpty(), Config.password.orEmpty()) { ok, _ ->
                        if (ok) {
                            val req2 = req.newBuilder().build()
                            enqueue(req2, cb, retry = false)
                        } else {
                            main.post { cb(Resp(401, "登录已过期，请重新登录")) }
                        }
                    }
                    return
                }
                main.post { cb(Resp(code, body)) }
            }
        })
    }

    private fun request(method: String, path: String, jsonBody: String?, cb: (Resp) -> Unit) {
        val body: RequestBody? = jsonBody?.toRequestBody(JSON_MT)
        val b = Request.Builder().url(url(path))
        when (method) {
            "GET" -> b.get()
            "DELETE" -> b.delete()
            "PUT" -> b.put(body ?: "".toRequestBody(JSON_MT))
            else -> b.post(body ?: "".toRequestBody(JSON_MT))
        }
        enqueue(b.build(), cb, retry = true)
    }

    // ---- 鉴权 ----

    fun login(user: String, pass: String, cb: (ok: Boolean, msg: String) -> Unit) {
        val body = gson.toJson(mapOf("username" to user, "password" to pass)).toRequestBody(JSON_MT)
        val req = Request.Builder().url(url("/api/admin/login")).post(body).build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                main.post { cb(false, "网络错误：${e.message ?: e.javaClass.simpleName}") }
            }

            override fun onResponse(call: Call, resp: Response) {
                val code = resp.code
                val text = try { resp.body?.string() ?: "" } catch (e: Exception) { "" }
                resp.close()
                val ok = code == 200 && try {
                    JsonParser.parseString(text).asJsonObject.get("success")?.asBoolean == true
                } catch (e: Exception) { false }
                main.post {
                    if (ok) cb(true, "登录成功")
                    else {
                        jar.clear()
                        val em = try {
                            JsonParser.parseString(text).asJsonObject.get("error")?.asString
                        } catch (e: Exception) { null }
                        cb(false, em ?: (if (code == 429) "尝试过于频繁，请 60 秒后再试" else "用户名或密码错误"))
                    }
                }
            }
        })
    }

    fun logout(cb: (Resp) -> Unit) {
        request("POST", "/api/admin/logout", null) { r ->
            jar.clear()
            cb(r)
        }
    }

    fun check(cb: (logged: Boolean) -> Unit) {
        request("GET", "/api/admin/check", null) { r ->
            cb(r.ok && r.json()?.get("logged_in")?.asBoolean == true)
        }
    }

    // ---- 订阅记录 ----

    fun records(page: Int, perPage: Int, search: String, cb: (Resp) -> Unit) {
        val q = StringBuilder("/api/admin/records?page=").append(page)
            .append("&per_page=").append(perPage)
        if (search.isNotBlank()) q.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"))
        request("GET", q.toString(), null, cb)
    }

    fun recordDetail(id: Long, cb: (Resp) -> Unit) {
        request("GET", "/api/admin/records/$id", null, cb)
    }

    fun deleteRecord(id: Long, cb: (Resp) -> Unit) {
        request("DELETE", "/api/admin/records/$id", null, cb)
    }

    fun refreshRecord(id: Long, cb: (Resp) -> Unit) {
        request("POST", "/api/admin/records/$id/refresh", "{}", cb)
    }

    fun refreshAll(cb: (Resp) -> Unit) {
        request("POST", "/api/admin/records/refresh-all", "{}", cb)
    }

    /** 极简新建：http(s) 链接进 subscriptions，其余（vless:// 等）进 links */
    fun convert(input: String, name: String, cb: (Resp) -> Unit) {
        val trimmed = input.trim()
        val obj = JsonObject()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            obj.addProperty("subscriptions", trimmed)
        } else {
            obj.addProperty("links", trimmed)
        }
        if (name.isNotBlank()) obj.addProperty("config_name", name.trim())
        request("POST", "/api/convert", gson.toJson(obj), cb)
    }

    // ---- 看板 / 全局 ----

    fun panelClients(cb: (Resp) -> Unit) {
        request("POST", "/api/admin/panel-clients", "{}", cb)
    }

    /** 返回原始 global config JSON（含 vps_traffic_sources） */
    fun globalConfig(cb: (Resp) -> Unit) {
        request("GET", "/api/admin/global-config", null, cb)
    }

    fun parseJson(r: Resp): JsonObject? = r.json()
}
