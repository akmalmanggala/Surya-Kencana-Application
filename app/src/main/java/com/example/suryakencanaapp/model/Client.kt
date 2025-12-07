package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Client(
    @SerializedName("id")
    val id: Int,

    @SerializedName("client_name")
    val clientName: String?,

    @SerializedName("institution")
    val institution: String?,

    @SerializedName("logo_url")
    val logoUrl: String? // Kita pakai yang ini karena link-nya sudah lengkap
)