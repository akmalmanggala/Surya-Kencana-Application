package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddEditClientBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditClientBinding
    private var clientId: Int = 0
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
        loadDataFromIntent()
    }

    private fun initViews() {
        binding.tvPageTitle.text = "Edit Data Client"
        binding.btnSave.text = "Simpan Perubahan"
    }

    private fun loadDataFromIntent() {
        clientId = intent.getIntExtra("ID", 0)
        binding.etClientName.setText(intent.getStringExtra("NAME"))
        binding.etInstitution.setText(intent.getStringExtra("INSTITUTION"))

        val oldLogoUrl = intent.getStringExtra("LOGO_URL")
        if (!oldLogoUrl.isNullOrEmpty()) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE

            Glide.with(this)
                .load(oldLogoUrl)
                .placeholder(R.drawable.upload_24dp_ffffff_fill0_wght400_grad0_opsz24)
                .into(binding.imgPreviewReal)

            binding.tvUploadInfo.text = "Logo Saat Ini (Ketuk untuk ganti)"
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnUploadLogo.setOnClickListener { openGallery() }
        binding.btnSave.setOnClickListener { updateClient() }
    }

    // --- LOGIC BUKA GALERI ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                binding.imgUploadIcon.visibility = View.GONE
                binding.imgPreviewReal.visibility = View.VISIBLE
                binding.imgPreviewReal.setImageURI(uri)
                binding.tvUploadInfo.text = "Gambar Terpilih (Ketuk untuk ganti)"

                // 2. PROSES DATA (FILE)
                // Konversi dari URI (content://...) ke File (java.io.File)
                // Fungsi ini ada di file FileUtils.kt
                selectedFile = FileUtils.getFileFromUri(this, uri)

                if (selectedFile == null) {
                    Toast.makeText(this, "Gagal mengkonversi gambar", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error saat memilih gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // --- LOGIC UPDATE ---
    private fun updateClient() {
        val name = binding.etClientName.text.toString().trim()
        val institution = binding.etInstitution.text.toString().trim()

        if (name.isEmpty() || institution.isEmpty()) {
            Toast.makeText(this, "Nama dan Institusi wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil Token
        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        lifecycleScope.launch {
            try {
                setLoading(true)

                // 1. Siapkan Data Teks
                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqInst = institution.toRequestBody("text/plain".toMediaTypeOrNull())

                // TRIK LARAVEL: Kirim field "_method" bernilai "PUT"
                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Siapkan Gambar (Kondisional)
                var bodyImage: MultipartBody.Part? = null

                if (selectedFile != null) {
                    // Jika user upload gambar baru
                    val requestFile = selectedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                    bodyImage = MultipartBody.Part.createFormData("logo_path", selectedFile!!.name, requestFile)
                }
                // Jika selectedFile null, bodyImage tetap null (Laravel tidak akan update kolom logo)

                // 3. Kirim ke Server
                val response = ApiClient.instance.updateClient(
                    "Bearer $token",
                    clientId,
                    reqName,
                    reqInst,
                    bodyImage, // Bisa null
                    reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EditClientActivity, "Klien Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    finish() // Kembali ke list
                } else {
                    Toast.makeText(this@EditClientActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@EditClientActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Updating..."
        } else {
            binding.btnSave.isEnabled = true
            binding.btnSave.text = "Simpan Perubahan"
        }
    }
}