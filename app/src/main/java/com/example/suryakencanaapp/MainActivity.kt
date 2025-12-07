package com.example.suryakencanaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // 1. AKSES HEADER VIEW
        // Karena header ada di dalam NavView, kita harus ambil dulu view-nya
        val headerView = navView.getHeaderView(0)

        // 2. CARI TEXTVIEW DI DALAM HEADER
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)

        // 3. AMBIL DATA DARI SESSION (SharedPref)
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val userRole = sharedPref.getString("ROLE", "admin")
        val username = sharedPref.getString("USERNAME", "Admin")

        val menu = navView.menu
        val adminMenu = menu.findItem(R.id.nav_admin_management)

        if (userRole.equals("superadmin", ignoreCase = true)) {
            // Jika Super Admin: Munculkan
            adminMenu.isVisible = true
        } else {
            // Jika Admin Biasa: Sembunyikan
            adminMenu.isVisible = false
        }

        // 4. TEMPELKAN DATA KE HEADER
        tvHeaderName.text = "Halo, $username"

        // 1. Setup Navigation Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 2. Tentukan menu mana saja yang dianggap "Level Atas" (Ada tombol garis tiga/hamburger)
        // Masukkan SEMUA ID fragment Anda di sini
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard, R.id.nav_produk, R.id.nav_klien, R.id.nav_testimoni,
                R.id.nav_visi_misi, R.id.nav_riwayat, R.id.nav_hero,
                R.id.nav_kontak, R.id.nav_pengaturan, R.id.nav_admin_management
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 3. HANDLER KHUSUS UNTUK LOGOUT
        // Karena Logout bukan pindah fragment, kita harus cegat event klik-nya
        val btnLogoutSidebar = findViewById<Button>(R.id.btnLogoutSidebar)

        btnLogoutSidebar.setOnClickListener {
            // Panggil fungsi logout yang sudah Anda buat
            logout()

            // (Opsional) Tutup drawer setelah klik
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    // Agar tombol hamburger di pojok kiri atas berfungsi
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun logout() {
        // 1. HAPUS DATA SESSION
        val sharedPref = getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear() // Menghapus semua data (Token, Username, Role)
        editor.apply()

        Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show()

        // 2. KEMBALI KE LOGIN
        val intent = Intent(this, LoginActivity::class.java)

        // PENTING: Flag ini membersihkan tumpukan aktivitas (History)
        // Agar saat di halaman Login, user tekan Back tidak balik lagi ke Dashboard
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish() // Matikan MainActivity
    }
}