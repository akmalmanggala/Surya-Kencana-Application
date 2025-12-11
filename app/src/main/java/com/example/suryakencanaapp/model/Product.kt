package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

// HAPUS data class ProductResponse, kita tidak pakai lagi.

data class Product(
    val id: Int,
    val name: String,
    val description: String?,
    val price: String,

    @SerializedName("hide_price")
    val hidePrice: Int = 0, // 0 = false, 1 = true (dari API)

    @SerializedName("image_url")
    val imageUrl: String?, // URL Lengkap Gambar Utama

    @SerializedName("image_path")
    val imagePath: String?, // Path Relatif Gambar Utama (untuk logic delete)

    // --- BAGIAN ALBUM ---
    @SerializedName("images")
    val images: List<String>?, // Path Relatif (Disimpan untuk fitur DELETE)

    @SerializedName("image_urls")
    val imageUrls: List<String>? // URL Lengkap (Dipakai untuk TAMPIL/PREVIEW di Glide)
)