package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class History(
    val id: Int,
    val tahun: Int,
    val judul: String,
    val deskripsi: String,

    @SerializedName("image_url")
    val imageUrl: String?, // Gambar Utama (URL)

    @SerializedName("image_path")
    val imagePath: String?, // Gambar Utama (Path - untuk delete jika perlu)

    // --- BAGIAN ALBUM ---
    @SerializedName("images")
    val images: List<String>?, // Path Relatif (Disimpan untuk fitur DELETE)

    @SerializedName("image_urls")
    val imageUrls: List<String>? // URL Lengkap (Dipakai untuk TAMPIL/PREVIEW)
)