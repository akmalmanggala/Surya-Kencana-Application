package com.example.suryakencanaapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.RecentProdukAdapter
import com.example.suryakencanaapp.adapter.RecentTestiAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentDashboardBinding
import com.example.suryakencanaapp.model.Dashboard
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var recentProductAdapter: RecentProdukAdapter
    private lateinit var recentTestiAdapter: RecentTestiAdapter
    private var cachedDashboardData: Dashboard? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        checkUserRole()

        if (cachedDashboardData != null) {
            applyDataToView(cachedDashboardData!!)
        } else {
            fetchDashboardData()
        }

        binding.swipeRefresh.setOnRefreshListener {
            fetchDashboardData()
        }

        setupActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViews() {
        binding.rvRecentProducts.layoutManager = LinearLayoutManager(context)
        binding.rvRecentTestimoni.layoutManager = LinearLayoutManager(context)

        recentProductAdapter = RecentProdukAdapter(listOf())
        binding.rvRecentProducts.adapter = recentProductAdapter

        recentTestiAdapter = RecentTestiAdapter(listOf())
        binding.rvRecentTestimoni.adapter = recentTestiAdapter
    }

    private fun checkUserRole() {
        val sharedPref = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val role = sharedPref.getString("ROLE", "admin")

        if (role.equals("superadmin", ignoreCase = true)) {
            binding.btnQuickAdmin.visibility = View.VISIBLE
        } else {
            binding.btnQuickAdmin.visibility = View.GONE
        }
    }

    private fun setupActions() {
        binding.tvSeeAllProducts.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_produk)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigasi ke Produk belum diset", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvSeeAllTestimonies.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_testimoni)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigasi ke Testimoni belum diset", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnQuickProduct.setOnClickListener {
            safeNavigate(R.id.nav_produk)
        }

        binding.btnQuickClient.setOnClickListener {
            safeNavigate(R.id.nav_klien)
        }

        binding.btnQuickTestimony.setOnClickListener {
            safeNavigate(R.id.nav_testimoni)
        }

        binding.btnQuickContact.setOnClickListener {
            safeNavigate(R.id.nav_kontak)
        }

        binding.btnQuickVisiMisi.setOnClickListener {
            safeNavigate(R.id.nav_visi_misi)
        }

        binding.btnQuickRiwayat.setOnClickListener {
            safeNavigate(R.id.nav_riwayat)
        }

        binding.btnQuickHero.setOnClickListener {
            safeNavigate(R.id.nav_hero)
        }

        binding.btnQuickSitus.setOnClickListener {
            safeNavigate(R.id.nav_pengaturan)
        }
        binding.btnQuickAdmin.setOnClickListener {
            safeNavigate(R.id.nav_admin_management)
        }
    }

    private fun fetchDashboardData() {

        _binding?.swipeRefresh?.isRefreshing = true

        // 2. Gunakan viewLifecycleOwner agar aman
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getDashboardData()

                // Cek _binding agar tidak crash jika user sudah pindah layar
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Simpan ke cache
                    cachedDashboardData = data

                    applyDataToView(data)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                } else {
                    Log.e("DASHBOARD", "Error: ${e.message}")
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun applyDataToView(data: Dashboard) {
        binding.tvCountProduk.text = data.summary.totalProducts.toString()
        binding.tvCountKlien.text = data.summary.totalClients.toString()
        binding.tvCountTesti.text = data.summary.totalTestimony.toString()
        binding.tvCountAdmin.text = data.summary.totalAdmins.toString()

        recentProductAdapter.updateData(data.recentProducts)
        if (data.recentProducts.isEmpty()) {
            binding.rvRecentProducts.visibility = View.GONE
            binding.tvEmptyRecentProduct.visibility = View.VISIBLE
        } else {
            binding.rvRecentProducts.visibility = View.VISIBLE
            binding.tvEmptyRecentProduct.visibility = View.GONE
        }
        recentTestiAdapter.updateData(data.recentTestimonials)
        if (data.recentTestimonials.isEmpty()) {
            binding.rvRecentTestimoni.visibility = View.GONE
            binding.tvEmptyRecentTestimoni.visibility = View.VISIBLE
        } else {
            binding.rvRecentTestimoni.visibility = View.VISIBLE
            binding.tvEmptyRecentTestimoni.visibility = View.GONE
        }
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