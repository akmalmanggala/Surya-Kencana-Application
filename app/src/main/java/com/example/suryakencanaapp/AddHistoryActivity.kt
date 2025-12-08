package com.example.suryakencanaapp

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

class AddHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHistoryBinding
    private lateinit var albumAdapter: AlbumImageAdapter

    private var selectedMainFile: File? = null
    private val selectedAlbumFiles = mutableListOf<File>()
    private val selectedAlbumUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddHistoryBinding.inflate(layoutInflater)
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
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { uploadHistory() }
        binding.btnUploadImage.setOnClickListener { mainImageLauncher.launch("image/*") }

        try {
            binding.btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
        } catch (e: Exception) { }
    }

    // --- 1. LAUNCHER GAMBAR UTAMA (Single) ---
    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgPreviewReal.visibility = View.VISIBLE
            binding.imgPreviewReal.setImageURI(uri)
            binding.tvUploadInfo.text = "Gambar Utama Terpilih"
            binding.tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"
            selectedMainFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    // --- 2. LAUNCHER GAMBAR ALBUM (Multiple) ---
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

    // --- LOGIC UPLOAD KE SERVER ---
    private fun uploadHistory() {
        val year = binding.etYear.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()

        if (year.isEmpty() || title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AppSession", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "")
        if (token.isNullOrEmpty()) return

        lifecycleScope.launch {
            try {
                setLoading(true)

                // 1. Siapkan RequestBody (Text)
                val reqYear = year.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqTitle = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Siapkan Gambar Utama (Optional di History, tapi kalau ada diproses)
                var bodyMainImage: MultipartBody.Part? = null
                if (selectedMainFile != null) {
                    val mimeType = if (selectedMainFile!!.extension.equals("png", true)) "image/png" else "image/jpeg"
                    val requestFile = selectedMainFile!!.asRequestBody(mimeType.toMediaTypeOrNull())
                    // Controller History pakai 'image' (sesuai JSON Anda sebelumnya)
                    bodyMainImage = MultipartBody.Part.createFormData("image", selectedMainFile!!.name, requestFile)
                }

                // 3. Siapkan Gambar Album (List)
                val albumParts = mutableListOf<MultipartBody.Part>()
                for (file in selectedAlbumFiles) {
                    val mimeType = if (file.extension.equals("png", true)) "image/png" else "image/jpeg"
                    val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    // Controller History pakai 'images[]'
                    val part = MultipartBody.Part.createFormData("images[]", file.name, reqFile)
                    albumParts.add(part)
                }

                // 4. Kirim
                // Pastikan ApiService.addHistory parameternya sudah diupdate menerima List
                val response = ApiClient.instance.addHistory(
                    "Bearer $token",
                    reqYear,
                    reqTitle,
                    reqDesc,
                    bodyMainImage,
                    if (albumParts.isEmpty()) null else albumParts
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@AddHistoryActivity, "Riwayat Berhasil Ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(this@AddHistoryActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AddHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            binding.btnSave.text = "Tambah Riwayat"
        }
    }
}