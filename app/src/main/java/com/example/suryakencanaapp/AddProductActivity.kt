package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.AlbumImageAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.ActivityAddProdukBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProdukBinding

    private var selectedFile: File? = null
    private val selectedAlbumFiles = mutableListOf<File>() // List file album
    private val selectedAlbumUris = mutableListOf<Uri>()
    private lateinit var albumAdapter: AlbumImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Inflate Layout menggunakan Binding
        binding = ActivityAddProdukBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set content view ke root milik binding

        // Inisialisasi tidak perlu findViewById lagi
        initViews()
        setupListeners()
    }

    private fun initViews() {
        // Akses view langsung lewat 'binding.idView'
        // Contoh: R.id.rvAlbumPreview otomatis jadi binding.rvAlbumPreview

        binding.rvAlbumPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        albumAdapter = AlbumImageAdapter(selectedAlbumUris) { position ->
            selectedAlbumUris.removeAt(position)
            selectedAlbumFiles.removeAt(position)
            albumAdapter.notifyItemRemoved(position)

            if (selectedAlbumUris.isEmpty()) {
                binding.rvAlbumPreview.visibility = View.GONE
            }
        }
        binding.rvAlbumPreview.adapter = albumAdapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { uploadProduct() }
        // Listener Gambar UTAMA (Single)
        binding.btnUploadImage.setOnClickListener {
            mainImageLauncher.launch("image/*")
        }

        // Listener Gambar ALBUM (Multiple)
        binding.btnUploadAlbum.setOnClickListener {
            albumImageLauncher.launch("image/*")
        }

    }

    // --- LOGIC BUKA GALERI & TAMPILKAN PREVIEW ---
    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Update UI Preview Utama
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE
            binding.imgPreviewReal.setImageURI(uri)
            binding.tvUploadInfo.text = "Gambar Utama Terpilih"
            binding.tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"

            // Konversi ke File
            selectedFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    // --- 2. LAUNCHER GAMBAR ALBUM (BARU) ---
    // Pakai: GetMultipleContents() -> Bisa pilih banyak
    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Munculkan RecyclerView Album
            binding.rvAlbumPreview.visibility = View.VISIBLE

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
        val name = binding.etName.text.toString().trim()
        val priceRaw = binding.etPrice.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()

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
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Uploading..."
        } else {
            binding.btnSave.isEnabled = true
            binding.btnSave.text = "Tambah Produk"
        }
    }
}