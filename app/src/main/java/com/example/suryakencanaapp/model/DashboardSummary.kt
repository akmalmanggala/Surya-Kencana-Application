package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("total_products")
    val totalProducts: Int,

    @SerializedName("total_clients")
    val totalClients: Int,

    @SerializedName("total_testimony")
    val totalTestimony: Int,

    @SerializedName("total_admins")
    val totalAdmins: Int
)