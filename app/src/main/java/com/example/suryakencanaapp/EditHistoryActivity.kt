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

class EditHistoryActivity : AppCompatActivity() {

    // UI Variables
    private lateinit var etYear: TextInputEditText
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var tvPageTitle: TextView
    private lateinit var imgUploadIcon: ImageView
    private lateinit var imgPreviewReal: ImageView

    // Image UI (Album) - TAMBAHAN
    private lateinit var btnUploadAlbum: LinearLayout
    private lateinit var rvAlbumPreview: RecyclerView
    private lateinit var albumAdapter: AlbumImageAdapter

    // Data
    private var historyId: Int = 0
    private var selectedMainFile: File? = null // Gambar Utama Baru

    // Data Album - TAMBAHAN
    private val selectedNewAlbumFiles = mutableListOf<File>() // File baru (siap upload)
    private val previewAlbumUris = mutableListOf<Uri>()       // Preview (Gabungan lama & baru)
    private val deletedImagePaths = mutableListOf<String>()   // Path gambar lama yang dihapus
    private val originalImageUrls = mutableListOf<String>()   // Helper untuk melacak gambar lama

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_history) // Reuse Layout

        initViews()

        // Ubah Judul & Tombol
        try { tvPageTitle.text = "Edit Riwayat" } catch (e: Exception) {}
        btnSave.text = "Simpan Perubahan"

        // Load data lengkap dari API
        loadDataFromApi()
        setupListeners()
    }

    private fun initViews() {
        etYear = findViewById(R.id.etYear)
        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnUpload = findViewById(R.id.btnUploadImage)
        tvUploadLabel = findViewById(R.id.tvUploadInfo)
        imgUploadIcon = findViewById(R.id.imgUploadIcon)
        imgPreviewReal = findViewById(R.id.imgPreviewReal)
        tvPageTitle = findViewById(R.id.tvPageTitle)

        // Init Album UI
        // Pastikan ID ini sudah ada di XML activity_add_history.xml
        // (Copy dari activity_add_produk.xml jika belum ada)
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

    private fun loadDataFromApi() {
        historyId = intent.getIntExtra("ID", 0)

        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getHistoryDetail(historyId)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // ... (Isi teks tahun, judul, deskripsi, gambar utama) ...
                    etYear.setText(data.tahun.toString())
                    etTitle.setText(data.judul)
                    etDesc.setText(data.deskripsi)
                    if (!data.imageUrl.isNullOrEmpty()) {
                        imgUploadIcon.visibility = View.GONE
                        imgPreviewReal.visibility = View.VISIBLE
                        Glide.with(this@EditHistoryActivity).load(data.imageUrl).into(imgPreviewReal)
                        tvUploadLabel.text = "Gambar Saat Ini"
                    }

                    // --- ISI ALBUM ---
                    // Pastikan Model History punya field: images (List<String>) DAN imageUrls (List<String>)
                    if (!data.images.isNullOrEmpty() && !data.imageUrls.isNullOrEmpty()) {
                        rvAlbumPreview.visibility = View.VISIBLE

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
                // Fallback jika API gagal (isi minimal dari Intent)
                etYear.setText(intent.getStringExtra("TAHUN"))
                etTitle.setText(intent.getStringExtra("JUDUL"))
            }
        }
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnUpload.setOnClickListener { mainImageLauncher.launch("image/*") }
        btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
        btnSave.setOnClickListener { updateHistory() }
    }

    // --- LAUNCHER GAMBAR UTAMA ---
    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE
            imgPreviewReal.setImageURI(uri)
            tvUploadLabel.text = "Gambar Baru"
            selectedMainFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    // --- LAUNCHER GAMBAR ALBUM ---
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
        if (previewAlbumUris.isEmpty()) rvAlbumPreview.visibility = View.GONE
    }

    // --- LOGIC UPDATE KE SERVER ---
    private fun updateHistory() {
        val year = etYear.text.toString().trim()
        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

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
        btnSave.isEnabled = !isLoading
        btnSave.text = if (isLoading) "Updating..." else "Simpan Perubahan"
    }
}