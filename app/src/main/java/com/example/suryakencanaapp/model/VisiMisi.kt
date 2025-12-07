package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class VisiMisi(
    @SerializedName("id") val id: Int?,
    @SerializedName("vision") val vision: String?,
    @SerializedName("mission") val mission: String? // String panjang
)