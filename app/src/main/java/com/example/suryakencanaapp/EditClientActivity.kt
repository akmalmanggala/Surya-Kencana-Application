package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.utils.FileUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditClientActivity : AppCompatActivity() {

    // Variabel UI
    private lateinit var etClientName: TextInputEditText
    private lateinit var etInstitution: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var imgUploadIcon: ImageView // Ikon panah
    private lateinit var imgPreviewReal: ImageView
    private lateinit var tvPageTitle: TextView

    // Data
    private var clientId: Int = 0
    private var selectedFile: File? = null // File baru (jika user ganti gambar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // KITA PAKAI LAYOUT YANG SAMA DENGAN ADD
        setContentView(R.layout.activity_add_edit_client)

        initViews()
        setupListeners()
        loadDataFromIntent()
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

        // Ubah Teks agar sesuai konteks Edit
        tvPageTitle.text = "Edit Data Client"
        btnSave.text = "Simpan Perubahan"
    }

    private fun loadDataFromIntent() {
        // Ambil data yang dikirim dari Adapter
        clientId = intent.getIntExtra("ID", 0)
        etClientName.setText(intent.getStringExtra("NAME"))
        etInstitution.setText(intent.getStringExtra("INSTITUTION"))

        // Tampilkan Logo Lama
        val oldLogoUrl = intent.getStringExtra("LOGO_URL")
        if (!oldLogoUrl.isNullOrEmpty()) {
            // Tampilkan gambar lama
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE

            Glide.with(this)
                .load(oldLogoUrl)
                .placeholder(R.drawable.upload_24dp_ffffff_fill0_wght400_grad0_opsz24)
                .into(imgPreviewReal)

            tvUploadLabel.text = "Logo Saat Ini (Ketuk untuk ganti)"
        }
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }

        // Klik area upload -> Buka Galeri
        btnUpload.setOnClickListener { openGallery() }

        btnSave.setOnClickListener { updateClient() }
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

    // --- LOGIC UPDATE ---
    private fun updateClient() {
        val name = etClientName.text.toString().trim()
        val institution = etInstitution.text.toString().trim()

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
            btnSave.isEnabled = false
            btnSave.text = "Updating..."
        } else {
            btnSave.isEnabled = true
            btnSave.text = "Simpan Perubahan"
        }
    }
}