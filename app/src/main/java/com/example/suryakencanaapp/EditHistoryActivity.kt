package com.example.suryakencanaapp

import android.app.AlertDialog
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
import com.example.suryakencanaapp.databinding.ActivityAddHistoryBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHistoryBinding
    private lateinit var albumAdapter: AlbumImageAdapter
    private var loadingDialog: AlertDialog? = null

    private var historyId: Int = 0
    private var selectedMainFile: File? = null

    private val selectedNewAlbumFiles = mutableListOf<File>()
    private val previewAlbumUris = mutableListOf<Uri>()
    private val deletedImagePaths = mutableListOf<String>()
    private val originalImageUrls = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()

        try { binding.tvPageTitle.text = "Edit Riwayat" } catch (e: Exception) {}
        binding.btnSave.text = "Simpan Perubahan"

        loadDataFromApi()
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

    private fun loadDataFromApi() {
        historyId = intent.getIntExtra("ID", 0)

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getHistoryDetail(historyId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    binding.etYear.setText(data.tahun.toString())
                    binding.etTitle.setText(data.judul)
                    binding.etDesc.setText(data.deskripsi)
                    if (!data.imageUrl.isNullOrEmpty()) {
                        binding.imgUploadIcon.visibility = View.GONE
                        binding.imgPreviewReal.visibility = View.VISIBLE
                        Glide.with(this@EditHistoryActivity).load(data.imageUrl).into(binding.imgPreviewReal)
                        binding.tvUploadInfo.text = "Gambar Saat Ini"
                    }

                    if (!data.images.isNullOrEmpty() && !data.imageUrls.isNullOrEmpty()) {
                        binding.rvAlbumPreview.visibility = View.VISIBLE

                        // Bersihkan list lama agar tidak duplikat
                        originalImageUrls.clear()
                        previewAlbumUris.clear()

                        // 1. Simpan PATH (relatif) untuk keperluan DELETE nanti
                        data.images.forEach { path ->
                            originalImageUrls.add(path)
                        }

                        // 2. Simpan URL (lengkap) untuk keperluan TAMPIL (Glide)
                        data.imageUrls.forEach { url ->
                            previewAlbumUris.add(Uri.parse(url))
                        }

                        albumAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                binding.etYear.setText(intent.getStringExtra("TAHUN"))
                binding.etTitle.setText(intent.getStringExtra("JUDUL"))
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnUploadImage.setOnClickListener { mainImageLauncher.launch("image/*") }
        binding.btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { updateHistory() }
    }

    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE
            binding.imgPreviewReal.setImageURI(uri)
            binding.tvUploadInfo.text = "Gambar Baru"
            selectedMainFile = FileUtils.getFileFromUri(this, uri)
        }
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

    // --- LOGIC HAPUS ITEM ALBUM ---
    private fun handleRemoveAlbumItem(position: Int) {
        val totalOldImages = originalImageUrls.size

        if (position < totalOldImages) {
            // Ini gambar lama -> Masukkan ke list delete
            deletedImagePaths.add(originalImageUrls[position])
            originalImageUrls.removeAt(position)
        } else {
            // Ini gambar baru -> Hapus dari list upload
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

    private fun updateHistory() {
        val year = binding.etYear.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()

        if (year.isEmpty() || title.isEmpty()) {
            Toast.makeText(this, "Data wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                setLoading(true)

                val reqYear = year.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqTitle = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                // 1. Gambar Utama (Opsional)
                var bodyMain: MultipartBody.Part? = null
                if (selectedMainFile != null) {
                    val reqFile = selectedMainFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                    bodyMain = MultipartBody.Part.createFormData("image", selectedMainFile!!.name, reqFile)
                }

                // 2. Gambar Album Baru (List)
                val newImagesParts = mutableListOf<MultipartBody.Part>()
                for (file in selectedNewAlbumFiles) {
                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("images[]", file.name, reqFile)
                    newImagesParts.add(part)
                }

                // 3. Gambar Dihapus (List)
                val deletedParts = mutableListOf<MultipartBody.Part>()
                for (path in deletedImagePaths) {
                    val body = path.toRequestBody("text/plain".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("deleted_images[]", null, body)
                    deletedParts.add(part)
                }

                // 4. Kirim (Sesuai ApiService yang baru)
                val response = ApiClient.instance.updateHistory(
                    "Bearer $token",
                    historyId,
                    reqYear,
                    reqTitle,
                    reqDesc,
                    bodyMain,
                    if (newImagesParts.isNotEmpty()) newImagesParts else null,
                    if (deletedParts.isNotEmpty()) deletedParts else null,
                    reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EditHistoryActivity, "Riwayat Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditHistoryActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@EditHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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