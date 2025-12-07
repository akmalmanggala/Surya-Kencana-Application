package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Testimoni(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("client_name")
    val clientName: String?,

    @SerializedName("institution")
    val institution: String?,

    @SerializedName("feedback")
    val feedback: String?,

    @SerializedName("date")
    val date: String? // Format: 2001-07-27
)