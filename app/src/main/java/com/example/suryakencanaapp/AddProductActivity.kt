package com.example.suryakencanaapp

import android.app.Activity
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

class AddProductActivity : AppCompatActivity() {

    // Variabel UI
    private lateinit var etName: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var imgPreview: ImageView // Tambahkan ImageView di XML Anda untuk preview (opsional)

    // Variabel Data
    private var selectedImageUri: Uri? = null
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_produk)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Karena TextInputLayout, kita ambil EditText di dalamnya
        // Pastikan Anda memberi ID pada TextInputEditText di XML, bukan Layout-nya
        // Contoh di XML: <TextInputEditText android:id="@+id/etName" ... />

        // SEMENTARA: Sesuaikan ID ini dengan XML Anda
        // Saya asumsikan Anda akan menambahkan ID ke TextInputEditText di XML
        etName = findViewById(R.id.etName)
        etPrice = findViewById(R.id.etPrice)
        etDesc = findViewById(R.id.etDesc)

        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnUpload = findViewById(R.id.btnUploadImage)

        // Cari TextView label upload untuk diubah text-nya nanti
        // Anda mungkin perlu menambahkan ID di TextView "Tap to Upload Image" pada XML
        // Contoh: android:id="@+id/tvUploadInfo"
        tvUploadLabel = findViewById(R.id.tvUploadInfo)
    }

    private fun setupListeners() {
        // 1. Tombol Batal
        btnCancel.setOnClickListener { finish() }

        // 2. Tombol Upload Gambar
        btnUpload.setOnClickListener {
            openGallery()
        }

        // 3. Tombol Simpan
        btnSave.setOnClickListener {
            uploadProduct()
        }
    }

    // --- LOGIC BUKA GALERI ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri

            // Tampilkan info file terpilih
            tvUploadLabel.text = "Gambar terpilih!"

            // Konversi URI ke File agar siap upload
            selectedFile = FileUtils.getFileFromUri(this, uri)

            if (selectedFile == null) {
                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // --- LOGIC UPLOAD KE SERVER ---
    private fun uploadProduct() {
        val name = etName.text.toString().trim()
        val priceRaw = etPrice.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        // VALIDASI INPUT
        if (name.isEmpty() || priceRaw.isEmpty() || desc.isEmpty() || selectedFile == null) {
            Toast.makeText(this, "Semua data (Nama, Harga, Deskripsi, Gambar) wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // BERSIHKAN HARGA (Opsional tapi disarankan)
        // Jika user mengetik "20.000", Laravel butuh "20000" (numeric).
        // Hapus titik/koma jika ada.
        val priceClean = priceRaw.replace(".", "").replace(",", "")

        btnSave.isEnabled = false
        btnSave.text = "Uploading..."

        lifecycleScope.launch {
            try {
                // 1. SIAPKAN DATA TEKS (name, price, description)
                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPrice = priceClean.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. SIAPKAN DATA GAMBAR (image_path)
                // Buat RequestBody dari file
                val requestFile = selectedFile!!.asRequestBody("image/*".toMediaTypeOrNull())

                // INI KUNCINYA: Ganti "image" menjadi "image_path" sesuai controller Laravel
                val bodyImage = MultipartBody.Part.createFormData("image_path", selectedFile!!.name, requestFile)

                // 3. KIRIM KE SERVER
                val response = ApiClient.instance.addProduct(reqName, reqPrice, reqDesc, bodyImage)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddProductActivity, "Produk Berhasil Disimpan!", Toast.LENGTH_LONG).show()
                    finish() // Tutup halaman dan kembali ke list
                } else {
                    // Baca error dari Laravel (biasanya JSON validation error)
                    val errorMsg = response.errorBody()?.string()
                    Log.e("UPLOAD_ERROR", errorMsg ?: "Unknown error")
                    Toast.makeText(this@AddProductActivity, "Gagal: Cek Logcat untuk detail", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("UPLOAD_EXCEPTION", "Error: ${e.message}")
                Toast.makeText(this@AddProductActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Produk"
            }
        }
    }
}