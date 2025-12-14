package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String,
    val token: String,
    @SerializedName("admin")
    val adminData: AdminData
)
data class AdminData(
    val id: Int,
    val username: String,
    val role: String
)
