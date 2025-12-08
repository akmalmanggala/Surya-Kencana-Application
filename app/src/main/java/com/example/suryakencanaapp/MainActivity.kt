package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
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
    private lateinit var navView: NavigationView // Tambahkan ini sebagai variabel global kelas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view) // Inisialisasi di sini

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

        // --- [BARU] PAKSA HIGHLIGHT MENU SAAT PINDAH HALAMAN ---
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 1. Cari item menu berdasarkan ID halaman yang sedang dibuka
            val menuItem = findMenuItemInNav(navView.menu, destination.id)

            // 2. Jika ketemu, aktifkan (Checked)
            if (menuItem != null) {
                menuItem.isChecked = true
            }
        }
        // -------------------------------------------------------

        // --- CUSTOM NAVIGATION LISTENER (YANG LAMA TETAP ADA) ---
        navView.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            if (navController.currentDestination?.id == id) {
                closeDrawerSmoothly()
                return@setNavigationItemSelectedListener false
            }

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
            closeDrawerSmoothly()
            true
        }

        // --- LOGOUT HANDLER ---
        val btnLogoutSidebar = findViewById<Button>(R.id.btnLogoutSidebar)
        btnLogoutSidebar.setOnClickListener {
            logout()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    // --- [BARU] FUNGSI PENCARI MENU (REKURSIF) ---
    // Fungsi ini bisa mencari menu sampai ke dalam sub-menu terdalam
    private fun findMenuItemInNav(menu: Menu, targetId: Int): MenuItem? {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)

            // Cek apakah item ini yang dicari?
            if (item.itemId == targetId) {
                return item
            }

            // Jika item ini punya sub-menu, cari lagi di dalamnya
            if (item.hasSubMenu()) {
                val result = findMenuItemInNav(item.subMenu!!, targetId)
                if (result != null) return result
            }
        }
        return null
    }

    private fun closeDrawerSmoothly() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }, 300)
    }

    private fun setupHeaderAndRole(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        val tvHeaderRole = headerView.findViewById<TextView>(R.id.tvHeaderRole)

        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val userRoleRaw = sharedPref.getString("ROLE", "") ?: ""
        val username = sharedPref.getString("USERNAME", "Admin")

        val menu = navView.menu
        val adminMenu = menu.findItem(R.id.nav_admin_management)
        adminMenu?.isVisible = userRoleRaw.equals("superadmin", ignoreCase = true)

        tvHeaderName.text = "Halo, $username"

        val formattedRole = if (userRoleRaw.isNotEmpty()) {
            userRoleRaw.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            "User"
        }
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