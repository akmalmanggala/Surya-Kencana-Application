package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // --- SETUP NAV CONTROLLER ---
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // --- SETUP HEADER & ROLE ---
        setupHeaderAndRole(navView)

        // --- SETUP APP BAR CONFIGURATION ---
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_produk, R.id.nav_klien, R.id.nav_testimoni,
                R.id.nav_visi_misi, R.id.nav_riwayat, R.id.nav_hero,
                R.id.nav_kontak, R.id.nav_pengaturan, R.id.nav_admin_management
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // --- CUSTOM NAVIGATION LISTENER (DENGAN DELAY) ---
        navView.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            // 1. Cek apakah halaman sudah sama? (Kalau sama, tutup aja tanpa reload)
            if (navController.currentDestination?.id == id) {
                closeDrawerSmoothly()
                return@setNavigationItemSelectedListener false
            }

            // 2. Lakukan Navigasi
            when (id) {
                R.id.nav_dashboard -> {
                    navController.popBackStack(R.id.nav_dashboard, false)
                }
                else -> {
                    val options = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.nav_dashboard, false)
                        .build()

                    try {
                        navController.navigate(id, null, options)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 3. TUTUP DRAWER DENGAN JEDA (Agar lebih halus)
            closeDrawerSmoothly()

            true
        }

        // --- LOGOUT HANDLER ---
        val btnLogoutSidebar = findViewById<Button>(R.id.btnLogoutSidebar)
        btnLogoutSidebar.setOnClickListener {
            logout()
            // Logout tidak perlu delay karena langsung pindah Activity
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    // --- FUNGSI BARU: Tutup Drawer dengan Delay ---
    private fun closeDrawerSmoothly() {
        // Beri jeda 300ms agar efek klik terlihat dulu
        Handler(Looper.getMainLooper()).postDelayed({
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }, 300) // <-- Ganti angka ini jika ingin lebih lambat (misal 500)
    }

    private fun setupHeaderAndRole(navView: NavigationView) {
        // 1. Ambil Header View
        val headerView = navView.getHeaderView(0)

        // 2. Inisialisasi TextViews
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        // Tambahkan inisialisasi untuk Role TV yang baru
        val tvHeaderRole = headerView.findViewById<TextView>(R.id.tvHeaderRole)

        // 3. Ambil data dari Shared Preferences
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        // Beri nilai default "" agar mudah dicek
        val userRoleRaw = sharedPref.getString("ROLE", "") ?: ""
        val username = sharedPref.getString("USERNAME", "Admin")

        // 4. Setup Menu Admin (Kode lama Anda)
        val menu = navView.menu
        val adminMenu = menu.findItem(R.id.nav_admin_management)
        // Cek role secara case-insensitive (aman untuk "superadmin" atau "SuperAdmin")
        adminMenu?.isVisible = userRoleRaw.equals("superadmin", ignoreCase = true)

        // 5. Set Teks Nama
        tvHeaderName.text = "Halo, $username"

        // 6. FORMAT & SET TEKS ROLE (BARU)
        // Ubah role menjadi format yang lebih rapi (Huruf depan kapital)
        // Contoh: "superadmin" menjadi "Superadmin", "admin" menjadi "Admin"
        val formattedRole = if (userRoleRaw.isNotEmpty()) {
            userRoleRaw.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            "User" // Default jika kosong
        }

        // Tampilkan dengan tanda kurung agar rapi, misal: (Superadmin)
        tvHeaderRole.text = "$formattedRole"
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}