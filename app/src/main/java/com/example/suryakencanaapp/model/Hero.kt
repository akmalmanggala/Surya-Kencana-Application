package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Hero(
    @SerializedName("id") val id: Int,
    @SerializedName("location") val location: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("machines") val machines: Int?,
    @SerializedName("clients") val clients: Int?,
    @SerializedName("customers") val customers: Int?,
    @SerializedName("experience_years") val experienceYears: Int?,
    @SerializedName("trust_years") val trustYears: Int?,
    @SerializedName("background_urls") val backgroundUrls: List<String>?, // URL Lengkap
    @SerializedName("backgrounds") val backgroundPaths: List<String>? // Path (untuk delete)
)