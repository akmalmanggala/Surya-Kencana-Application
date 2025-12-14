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
    private val selectedAlbumFiles = mutableListOf<File>()
    private val selectedAlbumUris = mutableListOf<Uri>()
    private lateinit var albumAdapter: AlbumImageAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProdukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
    }

    private fun initViews() {
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

        binding.tvPageTitle.text = "Tambah Produk Baru"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { uploadProduct() }
        binding.btnUploadImage.setOnClickListener { mainImageLauncher.launch("image/*") }
        binding.btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }

        // --- FITUR BARU: Listener Checkbox ---
        binding.cbHidePrice.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.tvHiddenInfo.visibility = View.VISIBLE
            } else {
                binding.tvHiddenInfo.visibility = View.GONE
            }
        }
    }

    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE
            binding.imgPreviewReal.setImageURI(uri)
            binding.tvUploadInfo.text = "Gambar Utama Terpilih"
            binding.tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"
            selectedFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            binding.rvAlbumPreview.visibility = View.VISIBLE
            for (uri in uris) {
                val file = FileUtils.getFileFromUri(this, uri)
                if (file != null) {
                    selectedAlbumFiles.add(file)
                    selectedAlbumUris.add(uri)
                }
            }
            albumAdapter.notifyDataSetChanged()
            Toast.makeText(this, "${uris.size} gambar ditambahkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProduct() {
        val name = binding.etName.text.toString().trim()
        val priceRaw = binding.etPrice.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()
        val isHidden = binding.cbHidePrice.isChecked

        // --- VALIDASI (Sesuai Logic Vue) ---
        // Jika hidden, harga boleh kosong. Jika tidak hidden, harga wajib isi.
        if (name.isEmpty() || desc.isEmpty() || selectedFile == null) {
            Toast.makeText(this, "Nama, Deskripsi, dan Gambar Utama wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isHidden && priceRaw.isEmpty()) {
            Toast.makeText(this, "Harga wajib diisi (kecuali disembunyikan)!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: return
        val authHeader = "Bearer $token"

        // Bersihkan harga (hapus titik/koma)
        val priceClean = if (priceRaw.isNotEmpty()) {
            priceRaw.replace(".", "").replace(",", "")
        } else {
            "0" // Default jika kosong (saat hidden)
        }

        lifecycleScope.launch {
            try {
                setLoading(true)

                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPrice = priceClean.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())

                // Hide Price (0 = false, 1 = true)
                val hidePriceStr = if (isHidden) "1" else "0"
                val reqHidePrice = hidePriceStr.toRequestBody("text/plain".toMediaTypeOrNull())

                val mimeTypeMain = if (selectedFile!!.extension.equals("png", true)) "image/png" else "image/jpeg"
                val requestFile = selectedFile!!.asRequestBody(mimeTypeMain.toMediaTypeOrNull())
                val bodyMainImage = MultipartBody.Part.createFormData("image_path", selectedFile!!.name, requestFile)

                val albumParts = mutableListOf<MultipartBody.Part>()
                for (file in selectedAlbumFiles) {
                    val mimeTypeAlbum = if (file.extension.equals("png", true)) "image/png" else "image/jpeg"
                    val reqFileAlbum = file.asRequestBody(mimeTypeAlbum.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("images[]", file.name, reqFileAlbum)
                    albumParts.add(part)
                }

                val response = ApiClient.instance.addProduct(
                    authHeader,
                    reqName,
                    reqPrice,
                    reqDesc,
                    reqHidePrice,
                    bodyMainImage,
                    if (albumParts.isEmpty()) null else albumParts
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@AddProductActivity, "Produk Berhasil Ditambahkan!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("UPLOAD_ERROR", errorMsg ?: "Unknown error")
                    Toast.makeText(this@AddProductActivity, "Gagal: $errorMsg", Toast.LENGTH_SHORT).show()
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