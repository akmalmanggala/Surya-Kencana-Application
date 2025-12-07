package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Contact(
    @SerializedName("id") val id: Int,
    @SerializedName("address") val address: String?,
    @SerializedName("phone") val phone: String?, // Map ke WhatsApp
    @SerializedName("email") val email: String?,
    @SerializedName("map_url") val mapUrl: String?
)