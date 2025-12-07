package com.example.suryakencanaapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit // PENTING: Jangan lupa import ini

object ApiClient {
    val instance: ApiService by lazy {

        // Logging tetap menggunakan gaya Anda
        val mHttpLoggingInterceptor = HttpLoggingInterceptor { msg ->
            android.util.Log.d("HTTP_LOG", msg)
        }.setLevel(HttpLoggingInterceptor.Level.BODY)

        val mOkHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(mHttpLoggingInterceptor)
            // --- TAMBAHAN SETTING TIMEOUT (60 Detik) ---
            .connectTimeout(60, TimeUnit.SECONDS) // Waktu tunggu nyambung
            .readTimeout(60, TimeUnit.SECONDS)    // Waktu tunggu respon server
            .writeTimeout(60, TimeUnit.SECONDS)   // Waktu tunggu upload data
            .build()

        val retrofit = Retrofit.Builder()
            // SAYA HAPUS 'api/' DI UJUNGNYA AGAR AMAN DARI ERROR 404
            .baseUrl("http://192.168.109.17:8000/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(mOkHttpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}