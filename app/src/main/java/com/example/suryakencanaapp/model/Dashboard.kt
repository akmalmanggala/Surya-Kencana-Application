package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class Dashboard(
    @SerializedName("summary")
    val summary: DashboardSummary,

    @SerializedName("recent_products")
    val recentProducts: List<Product>,

    @SerializedName("recent_testimonials")
    val recentTestimonials: List<Testimoni>
)