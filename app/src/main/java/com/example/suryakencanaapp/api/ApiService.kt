package com.example.suryakencanaapp.api

import com.example.suryakencanaapp.model.Admin
import com.example.suryakencanaapp.model.Client
import com.example.suryakencanaapp.model.Contact
import com.example.suryakencanaapp.model.Dashboard
import com.example.suryakencanaapp.model.Hero
import com.example.suryakencanaapp.model.History
import com.example.suryakencanaapp.model.LoginRequest
import com.example.suryakencanaapp.model.LoginResponse
import com.example.suryakencanaapp.model.Product
import com.example.suryakencanaapp.model.SiteSetting
import com.example.suryakencanaapp.model.Testimoni
import com.example.suryakencanaapp.model.VisiMisi
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @Headers("Accept: application/json")
    @POST("login")
    // Kata kunci 'suspend' memungkinkan fungsi ini dipanggil di background thread (Coroutines)
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("dashboard") // Ganti sesuai route baru di Laravel Anda
    suspend fun getDashboardData(): Response<Dashboard>

    @GET("product")
    suspend fun getProducts(
        @Query("search") search: String? = null
    ): Response<List<Product>>

    @GET("product/{id}")
    suspend fun getProductDetail(
        @Path("id") id: Int
    ): Response<Product>

    @Multipart
    @POST("product")
    suspend fun addProduct(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part("hide_price") hidePrice: RequestBody, // Kirim "1" atau "0"

        // Sesuaikan nama field dengan validasi Laravel: "image_path"
        @Part file: MultipartBody.Part,

        // Sesuaikan nama field dengan loop Laravel: "images[]"
        @Part albumImages: List<MultipartBody.Part>?
    ): Response<Any>

    @Multipart
    @POST("product/{id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part("hide_price") hidePrice: RequestBody,

        // Gambar Utama (image_path)
        @Part file: MultipartBody.Part?,

        // Album Baru (images[])
        @Part newImages: List<MultipartBody.Part>?,

        // Album Dihapus (deleted_images[])
        // Perhatikan tipe datanya, ini mengirim string path
        @Part deletedImages: List<MultipartBody.Part>?,

        @Part("_method") method: RequestBody // PUT
    ): Response<Any>

    @DELETE("product/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Void>

    @GET("testimonial")
    suspend fun getTestimonies(
        @Query("search") search: String? = null
    ): Response<List<Testimoni>>
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("testimonial")
    suspend fun addTestimoni(
        @Header("Authorization") token: String,
        @Field("client_name") clientName: String,
        @Field("institution") institution: String,
        @Field("feedback") feedback: String,
        @Field("date") date: String
    ): Response<Any>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @PUT("testimonial/{id}")
    suspend fun updateTestimoni(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Field("client_name") clientName: String,
        @Field("institution") institution: String,
        @Field("feedback") feedback: String,
        @Field("date") date: String
    ): Response<Any>

    @DELETE("testimonial/{id}")
    suspend fun deleteTestimoni(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>


    //VISI MISI
    @GET("vision-mission")
    suspend fun getVisiMisi(): Response<List<VisiMisi>>

    // Simpan Perubahan (Update)
    @FormUrlEncoded
    @PUT("vision-mission") // Sesuaikan dengan route Laravel Anda
    suspend fun updateVisiMisi(
        @Header("Authorization") token: String,
        @Field("vision") vision: String,
        @Field("mission") mission: String // Kirim sebagai string gabungan
    ): Response<Any>


    //CLIENT
    // 1. GET CLIENT
    @GET("our-client") // Sesuaikan dengan route Laravel (misal: Route::get('/client', ...))
    suspend fun getClients(
        @Query("search") search: String? = null
    ): Response<List<Client>>

    // 2. POST CLIENT (Upload Gambar - Multipart)
    @Headers("Accept: application/json")
    @Multipart
    @POST("our-client")
    suspend fun addClient(
        @Header("Authorization") token: String,
        @Part("client_name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Any>

    @Headers("Accept: application/json")
    @Multipart
    @POST("our-client/{id}") // Gunakan POST, bukan PUT untuk Multipart
    suspend fun updateClient(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("client_name") name: RequestBody,
        @Part file: MultipartBody.Part?, // Boleh Null (jika tidak ganti gambar)
        @Part("_method") method: RequestBody // Wajib isi "PUT"
    ): Response<Any>

    // 3. DELETE CLIENT
    @DELETE("our-client/{id}")
    suspend fun deleteClient(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>


    //Riwayat Perusahaan
    // 1. GET HISTORY
    @GET("company-history")
    suspend fun getHistories(): Response<List<History>>

    @GET("company-history/{id}")
    suspend fun getHistoryDetail(
        @Path("id") id: Int
    ): Response<History>

    // 2. ADD HISTORY
    @Multipart
    @POST("company-history")
    suspend fun addHistory(
        @Header("Authorization") token: String,
        @Part("tahun") tahun: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part file: MultipartBody.Part?, // Boleh null (Nullable di Controller)
        @Part albumImages: List<MultipartBody.Part>?
    ): Response<Any>

    // 3. UPDATE HISTORY (Pakai Trik _method = PUT)
    @Multipart
    @POST("company-history/{id}")
    suspend fun updateHistory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("tahun") tahun: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part file: MultipartBody.Part?,
        @Part newImages: List<MultipartBody.Part>?, // Tambah Album
        @Part deletedImages: List<MultipartBody.Part>?, // Hapus Album
        @Part("_method") method: RequestBody
    ): Response<Any>

    // 4. DELETE HISTORY
    @DELETE("company-history/{id}")
    suspend fun deleteHistory(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    @GET("contact")
    suspend fun getContact(): Response<List<Contact>>

    // 2. UPDATE DATA KONTAK
    @FormUrlEncoded
    @PUT("contact") // Sesuaikan dengan route Laravel (misal: /contact atau /contact/update)
    suspend fun updateContact(
        @Header("Authorization") token: String,
        @Field("email") email: String,
        @Field("phone") phone: String,
        @Field("address") address: String,
        @Field("map_url") mapUrl: String
    ): Response<Any>

    // 1. GET HERO
    @GET("hero")
    suspend fun getHero(): Response<List<Hero>>

    // 2. UPDATE HERO (Text, Upload Image, Delete Image jadi satu)
    @Multipart
    @POST("hero") // Pakai POST karena Multipart (walaupun logicnya Update)
    suspend fun updateHero(
        @Header("Authorization") token: String,

        // Data Teks (Nullable semua)
        @Part("location") location: RequestBody?,
        @Part("title") title: RequestBody?,
        @Part("machines") machines: RequestBody?,
        @Part("clients") clients: RequestBody?,
        @Part("customers") customers: RequestBody?,
        @Part("experience_years") experienceYears: RequestBody?,
        @Part("trust_years") trustYears: RequestBody?,

        // Upload Gambar Baru (Array)
        @Part newImages: List<MultipartBody.Part>?,

        // Hapus Gambar Lama (Array String Path)
        // Kita kirim sebagai field text array: deleted_backgrounds[0], deleted_backgrounds[1]...
        @Part deletedImages: List<MultipartBody.Part>?
    ): Response<Any>

    // 1. GET SETTINGS
    @GET("site-settings") // Sesuaikan route Laravel
    suspend fun getSiteSettings(): Response<SiteSetting>
    // Perhatikan: Controller 'index' mengembalikan Object { ... }, bukan Array [ ... ]

    // 2. UPDATE SETTINGS
    @Multipart
    @POST("site-settings") // Route Update
    suspend fun updateSiteSettings(
        @Header("Authorization") token: String,

        // Data Teks (Semua Nullable)
        @Part("company_name") companyName: RequestBody?,
        @Part("hero_title") heroTitle: RequestBody?,
        @Part("hero_subtitle") heroSubtitle: RequestBody?,
        @Part("visi_misi_label") visionLabel: RequestBody?,
        @Part("visi_misi_title") visionTitle: RequestBody?,
        @Part("produk_label") productLabel: RequestBody?,
        @Part("produk_title") productTitle: RequestBody?,
        @Part("clients_label") clientLabel: RequestBody?,
        @Part("clients_title") clientTitle: RequestBody?,
        @Part("riwayat_label") historyLabel: RequestBody?,
        @Part("riwayat_title") historyTitle: RequestBody?,
        @Part("testimoni_label") testiLabel: RequestBody?,
        @Part("testimoni_title") testiTitle: RequestBody?,
        @Part("kontak_label") contactLabel: RequestBody?,
        @Part("kontak_title") contactTitle: RequestBody?,

        // File Logo (Optional)
        @Part companyLogo: MultipartBody.Part?,

        // Method Spoofing
        @Part("_method") method: RequestBody
    ): Response<Any>

    @GET("admin")
    suspend fun getAdmins(
        @Header("Authorization") token: String,
        @Query("search") search: String? = null
    ): Response<List<Admin>>

    // 2. ADD ADMIN
    @FormUrlEncoded
    @POST("admin")
    suspend fun addAdmin(
        @Header("Authorization") token: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<Any>

    @DELETE("admin/{id}")
    suspend fun deleteAdmin(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Any>

    @Multipart
    @POST("admin/{id}") // Laravel Update via POST + _method=PUT
    suspend fun updateAdmin(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("username") username: RequestBody,
        @Part password: MultipartBody.Part?,
        @Part("_method") method: RequestBody
    ): Response<Any>

}