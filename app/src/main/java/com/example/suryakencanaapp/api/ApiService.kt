package com.example.suryakencanaapp.api

import com.example.suryakencanaapp.model.DashboardSummary
import com.example.suryakencanaapp.model.LoginRequest
import com.example.suryakencanaapp.model.LoginResponse
import com.example.suryakencanaapp.model.Product
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Headers("Accept: application/json")
    @POST("login")
    // Kata kunci 'suspend' memungkinkan fungsi ini dipanggil di background thread (Coroutines)
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("dashboard-summary")
    suspend fun getDashboardSummary(): Response<DashboardSummary>
    @GET("products/recent")
    suspend fun getRecentProducts(): Response<List<Product>>

    @GET("product")
    suspend fun getProducts(
        @Query("search") search: String? = null
    ): Response<List<Product>>

    @Multipart
    @POST("api/products")
    suspend fun addProduct(
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,

        // PENTING: Parameter ini menangkap file gambar
        @Part file: MultipartBody.Part
    ): Response<Any>

    // DELETE PRODUK
    @DELETE("product/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Int
    ): Response<Void>
}