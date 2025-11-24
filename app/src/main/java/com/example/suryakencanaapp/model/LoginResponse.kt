package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String,
    val token: String,
    val role: String?,
    @SerializedName("admin") // Pastikan ini match dengan JSON controller Laravel Anda
    val adminData: AdminData
)
data class AdminData(
    val id: Int,
    val username: String
)
