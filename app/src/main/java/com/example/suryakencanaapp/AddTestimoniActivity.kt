package com.example.suryakencanaapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddEditTestimoniBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class AddTestimoniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTestimoniBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPageTitle.text = "Tambah Testimoni Baru"
        binding.btnSave.text = "Tambah Testimoni"

        // 2. Setup Date Picker (Kalender)
        setupDatePicker()

        // 3. Tombol Simpan
        binding.btnSave.setOnClickListener {
            saveData()
        }

        // 4. Tombol Batal
        binding.btnCancel.setOnClickListener {
            finish() // Kembali ke halaman sebelumnya
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format harus yyyy-MM-dd agar diterima Database MySQL/Laravel
                    // Tambah +1 pada bulan karena Calendar.MONTH dimulai dari 0
                    val formattedMonth = String.format("%02d", selectedMonth + 1)
                    val formattedDay = String.format("%02d", selectedDay)

                    val dateString = "$selectedYear-$formattedMonth-$formattedDay"
                    binding.etDate.setText(dateString)
                },
                year, month, day
            )
            datePickerDialog.show()
        }
    }

    private fun saveData() {
        // 1. Ambil data dari input
        val name = binding.etClientName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val feedback = binding.etFeedback.text.toString().trim()
        val date = binding.etDate.text.toString().trim()

        // 2. Validasi Input
        if (name.isEmpty() || institution.isEmpty() || feedback.isEmpty() || date == "dd/mm/yyyy") {
            Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. AMBIL TOKEN DARI SHAREDPREFERENCES (PENTING!)
        // Sesuaikan "user_session" dan "token" dengan apa yang Anda buat di LoginActivity
        val sharedPreferences = getSharedPreferences("AppSession", MODE_PRIVATE) // Ganti nama pref jika beda
        val savedToken = sharedPreferences.getString("TOKEN", "") // Ganti key "token" jika beda

        if (savedToken.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_LONG).show()
            // Opsional: Arahkan ke LoginActivity
            return
        }

        // Format Token untuk Laravel (Bearer + Spasi + Token)
        val authHeader = "Bearer $savedToken"

        // 4. Kirim ke API
        lifecycleScope.launch {
            try {
                // Ubah teks tombol jadi "Loading..."
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Uploading..."

                // PERHATIKAN: Parameter pertama sekarang adalah 'authHeader'
                val response = ApiClient.instance.addTestimoni(
                    authHeader, // <--- Token dikirim di sini
                    name,
                    institution,
                    feedback,
                    date
                )

                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Testimoni Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish() // Tutup activity ini & kembali ke list
                } else {
                    Toast.makeText(applicationContext, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    // Kembalikan tombol
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Tambah Testimoni"
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Tambah Testimoni"
            }
        }
    }
}