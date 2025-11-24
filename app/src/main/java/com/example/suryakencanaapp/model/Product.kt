package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

// HAPUS data class ProductResponse, kita tidak pakai lagi.

data class Product(
    val id: Int,
    val name: String,
    val description: String?,

    // JSON: "20000000.00" (String)
    val price: String,

    // JSON: "image_url": "https://..." (Langsung pakai ini!)
    @SerializedName("image_url")
    val imageUrl: String?
)