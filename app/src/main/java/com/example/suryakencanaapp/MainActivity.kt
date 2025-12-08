package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.suryakencanaapp.databinding.ActivityMainBinding // Binding Activity
import com.example.suryakencanaapp.databinding.NavHeaderBinding   // Binding Header Sidebar

class MainActivity : AppCompatActivity() {

    // 1. Ganti variabel View lama dengan Binding
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Inflate menggunakan Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Akses Toolbar lewat binding
        setSupportActionBar(binding.toolbar)

        // --- SETUP NAV CONTROLLER ---
        // (Fragment Manager tetap butuh ID, bisa pakai R.id atau binding.id.id)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // --- SETUP HEADER & ROLE ---
        setupHeaderAndRole()

        // --- SETUP APP BAR CONFIGURATION ---
        // Akses DrawerLayout lewat binding
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_produk, R.id.nav_klien, R.id.nav_testimoni,
                R.id.nav_visi_misi, R.id.nav_riwayat, R.id.nav_hero,
                R.id.nav_kontak, R.id.nav_pengaturan, R.id.nav_admin_management
            ), binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        // --- [LISTENER] HIGHLIGHT MENU SAAT PINDAH HALAMAN ---
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuItem = findMenuItemInNav(binding.navView.menu, destination.id)
            if (menuItem != null) {
                menuItem.isChecked = true
            }
        }

        // --- CUSTOM NAVIGATION LISTENER ---
        binding.navView.setNavigationItemSelectedListener { item ->
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
        // Akses tombol logout lewat binding
        binding.btnLogoutSidebar.setOnClickListener {
            logout()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun findMenuItemInNav(menu: Menu, targetId: Int): MenuItem? {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId == targetId) return item
            if (item.hasSubMenu()) {
                val result = findMenuItemInNav(item.subMenu!!, targetId)
                if (result != null) return result
            }
        }
        return null
    }

    private fun closeDrawerSmoothly() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }, 300)
    }

    private fun setupHeaderAndRole() {
        // Akses Header View menggunakan Binding khusus Header
        // (Pastikan nav_header.xml valid agar NavHeaderBinding ter-generate)
        val headerView = binding.navView.getHeaderView(0)
        val headerBinding = NavHeaderBinding.bind(headerView)

        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val userRoleRaw = sharedPref.getString("ROLE", "") ?: ""
        val username = sharedPref.getString("USERNAME", "Admin")

        // Logic Admin Menu
        val menu = binding.navView.menu
        val adminMenu = menu.findItem(R.id.nav_admin_management)
        adminMenu?.isVisible = userRoleRaw.equals("superadmin", ignoreCase = true)

        // Set Text lewat Header Binding
        headerBinding.tvHeaderName.text = "Halo, $username"

        val formattedRole = if (userRoleRaw.isNotEmpty()) {
            userRoleRaw.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            "User"
        }
        headerBinding.tvHeaderRole.text = "$formattedRole"
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