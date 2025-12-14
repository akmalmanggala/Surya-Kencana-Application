package com.example.suryakencanaapp

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddEditClientBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditClientBinding
    private var selectedFile: File? = null
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        binding.tvPageTitle.text = "Tambah Klien Baru"
        binding.btnSave.text = "Tambah Klien"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnUploadLogo.setOnClickListener { openGallery() }
        binding.btnSave.setOnClickListener { uploadClient() }
    }

    // --- LOGIC BUKA GALERI ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Cek apakah user benar-benar memilih gambar (tidak membatalkan)
        if (uri != null) {
            try {
                // 1. UPDATE UI (TAMPILAN)
                // Sembunyikan ikon panah upload
                binding.imgUploadIcon.visibility = View.GONE

                // Munculkan ImageView preview dan isi gambarnya
                binding.imgPreviewReal.visibility = View.VISIBLE
                binding.imgPreviewReal.setImageURI(uri)

                // Ubah teks label agar user tahu gambar sudah masuk
                binding.tvUploadInfo.text = "Gambar Utama Terpilih"
                binding.tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"


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

    // --- LOGIC UPLOAD KE SERVER ---
    private fun uploadClient() {
        val name = binding.etClientName.text.toString().trim()

        // 1. VALIDASI
        if (name.isEmpty() || selectedFile == null) {
            Toast.makeText(this, "Nama dan Logo wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. AMBIL TOKEN
        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "")

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Token tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val authHeader = "Bearer $token"

        lifecycleScope.launch {
            try {
                setLoading(true)

                // 3. SIAPKAN DATA TEXT
                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())

                // 4. SIAPKAN FILE GAMBAR
                // Nama part 'logo' atau 'image' harus sesuai dengan Controller Laravel ($request->file('logo'))
                // Disini saya gunakan 'logo' sesuai nama field di form web Anda
                val requestFile = selectedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                val bodyImage = MultipartBody.Part.createFormData("logo_path", selectedFile!!.name, requestFile)

                // 5. KIRIM (Pastikan addClient dibuat di ApiService nanti)
                val response = ApiClient.instance.addClient(authHeader, reqName, bodyImage)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddClientActivity, "Klien Berhasil Ditambahkan!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("UPLOAD_ERROR", errorMsg ?: "Unknown error")
                    Toast.makeText(this@AddClientActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AddClientActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

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
}