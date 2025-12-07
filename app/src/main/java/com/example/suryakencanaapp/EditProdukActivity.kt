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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

class EditProdukActivity : AppCompatActivity() {

    // UI Variables
    private lateinit var etName: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var tvPageTitle: TextView

    // Image UI
    private lateinit var imgUploadIcon: ImageView
    private lateinit var imgPreviewReal: ImageView

    // --- TAMBAHAN UNTUK ALBUM ---
    private lateinit var btnUploadAlbum: LinearLayout
    private lateinit var rvAlbumPreview: RecyclerView
    private lateinit var albumAdapter: AlbumImageAdapter

    // List untuk menampung data album
    private val selectedNewAlbumFiles = mutableListOf<File>() // Gambar baru yang mau diupload
    private val previewAlbumUris = mutableListOf<Uri>()       // Untuk tampil di layar
    private val deletedImagePaths = mutableListOf<String>()   // Path gambar lama yang dihapus
    private val originalImageUrls = mutableListOf<String>()   // Helper path lama

    // Data
    private var productId: Int = 0
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_produk)

        initViews()

        try {
            tvPageTitle.text = "Edit Produk"
        } catch (e: Exception) { }

        btnSave.text = "Simpan Perubahan"

        loadDataFromIntent()
        loadAlbumFromApi() // Fungsi ini yang kita perbaiki
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

        imgUploadIcon = findViewById(R.id.imgUploadIcon)
        imgPreviewReal = findViewById(R.id.imgPreviewReal)
        tvPageTitle = findViewById(R.id.tvPageTitle)

        // Init Album UI
        btnUploadAlbum = findViewById(R.id.btnUploadAlbum)
        rvAlbumPreview = findViewById(R.id.rvAlbumPreview)

        rvAlbumPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // --- TAMBAHAN OPTIMASI RECYCLERVIEW ---
        rvAlbumPreview.setHasFixedSize(true) // Agar layout tidak hitung ulang ukuran terus menerus
        rvAlbumPreview.setItemViewCacheSize(10) // Simpan 10 item di memori agar tidak reload saat scroll
// --------------------------------------

        albumAdapter = AlbumImageAdapter(previewAlbumUris) { position ->
            handleRemoveAlbumItem(position)
        }
        rvAlbumPreview.adapter = albumAdapter
    }

    private fun loadDataFromIntent() {
        productId = intent.getIntExtra("ID", 0)
        etName.setText(intent.getStringExtra("NAME"))
        etDesc.setText(intent.getStringExtra("DESC"))

        val rawPrice = intent.getStringExtra("PRICE") ?: "0"
        val cleanPrice = rawPrice.substringBefore(".")
        etPrice.setText(cleanPrice)

        val oldImageUrl = intent.getStringExtra("IMAGE_URL")
        if (!oldImageUrl.isNullOrEmpty()) {
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE

            // Load Gambar Utama (Sudah benar pakai URL lengkap dari Intent)
            Glide.with(this)
                .load(oldImageUrl)
                .placeholder(R.drawable.package_2_24dp_ffffff_fill0_wght400_grad0_opsz24)
                .into(imgPreviewReal)

            tvUploadLabel.text = "Gambar Saat Ini (Ketuk untuk ganti)"
        }
    }

    // --- PERBAIKAN UTAMA ADA DI SINI ---
    private fun loadAlbumFromApi() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProductDetail(productId)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Pastikan API mengirim images (path) DAN imageUrls (link lengkap)
                    if (!data.images.isNullOrEmpty() && !data.imageUrls.isNullOrEmpty()) {
                        rvAlbumPreview.visibility = View.VISIBLE

                        // 1. Simpan PATH (relatif) untuk keperluan DELETE nanti
                        // Contoh isi: "products/foto1.jpg"
                        data.images.forEach { path ->
                            originalImageUrls.add(path)
                        }

                        // 2. Simpan URL (lengkap) untuk TAMPILAN di HP (CDN)
                        // Contoh isi: "https://pub-r2..../products/foto1.jpg"
                        // Ini yang bikin gambar MUNCUL di HP Asli
                        data.imageUrls.forEach { url ->
                            previewAlbumUris.add(Uri.parse(url))
                        }

                        albumAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                // Error handling silent
            }
        }
    }
    // -----------------------------------

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnUpload.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { updateProduct() }
        btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE
            imgPreviewReal.setImageURI(uri)
            tvUploadLabel.text = "Gambar Baru Terpilih"
            selectedFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            rvAlbumPreview.visibility = View.VISIBLE
            for (uri in uris) {
                val file = FileUtils.getFileFromUri(this, uri)
                if (file != null) {
                    selectedNewAlbumFiles.add(file)
                    previewAlbumUris.add(uri)
                }
            }
            albumAdapter.notifyDataSetChanged()
        }
    }

    private fun handleRemoveAlbumItem(position: Int) {
        val totalOldImages = originalImageUrls.size

        if (position < totalOldImages) {
            // Hapus gambar lama
            deletedImagePaths.add(originalImageUrls[position])
            originalImageUrls.removeAt(position)
        } else {
            // Hapus gambar baru
            val idx = position - totalOldImages
            if (idx >= 0 && idx < selectedNewAlbumFiles.size) {
                selectedNewAlbumFiles.removeAt(idx)
            }
        }

        previewAlbumUris.removeAt(position)
        albumAdapter.notifyItemRemoved(position)
        if (previewAlbumUris.isEmpty()) rvAlbumPreview.visibility = View.GONE
    }

    private fun updateProduct() {
        val name = etName.text.toString().trim()
        val priceRaw = etPrice.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (name.isEmpty() || priceRaw.isEmpty()) {
            Toast.makeText(this, "Data wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return
        val priceClean = priceRaw.replace(".", "").replace(",", "")

        lifecycleScope.launch {
            try {
                setLoading(true)

                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPrice = priceClean.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                // 1. Gambar Utama
                var bodyImage: MultipartBody.Part? = null
                if (selectedFile != null) {
                    val reqFile = selectedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                    bodyImage = MultipartBody.Part.createFormData("image_path", selectedFile!!.name, reqFile)
                }

                // 2. Album Baru
                val newImagesParts = mutableListOf<MultipartBody.Part>()
                for (file in selectedNewAlbumFiles) {
                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("images[]", file.name, reqFile)
                    newImagesParts.add(part)
                }

                // 3. Album Dihapus
                val deletedParts = mutableListOf<MultipartBody.Part>()
                for (path in deletedImagePaths) {
                    val body = path.toRequestBody("text/plain".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("deleted_images[]", null, body)
                    deletedParts.add(part)
                }

                val response = ApiClient.instance.updateProduct(
                    "Bearer $token",
                    productId,
                    reqName,
                    reqPrice,
                    reqDesc,
                    bodyImage,
                    if (newImagesParts.isNotEmpty()) newImagesParts else null,
                    if (deletedParts.isNotEmpty()) deletedParts else null,
                    reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EditProdukActivity, "Update Berhasil!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(this@EditProdukActivity, "Gagal: $errorMsg", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@EditProdukActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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