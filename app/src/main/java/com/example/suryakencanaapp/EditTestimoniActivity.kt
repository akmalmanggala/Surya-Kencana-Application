package com.example.suryakencanaapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar

class EditTestimoniActivity : AppCompatActivity() {

    private lateinit var etClientName: TextInputEditText
    private lateinit var etInstitution: TextInputEditText
    private lateinit var etFeedback: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var tvPageTitle: TextView


    private var testimonialId: Int = 0 // Untuk menyimpan ID yang mau diedit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_testimoni) // Pastikan XML Edit sudah dibuat

        // 1. Inisialisasi View
        etClientName = findViewById(R.id.etClientName)
        etInstitution = findViewById(R.id.etInstitution)
        etFeedback = findViewById(R.id.etFeedback)
        etDate = findViewById(R.id.etDate)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
        tvPageTitle = findViewById(R.id.tvPageTitle)
        tvPageTitle.text = "Edit Testimoni"
        btnSave.text = "Simpan Perubahan"

        // 2. AMBIL DATA DARI ADAPTER (Intent)
        testimonialId = intent.getIntExtra("ID", 0)
        etClientName.setText(intent.getStringExtra("NAME"))
        etInstitution.setText(intent.getStringExtra("INSTITUTION"))
        etFeedback.setText(intent.getStringExtra("FEEDBACK"))
        etDate.setText(intent.getStringExtra("DATE"))

        // 3. Setup Date Picker
        setupDatePicker()

        // 4. Action Tombol
        btnSave.setOnClickListener { updateData() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupDatePicker() {
        // ... (Kode DatePicker SAMA PERSIS dengan AddTestimoni) ...
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                val formattedMonth = String.format("%02d", m + 1)
                val formattedDay = String.format("%02d", d)
                etDate.setText("$y-$formattedMonth-$formattedDay")
            }, year, month, day).show()
        }
    }

    private fun updateData() {
        val name = etClientName.text.toString().trim()
        val institution = etInstitution.text.toString().trim()
        val feedback = etFeedback.text.toString().trim()
        val date = etDate.text.toString().trim()

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
                btnSave.isEnabled = false
                btnSave.text = "Updating..."

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
                    Toast.makeText(applicationContext, "Data berhasil diubah!", Toast.LENGTH_SHORT).show()
                    finish() // Tutup halaman edit
                } else {
                    Toast.makeText(applicationContext, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan Perubahan"
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
            }
        }
    }
}