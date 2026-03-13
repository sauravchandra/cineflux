package com.cineflux.di

import com.cineflux.data.api.PirateBayApi
import com.cineflux.data.api.SubtitleApi
import com.cineflux.data.api.TmdbApi
import com.cineflux.data.api.YtsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val dohDns = object : Dns {
        private val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        private val ipPattern = Regex("\"data\"\\s*:\\s*\"([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)\"")

        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                resolveViaDoH(hostname)
            } catch (_: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }

        private fun resolveViaDoH(hostname: String): List<InetAddress> {
            val request = okhttp3.Request.Builder()
                .url("https://1.1.1.1/dns-query?name=$hostname&type=A")
                .header("Accept", "application/dns-json")
                .build()
            val body = bootstrapClient.newCall(request).execute().body?.string()
                ?: throw java.net.UnknownHostException(hostname)
            val ips = ipPattern.findAll(body)
                .map { InetAddress.getByName(it.groupValues[1]) }
                .toList()
            if (ips.isEmpty()) throw java.net.UnknownHostException(hostname)
            return ips
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dohDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @Named("yts")
    fun provideYtsRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(YtsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(@Named("tmdb") retrofit: Retrofit): TmdbApi {
        return retrofit.create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYtsApi(@Named("yts") retrofit: Retrofit): YtsApi {
        return retrofit.create(YtsApi::class.java)
    }

    @Provides
    @Singleton
    @Named("tpb")
    fun providePirateBayRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PirateBayApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePirateBayApi(@Named("tpb") retrofit: Retrofit): PirateBayApi {
        return retrofit.create(PirateBayApi::class.java)
    }

    @Provides
    @Singleton
    @Named("subtitle")
    fun provideSubtitleRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SubtitleApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideSubtitleApi(@Named("subtitle") retrofit: Retrofit): SubtitleApi {
        return retrofit.create(SubtitleApi::class.java)
    }
}
