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

class EditTestimoniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTestimoniBinding
    private var testimonialId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPageTitle.text = "Edit Testimoni"
        binding.btnSave.text = "Simpan Perubahan"

        // 2. AMBIL DATA DARI ADAPTER (Intent)
        testimonialId = intent.getIntExtra("ID", 0)
        binding.etClientName.setText(intent.getStringExtra("NAME"))
        binding.etInstitution.setText(intent.getStringExtra("INSTITUTION"))
        binding.etFeedback.setText(intent.getStringExtra("FEEDBACK"))
        binding.etDate.setText(intent.getStringExtra("DATE"))

        // 3. Setup Date Picker
        setupDatePicker()

        // 4. Action Tombol
        binding.btnSave.setOnClickListener { updateData() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                val formattedMonth = String.format("%02d", m + 1)
                val formattedDay = String.format("%02d", d)
                binding.etDate.setText("$y-$formattedMonth-$formattedDay")
            }, year, month, day).show()
        }
    }

    private fun updateData() {
        val name = binding.etClientName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val feedback = binding.etFeedback.text.toString().trim()
        val date = binding.etDate.text.toString().trim()

        if (name.isEmpty() || institution.isEmpty()) {
            Toast.makeText(this, "Data tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil Token Session
        val prefs = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi habis", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Updating..."

                // PANGGIL API UPDATE
                val response = ApiClient.instance.updateTestimoni(
                    "Bearer $token",
                    testimonialId, // ID dikirim terpisah
                    name,
                    institution,
                    feedback,
                    date
                )

                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Testimoni Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    finish() // Tutup halaman edit
                } else {
                    Toast.makeText(applicationContext, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Simpan Perubahan"
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Simpan Perubahan"
            }
        }
    }
}