package com.example.suryakencanaapp

import android.graphics.ImageDecoder
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.suryakencanaapp.adapter.AlbumImageAdapter
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

    // --- VARIABEL UNTUK PREVIEW ---
    private lateinit var imgUploadIcon: ImageView
    private lateinit var imgPreviewReal: ImageView
    private lateinit var btnUploadAlbum: LinearLayout
    private lateinit var rvAlbumPreview: RecyclerView
    private lateinit var albumAdapter: AlbumImageAdapter
    private lateinit var tvUploadInfo2: TextView

    private var selectedFile: File? = null
    private val selectedAlbumFiles = mutableListOf<File>() // List file album
    private val selectedAlbumUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_produk) // Pastikan nama XML benar

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etPrice = findViewById(R.id.etPrice)
        etDesc = findViewById(R.id.etDesc)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnUpload = findViewById(R.id.btnUploadImage)
        tvUploadLabel = findViewById(R.id.tvUploadInfo)
        tvUploadInfo2 = findViewById(R.id.tvUploadInfo2)


        // --- INISIALISASI IMAGE VIEW ---
        imgUploadIcon = findViewById(R.id.imgUploadIcon)
        imgPreviewReal = findViewById(R.id.imgPreviewReal)

        btnUploadAlbum = findViewById(R.id.btnUploadAlbum)
        rvAlbumPreview = findViewById(R.id.rvAlbumPreview)

        rvAlbumPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = AlbumImageAdapter(selectedAlbumUris) { position ->
            // Logic Hapus Item dari Album
            selectedAlbumUris.removeAt(position)
            selectedAlbumFiles.removeAt(position)
            albumAdapter.notifyItemRemoved(position)

            // Sembunyikan RecyclerView jika kosong
            if (selectedAlbumUris.isEmpty()) {
                rvAlbumPreview.visibility = View.GONE
            }
        }
        rvAlbumPreview.adapter = albumAdapter
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { uploadProduct() }
        // Listener Gambar UTAMA (Single)
        btnUpload.setOnClickListener {
            mainImageLauncher.launch("image/*")
        }

        // Listener Gambar ALBUM (Multiple)
        btnUploadAlbum.setOnClickListener {
            albumImageLauncher.launch("image/*")
        }

    }

    // --- LOGIC BUKA GALERI & TAMPILKAN PREVIEW ---
    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Update UI Preview Utama
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE
            imgPreviewReal.setImageURI(uri)
            tvUploadLabel.text = "Gambar Utama Terpilih"
            tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"

            // Konversi ke File
            selectedFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    // --- 2. LAUNCHER GAMBAR ALBUM (BARU) ---
    // Pakai: GetMultipleContents() -> Bisa pilih banyak
    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Munculkan RecyclerView Album
            rvAlbumPreview.visibility = View.VISIBLE

            for (uri in uris) {
                // Konversi setiap gambar jadi File
                val file = FileUtils.getFileFromUri(this, uri)
                if (file != null) {
                    selectedAlbumFiles.add(file) // Simpan File untuk diupload
                    selectedAlbumUris.add(uri)   // Simpan URI untuk preview
                }
            }

            // Refresh Adapter Album
            albumAdapter.notifyDataSetChanged()
            Toast.makeText(this, "${uris.size} gambar ditambahkan", Toast.LENGTH_SHORT).show()
        }
    }

    // --- LOGIC UPLOAD (Sama seperti sebelumnya) ---
    private fun uploadProduct() {
        val name = etName.text.toString().trim()
        val priceRaw = etPrice.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        // Validasi dasar (Main Image wajib, Album opsional)
        if (name.isEmpty() || priceRaw.isEmpty() || desc.isEmpty() || selectedFile == null) {
            Toast.makeText(this, "Data wajib (Nama, Harga, Deskripsi, Gambar Utama) belum lengkap!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: return
        val authHeader = "Bearer $token"

        val priceClean = priceRaw.replace(".", "").replace(",", "")

        lifecycleScope.launch {
            try {
                setLoading(true)

                // 1. SIAPKAN DATA TEKS
                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPrice = priceClean.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. SIAPKAN GAMBAR UTAMA (selectedFile)
                // Deteksi MimeType (PNG/JPG)
                val mimeTypeMain = if (selectedFile!!.extension.equals("png", true)) "image/png" else "image/jpeg"
                val requestFile = selectedFile!!.asRequestBody(mimeTypeMain.toMediaTypeOrNull())

                // Nama field: "image_path" (Sesuai Controller Laravel)
                val bodyMainImage = MultipartBody.Part.createFormData("image_path", selectedFile!!.name, requestFile)

                // 3. SIAPKAN GAMBAR ALBUM (Looping selectedAlbumFiles) --- [BAGIAN BARU] ---
                val albumParts = mutableListOf<MultipartBody.Part>()

                // 'selectedAlbumFiles' adalah list yang kita buat di langkah sebelumnya
                for (file in selectedAlbumFiles) {
                    val mimeTypeAlbum = if (file.extension.equals("png", true)) "image/png" else "image/jpeg"
                    val reqFileAlbum = file.asRequestBody(mimeTypeAlbum.toMediaTypeOrNull())

                    // PENTING: Nama field harus "images[]" (pakai kurung siku) agar dibaca Array oleh Laravel
                    val part = MultipartBody.Part.createFormData("images[]", file.name, reqFileAlbum)

                    albumParts.add(part)
                }
                // -------------------------------------------------------------------------

                // 4. KIRIM KE API (Tambahkan parameter albumParts)
                val response = ApiClient.instance.addProduct(
                    authHeader,
                    reqName,
                    reqPrice,
                    reqDesc,
                    bodyMainImage,
                    if (albumParts.isEmpty()) null else albumParts // Kirim null jika tidak ada album
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@AddProductActivity, "Produk Berhasil Ditambahkan!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("UPLOAD_ERROR", errorMsg ?: "Unknown error")
                    Toast.makeText(this@AddProductActivity, "Gagal: Cek Logcat", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AddProductActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            btnSave.isEnabled = false
            btnSave.text = "Uploading..."
        } else {
            btnSave.isEnabled = true
            btnSave.text = "Tambah Produk"
        }
    }
}