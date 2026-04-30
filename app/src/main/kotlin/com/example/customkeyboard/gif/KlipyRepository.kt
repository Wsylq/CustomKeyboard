package com.example.customkeyboard.gif

import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Fetches GIF data from the Klipy API.
 *
 * Endpoints used:
 *   Trending: GET https://api.klipy.com/api/v1/{key}/gifs/trending?per_page=24&page={page}
 *   Search:   GET https://api.klipy.com/api/v1/{key}/gifs/search?q={q}&per_page=24&page={page}
 *
 * Response shape:
 *   { "result": true, "data": { "data": [ { "files": { ... } }, ... ], "has_next": true } }
 *
 * Each GIF item has a "files" object. We prefer "gif" > "webp" > "mp4" preview URLs.
 * The "preview" sub-object inside each format gives a smaller thumbnail URL.
 */
object KlipyRepository {

    // NOTE: Regenerate this key in your Klipy dashboard — it was exposed in chat.
    private const val API_KEY = "pf0JUkR9VyZKZxzqG30QgxRawhUxVL6PetvCdHrW7A5bXSN2New8NMyB5Il7vqqi"
    private const val BASE    = "https://api.klipy.com/api/v1/$API_KEY/gifs"
    private const val PER_PAGE = 24

    /** Prefer IPv4 addresses to avoid IPv6 connectivity failures on some devices/networks. */
    private val ipv4Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            // Put IPv4 addresses first; fall back to IPv6 if no IPv4 available
            val ipv4 = addresses.filterIsInstance<Inet4Address>()
            return if (ipv4.isNotEmpty()) ipv4 else addresses
        }
    }

    private val client = OkHttpClient.Builder()
        .dns(ipv4Dns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class GifItem(
        val id: String,
        /** URL of the small preview (thumbnail) — used in the grid. */
        val previewUrl: String,
        /** URL of the full GIF — committed to the input field on tap. */
        val fullUrl: String,
        val width: Int,
        val height: Int
    )

    data class GifPage(
        val items: List<GifItem>,
        val hasNext: Boolean,
        val error: String? = null
    )

    /** Fetches trending GIFs. Runs on the calling thread — call from a background thread. */
    fun trending(page: Int = 1): GifPage {
        val url = "$BASE/trending?per_page=$PER_PAGE&page=$page"
        return fetch(url)
    }

    /** Searches GIFs by query. Runs on the calling thread — call from a background thread. */
    fun search(query: String, page: Int = 1): GifPage {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$BASE/search?q=$encoded&per_page=$PER_PAGE&page=$page"
        return fetch(url)
    }

    private fun fetch(url: String): GifPage {
        return try {
            Log.d("KlipyRepo", "Fetching: $url")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val msg = "HTTP ${response.code}"
                Log.e("KlipyRepo", "$msg for $url")
                return GifPage(emptyList(), false, error = msg)
            }
            val body = response.body?.string()
                ?: return GifPage(emptyList(), false, error = "Empty response")
            parse(body)
        } catch (e: IOException) {
            Log.e("KlipyRepo", "Network error: ${e.message}")
            GifPage(emptyList(), false, error = "Network error: ${e.message}")
        }
    }

    private fun parse(json: String): GifPage {
        return try {
            val root     = JSONObject(json)
            val dataObj  = root.getJSONObject("data")
            val array    = dataObj.getJSONArray("data")
            val hasNext  = dataObj.optBoolean("has_next", false)
            val items    = mutableListOf<GifItem>()

            for (i in 0 until array.length()) {
                val item  = array.getJSONObject(i)
                val id    = item.optString("id", i.toString())
                val files = item.optJSONObject("files") ?: continue

                // Pick best available format: gif > webp > mp4
                val formatObj = files.optJSONObject("gif")
                    ?: files.optJSONObject("webp")
                    ?: files.optJSONObject("mp4")
                    ?: continue

                val fullUrl    = formatObj.optString("url").takeIf { it.isNotBlank() } ?: continue
                val previewObj = formatObj.optJSONObject("preview")
                val previewUrl = previewObj?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: fullUrl   // fall back to full URL if no preview

                val width  = formatObj.optInt("width", 200)
                val height = formatObj.optInt("height", 200)

                items.add(GifItem(id, previewUrl, fullUrl, width, height))
            }
            GifPage(items, hasNext)
        } catch (e: Exception) {
            Log.e("KlipyRepo", "Parse error: ${e.message}")
            GifPage(emptyList(), false, error = "Parse error: ${e.message}")
        }
    }
}
