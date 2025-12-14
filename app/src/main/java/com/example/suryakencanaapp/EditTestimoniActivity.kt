package com.example.suryakencanaapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
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
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPageTitle.text = "Edit Testimoni"
        binding.btnSave.text = "Simpan Perubahan"

        testimonialId = intent.getIntExtra("ID", 0)
        binding.etClientName.setText(intent.getStringExtra("NAME"))
        binding.etInstitution.setText(intent.getStringExtra("INSTITUTION"))
        binding.etFeedback.setText(intent.getStringExtra("FEEDBACK"))
        binding.etDate.setText(intent.getStringExtra("DATE"))

        setupDatePicker()

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

    // --- HELPER LOADING ---
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = AlertDialog.Builder(this)
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false)
                loadingDialog = builder.create()
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
            binding.btnSave.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSave.isEnabled = true
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

        val prefs = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "")

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi habis", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val response = ApiClient.instance.updateTestimoni(
                    "Bearer $token",
                    testimonialId,
                    name,
                    institution,
                    feedback,
                    date
                )

                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Testimoni Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(applicationContext, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // 2. Sembunyikan Overlay
                setLoading(false)
            }
        }
    }
}