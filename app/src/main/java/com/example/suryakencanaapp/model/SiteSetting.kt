package com.example.suryakencanaapp.model

import com.google.gson.annotations.SerializedName

data class SiteSetting(
    @SerializedName("id") val id: Int,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("company_logo_url") val companyLogoUrl: String?, // URL Lengkap

    // Hero
    @SerializedName("hero_title") val heroTitle: String?,
    @SerializedName("hero_subtitle") val heroSubtitle: String?,

    // Visi Misi
    @SerializedName("visi_misi_label") val visionLabel: String?,
    @SerializedName("visi_misi_title") val visionTitle: String?,

    // Produk
    @SerializedName("produk_label") val productLabel: String?,
    @SerializedName("produk_title") val productTitle: String?,

    // Clients
    @SerializedName("clients_label") val clientLabel: String?,
    @SerializedName("clients_title") val clientTitle: String?,

    // History
    @SerializedName("riwayat_label") val historyLabel: String?,
    @SerializedName("riwayat_title") val historyTitle: String?,

    // Testimoni
    @SerializedName("testimoni_label") val testiLabel: String?,
    @SerializedName("testimoni_title") val testiTitle: String?,

    // Contact
    @SerializedName("kontak_label") val contactLabel: String?,
    @SerializedName("kontak_title") val contactTitle: String?
)