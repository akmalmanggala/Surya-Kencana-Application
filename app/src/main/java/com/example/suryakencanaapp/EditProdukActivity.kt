package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
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

class EditProdukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProdukBinding
    private lateinit var albumAdapter: AlbumImageAdapter

    private val selectedNewAlbumFiles = mutableListOf<File>()
    private val previewAlbumUris = mutableListOf<Uri>()
    private val deletedImagePaths = mutableListOf<String>()
    private val originalImageUrls = mutableListOf<String>()

    private var productId: Int = 0
    private var selectedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()

        try {
            binding.tvPageTitle.text = "Edit Produk"
        } catch (e: Exception) { }

        binding.btnSave.text = "Simpan Perubahan"

        loadDataFromIntent()
        loadAlbumFromApi()
        setupListeners()
    }

    private fun initViews() {
        binding.rvAlbumPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAlbumPreview.setHasFixedSize(true)
        binding.rvAlbumPreview.setItemViewCacheSize(10)

        albumAdapter = AlbumImageAdapter(previewAlbumUris) { position ->
            handleRemoveAlbumItem(position)
        }
        binding.rvAlbumPreview.adapter = albumAdapter
    }

    private fun loadDataFromIntent() {
        productId = intent.getIntExtra("ID", 0)
        binding.etName.setText(intent.getStringExtra("NAME"))
        binding.etDesc.setText(intent.getStringExtra("DESC"))

        val rawPrice = intent.getStringExtra("PRICE") ?: "0"
        val cleanPrice = rawPrice.substringBefore(".")
        binding.etPrice.setText(cleanPrice)
        
        // Load hide_price state (convert Int to Boolean)
        val hidePrice = intent.getIntExtra("HIDE_PRICE", 0)
        binding.cbHidePrice.isChecked = (hidePrice == 1)

        val oldImageUrl = intent.getStringExtra("IMAGE_URL")
        if (!oldImageUrl.isNullOrEmpty()) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE

            Glide.with(this)
                .load(oldImageUrl)
                .placeholder(R.drawable.package_2_24dp_ffffff_fill0_wght400_grad0_opsz24)
                .into(binding.imgPreviewReal)

            binding.tvUploadInfo.text = "Gambar Saat Ini (Ketuk untuk ganti)"
        }
    }

    private fun loadAlbumFromApi() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getProductDetail(productId)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    if (!data.images.isNullOrEmpty() && !data.imageUrls.isNullOrEmpty()) {
                        binding.rvAlbumPreview.visibility = View.VISIBLE

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
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnUploadImage.setOnClickListener { openGallery() }
        binding.btnSave.setOnClickListener { updateProduct() }
        binding.btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE
            binding.imgPreviewReal.setImageURI(uri)
            binding.tvUploadInfo.text = "Gambar Baru Terpilih"
            selectedFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            binding.rvAlbumPreview.visibility = View.VISIBLE
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
        albumAdapter.notifyItemRangeChanged(position, previewAlbumUris.size)
        if (previewAlbumUris.isEmpty()) binding.rvAlbumPreview.visibility = View.GONE
    }

    private fun updateProduct() {
        val name = binding.etName.text.toString().trim()
        val priceRaw = binding.etPrice.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()

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
                
                // Hide Price
                val hidePrice = if (binding.cbHidePrice.isChecked) "1" else "0"
                val reqHidePrice = hidePrice.toRequestBody("text/plain".toMediaTypeOrNull())
                
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
                    reqHidePrice, // TAMBAHAN: hide_price
                    bodyImage,
                    if (newImagesParts.isNotEmpty()) newImagesParts else null,
                    if (deletedParts.isNotEmpty()) deletedParts else null,
                    reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EditProdukActivity, "Produk Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
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
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "Updating..."
        } else {
            binding.btnSave.isEnabled = true
            binding.btnSave.text = "Simpan Perubahan"
        }
    }
}