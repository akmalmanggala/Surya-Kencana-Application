package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.utils.FileUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddClientActivity : AppCompatActivity() {

    // Variabel UI
    private lateinit var etClientName: TextInputEditText
    private lateinit var etInstitution: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var imgUploadIcon: ImageView // Ikon panah
    private lateinit var imgPreviewReal: ImageView // Preview gambar asli
    private lateinit var tvPageTitle: TextView


    // Variabel Data
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_client) // Pastikan nama XML benar

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etClientName = findViewById(R.id.etClientName)
        etInstitution = findViewById(R.id.etInstitution)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnUpload = findViewById(R.id.btnUploadLogo)
        tvUploadLabel = findViewById(R.id.tvUploadInfo)
        imgUploadIcon = findViewById(R.id.imgUploadIcon)
        imgPreviewReal = findViewById(R.id.imgPreviewReal)
        tvPageTitle = findViewById(R.id.tvPageTitle)
        tvPageTitle.text = "Tambah Klien Baru"
        btnSave.text = "Tambah Testimoni"
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnUpload.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { uploadClient() }
    }

    // --- LOGIC BUKA GALERI ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Cek apakah user benar-benar memilih gambar (tidak membatalkan)
        if (uri != null) {
            try {
                // 1. UPDATE UI (TAMPILAN)
                // Sembunyikan ikon panah upload
                imgUploadIcon.visibility = View.GONE

                // Munculkan ImageView preview dan isi gambarnya
                imgPreviewReal.visibility = View.VISIBLE
                imgPreviewReal.setImageURI(uri)

                // Ubah teks label agar user tahu gambar sudah masuk
                tvUploadLabel.text = "Gambar Terpilih (Ketuk untuk ganti)"

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
        val name = etClientName.text.toString().trim()
        val institution = etInstitution.text.toString().trim()

        // 1. VALIDASI
        if (name.isEmpty() || institution.isEmpty() || selectedFile == null) {
            Toast.makeText(this, "Nama, Institusi, dan Logo wajib diisi!", Toast.LENGTH_SHORT).show()
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
                val reqInst = institution.toRequestBody("text/plain".toMediaTypeOrNull())

                // 4. SIAPKAN FILE GAMBAR
                // Nama part 'logo' atau 'image' harus sesuai dengan Controller Laravel ($request->file('logo'))
                // Disini saya gunakan 'logo' sesuai nama field di form web Anda
                val requestFile = selectedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                val bodyImage = MultipartBody.Part.createFormData("logo_path", selectedFile!!.name, requestFile)

                // 5. KIRIM (Pastikan addClient dibuat di ApiService nanti)
                val response = ApiClient.instance.addClient(authHeader, reqName, reqInst, bodyImage)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddClientActivity, "Klien Berhasil Disimpan!", Toast.LENGTH_LONG).show()
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
            btnSave.isEnabled = false
            btnSave.text = "Mengupload..."
        } else {
            btnSave.isEnabled = true
            btnSave.text = "Tambah Client"
        }
    }
}