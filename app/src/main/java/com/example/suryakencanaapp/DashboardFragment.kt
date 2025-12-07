package com.example.suryakencanaapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.RecentProdukAdapter
import com.example.suryakencanaapp.adapter.RecentTestiAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.model.Dashboard
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class DashboardFragment : Fragment() {

    // Variabel UI
    private lateinit var tvCountProduk: TextView
    private lateinit var tvCountKlien: TextView
    private lateinit var tvCountTesti: TextView
    private lateinit var tvCountAdmin: TextView
    private lateinit var rvRecentProducts: RecyclerView
    private lateinit var rvRecentTestimoni: RecyclerView
    private lateinit var recentProductAdapter: RecentProdukAdapter
    private lateinit var recentTestiAdapter: RecentTestiAdapter
    private lateinit var tvSeeAllProduct: TextView
    private lateinit var tvSeeAllTestimoni: TextView
    private lateinit var btnQuickProduct: MaterialCardView
    private lateinit var btnQuickClient: MaterialCardView
    private lateinit var btnQuickTestimony: MaterialCardView
    private lateinit var btnQuickVisiMisi: MaterialCardView
    private lateinit var btnQuickRiwayat: MaterialCardView
    private lateinit var btnQuickHero: MaterialCardView
    private lateinit var btnQuickContact: MaterialCardView
    private lateinit var btnQuickSitus: MaterialCardView
    private lateinit var btnQuickAdmin: MaterialCardView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var cachedDashboardData: Dashboard? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout XML
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. INISIALISASI VIEW
        initViews(view)

        checkUserRole()

        if (cachedDashboardData != null) {
            // JIKA ADA: Pakai data lama (Instant Load!)
            applyDataToView(cachedDashboardData!!)
        } else {
            // JIKA KOSONG: Baru ambil dari Internet
            fetchDashboardData()
        }

        swipeRefresh.setOnRefreshListener {
            // Paksa ambil data baru dari server
            fetchDashboardData()
        }

        // 2. SETUP NAVIGASI
        setupActions()
    }

    private fun initViews(view: View) {

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        // Summary Cards
        tvCountProduk = view.findViewById(R.id.tvCountProduk)
        tvCountKlien = view.findViewById(R.id.tvCountKlien)
        tvCountTesti = view.findViewById(R.id.tvCountTesti)
        tvCountAdmin = view.findViewById(R.id.tvCountAdmin)

        // Recent Products List
        rvRecentProducts = view.findViewById(R.id.rvRecentProducts)
        rvRecentProducts.layoutManager = LinearLayoutManager(context) // List ke bawah

        rvRecentTestimoni = view.findViewById(R.id.rvRecentTestimoni) // Pastikan ID ini ada di XML
        rvRecentTestimoni.layoutManager = LinearLayoutManager(context)

        recentProductAdapter = RecentProdukAdapter(listOf()) // Init kosong
        rvRecentProducts.adapter = recentProductAdapter

        recentTestiAdapter = RecentTestiAdapter(listOf())
        rvRecentTestimoni.adapter = recentTestiAdapter

        // Tombol Lihat Semua
        tvSeeAllProduct = view.findViewById(R.id.tvSeeAllProducts)
        tvSeeAllTestimoni = view.findViewById(R.id.tvSeeAllTestimonies)

        btnQuickProduct = view.findViewById(R.id.btnQuickProduct)
        btnQuickClient = view.findViewById(R.id.btnQuickClient)
        btnQuickTestimony = view.findViewById(R.id.btnQuickTestimony)
        btnQuickContact = view.findViewById(R.id.btnQuickContact)
        btnQuickVisiMisi = view.findViewById(R.id.btnQuickVisiMisi)
        btnQuickRiwayat = view.findViewById(R.id.btnQuickRiwayat)
        btnQuickHero = view.findViewById(R.id.btnQuickHero)
        btnQuickSitus = view.findViewById(R.id.btnQuickSitus)
        btnQuickAdmin = view.findViewById(R.id.btnQuickAdmin)

    }

    private fun checkUserRole() {
        val sharedPref = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val role = sharedPref.getString("ROLE", "admin") // Default admin biasa

        if (role.equals("superadmin", ignoreCase = true)) {
            // Jika Super Admin -> TAMPILKAN
            btnQuickAdmin.visibility = View.VISIBLE
        } else {
            // Jika Admin Biasa -> SEMBUNYIKAN
            btnQuickAdmin.visibility = View.GONE
        }
    }

    private fun setupActions() {
        // Klik "Lihat Semua" -> Pindah ke Fragment Produk
        tvSeeAllProduct.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_produk)
            } catch (e: Exception) {
                // Jaga-jaga jika navigasi belum di-setup dengan benar
                Toast.makeText(context, "Navigasi ke Produk belum diset", Toast.LENGTH_SHORT).show()
            }
        }

        tvSeeAllTestimoni.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_testimoni)
            } catch (e: Exception) {
                // Jaga-jaga jika navigasi belum di-setup dengan benar
                Toast.makeText(context, "Navigasi ke Testimoni belum diset", Toast.LENGTH_SHORT).show()
            }
        }
        // --- 2. LOGIC AKSES CEPAT ---

        // Klik Produk
        btnQuickProduct.setOnClickListener {
            safeNavigate(R.id.nav_produk)
        }

        // Klik Klien
        btnQuickClient.setOnClickListener {
            safeNavigate(R.id.nav_klien)
        }

        // Klik Testimoni
        btnQuickTestimony.setOnClickListener {
            safeNavigate(R.id.nav_testimoni)
        }

        // Klik Kontak
        btnQuickContact.setOnClickListener {
            safeNavigate(R.id.nav_kontak)
        }

        btnQuickVisiMisi.setOnClickListener {
            safeNavigate(R.id.nav_visi_misi)
        }

        btnQuickRiwayat.setOnClickListener {
            safeNavigate(R.id.nav_riwayat)
        }

        btnQuickHero.setOnClickListener {
            safeNavigate(R.id.nav_hero)
        }

        btnQuickSitus.setOnClickListener {
            safeNavigate(R.id.nav_pengaturan)
        }
        btnQuickAdmin.setOnClickListener {
            safeNavigate(R.id.nav_admin_management)
        }
    }

    private fun fetchDashboardData() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getDashboardData()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Simpan ke cache agar tidak perlu download lagi nanti
                    cachedDashboardData = data

                    // Tampilkan ke layar
                    applyDataToView(data)
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Error: ${e.message}")
            } finally {
                // 4. PENTING: Matikan animasi loading setelah selesai
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyDataToView(data: Dashboard) {
        // Summary
        tvCountProduk.text = data.summary.totalProducts.toString()
        tvCountKlien.text = data.summary.totalClients.toString()
        tvCountTesti.text = data.summary.totalTestimony.toString()
        tvCountAdmin.text = data.summary.totalAdmins.toString()

        // List
        recentProductAdapter.updateData(data.recentProducts)
        recentTestiAdapter.updateData(data.recentTestimonials)
    }

    private fun safeNavigate(navId: Int) {
        try {
            findNavController().navigate(navId)
        } catch (e: Exception) {
            Toast.makeText(context, "Halaman belum tersedia / ID Navigasi salah", Toast.LENGTH_SHORT).show()
            Log.e("NAV_ERROR", e.message.toString())
        }
    }
}