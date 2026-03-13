package com.cineflux.data.api

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareBypass @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cookieCache = mutableMapOf<String, String>()
    private val resolvedHosts = mutableSetOf<String>()

    fun getInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val host = request.url.host

        val cachedCookies = cookieCache[host]
        val modifiedRequest = if (cachedCookies != null) {
            request.newBuilder()
                .header("Cookie", cachedCookies)
                .header("User-Agent", USER_AGENT)
                .build()
        } else {
            request.newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
        }

        val response = chain.proceed(modifiedRequest)

        if (response.code == 403 || response.code == 503) {
            val body = response.peekBody(1024).string()
            if (body.contains("challenge") || body.contains("cf_chl") || body.contains("Just a moment")) {
                response.close()
                Log.i(TAG, "Cloudflare challenge detected for $host, resolving...")
                val cookies = resolveChallenge(request.url.toString())
                if (cookies != null) {
                    cookieCache[host] = cookies
                    val retryRequest = request.newBuilder()
                        .header("Cookie", cookies)
                        .header("User-Agent", USER_AGENT)
                        .build()
                    return@Interceptor chain.proceed(retryRequest)
                }
            }
        }

        response
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveChallenge(url: String): String? {
        val latch = CountDownLatch(1)
        var resultCookies: String? = null

        Handler(Looper.getMainLooper()).post {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = USER_AGENT
                }

                webView.webViewClient = object : WebViewClient() {
                    private var resolved = false

                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        super.onPageFinished(view, loadedUrl)
                        val cookies = cookieManager.getCookie(loadedUrl ?: url)
                        if (cookies != null && cookies.contains("cf_clearance") && !resolved) {
                            resolved = true
                            resultCookies = cookies
                            Log.i(TAG, "Cloudflare resolved! Cookies: ${cookies.take(80)}...")
                            latch.countDown()
                            view?.destroy()
                        }
                    }
                }

                webView.loadUrl(url)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (resultCookies == null) {
                        Log.w(TAG, "Cloudflare resolution timed out")
                        latch.countDown()
                        webView.destroy()
                    }
                }, TIMEOUT_MS)
            } catch (e: Exception) {
                Log.e(TAG, "WebView error: ${e.message}")
                latch.countDown()
            }
        }

        latch.await(TIMEOUT_MS + 2000, TimeUnit.MILLISECONDS)
        return resultCookies
    }

    companion object {
        private const val TAG = "CloudflareBypass"
        private const val TIMEOUT_MS = 15000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
