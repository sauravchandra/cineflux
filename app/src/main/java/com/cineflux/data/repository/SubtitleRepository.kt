package com.cineflux.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun findAndDownloadSubtitle(movieTitle: String, videoFilePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = movieTitle.trim().replace(" ", "+")
                val url = "$BASE_URL/api/v1/subtitles?api_key=$API_KEY&film_name=$encoded&languages=en&type=movie"
                Log.i(TAG, "SubDL search: $movieTitle")

                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext null

                val json = JSONObject(body)
                val subtitles = json.optJSONArray("subtitles")
                if (subtitles == null || subtitles.length() == 0) {
                    Log.i(TAG, "No subtitles found")
                    return@withContext null
                }

                Log.i(TAG, "Found ${subtitles.length()} subtitles")

                var bestUrl: String? = null
                for (i in 0 until subtitles.length()) {
                    val sub = subtitles.getJSONObject(i)
                    val hi = sub.optBoolean("hi", false)
                    val subUrl = sub.optString("url", "")
                    if (subUrl.isNotBlank() && !hi) {
                        bestUrl = subUrl
                        Log.i(TAG, "Selected: ${sub.optString("release_name", "")}")
                        break
                    }
                }
                if (bestUrl == null) {
                    bestUrl = subtitles.getJSONObject(0).optString("url", "")
                }
                if (bestUrl.isNullOrBlank()) return@withContext null

                val downloadUrl = "https://dl.subdl.com$bestUrl"
                downloadAndExtract(downloadUrl, videoFilePath)
            } catch (e: Exception) {
                Log.e(TAG, "Subtitle failed: ${e.javaClass.simpleName}: ${e.message}")
                null
            }
        }
    }

    private fun downloadAndExtract(zipUrl: String, videoFilePath: String): String? {
        return try {
            val request = Request.Builder().url(zipUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body ?: return null

            val videoFile = File(videoFilePath)
            val srtFile = File(videoFile.parentFile, videoFile.nameWithoutExtension + ".srt")

            ZipInputStream(responseBody.byteStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".srt", ignoreCase = true)) {
                        FileOutputStream(srtFile).use { out ->
                            zip.copyTo(out)
                        }
                        Log.i(TAG, "Subtitle saved: ${srtFile.absolutePath} (${srtFile.length()} bytes)")
                        return srtFile.absolutePath
                    }
                    entry = zip.nextEntry
                }
            }

            Log.w(TAG, "No .srt found in zip")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SubtitleRepo"
        private const val BASE_URL = "https://api.subdl.com"
        private val API_KEY = com.cineflux.BuildConfig.SUBDL_API_KEY
    }
}
