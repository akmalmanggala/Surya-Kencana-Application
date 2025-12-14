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

class AddTestimoniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTestimoniBinding
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditTestimoniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPageTitle.text = "Tambah Testimoni Baru"
        binding.btnSave.text = "Tambah Testimoni"

        setupDatePicker()

        binding.btnSave.setOnClickListener { saveData() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
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

    private fun saveData() {
        val name = binding.etClientName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()
        val feedback = binding.etFeedback.text.toString().trim()
        val date = binding.etDate.text.toString().trim()

        if (name.isEmpty() || institution.isEmpty() || feedback.isEmpty() || date == "dd/mm/yyyy") {
            Toast.makeText(this, "Semua data wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("AppSession", MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("TOKEN", "")

        if (savedToken.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_LONG).show()
            return
        }

        val authHeader = "Bearer $savedToken"

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val response = ApiClient.instance.addTestimoni(
                    authHeader,
                    name,
                    institution,
                    feedback,
                    date
                )

                if (response.isSuccessful) {
                    Toast.makeText(applicationContext, "Testimoni Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
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