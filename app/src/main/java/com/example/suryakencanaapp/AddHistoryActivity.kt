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

class AddHistoryActivity : AppCompatActivity() {

    // Variabel UI Text
    private lateinit var etYear: TextInputEditText
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // Variabel UI Gambar Utama
    private lateinit var btnUpload: LinearLayout
    private lateinit var tvUploadLabel: TextView
    private lateinit var imgUploadIcon: ImageView
    private lateinit var imgPreviewReal: ImageView
    private lateinit var tvUploadInfo2: TextView

    // Variabel UI Gambar Album (BARU)
    private lateinit var btnUploadAlbum: LinearLayout
    private lateinit var rvAlbumPreview: RecyclerView
    private lateinit var albumAdapter: AlbumImageAdapter

    // Variabel Data
    private var selectedMainFile: File? = null // Ganti nama agar jelas (Gambar Utama)
    private val selectedAlbumFiles = mutableListOf<File>() // List File Album
    private val selectedAlbumUris = mutableListOf<Uri>()   // List URI Preview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_history)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etYear = findViewById(R.id.etYear)
        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        tvUploadInfo2 = findViewById(R.id.tvUploadInfo2)


        // Init Gambar Utama
        btnUpload = findViewById(R.id.btnUploadImage)
        tvUploadLabel = findViewById(R.id.tvUploadInfo)
        imgUploadIcon = findViewById(R.id.imgUploadIcon)
        imgPreviewReal = findViewById(R.id.imgPreviewReal)

        // Init Gambar Album (Pastikan ID ini ada di XML activity_add_history.xml)
        // Jika belum ada, copy XML bagian album dari activity_add_produk.xml
        try {
            btnUploadAlbum = findViewById(R.id.btnUploadAlbum)
            rvAlbumPreview = findViewById(R.id.rvAlbumPreview)

            // Setup Adapter Album
            rvAlbumPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            albumAdapter = AlbumImageAdapter(selectedAlbumUris) { position ->
                // Logic Hapus Item dari Album
                selectedAlbumUris.removeAt(position)
                selectedAlbumFiles.removeAt(position)
                albumAdapter.notifyItemRemoved(position)

                if (selectedAlbumUris.isEmpty()) {
                    rvAlbumPreview.visibility = View.GONE
                }
            }
            rvAlbumPreview.adapter = albumAdapter
        } catch (e: Exception) {
            // Jaga-jaga jika XML belum diupdate
        }
    }

    private fun setupListeners() {
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { uploadHistory() }

        // Listener Gambar Utama
        btnUpload.setOnClickListener { mainImageLauncher.launch("image/*") }

        // Listener Gambar Album (Multiple)
        try {
            btnUploadAlbum.setOnClickListener { albumImageLauncher.launch("image/*") }
        } catch (e: Exception) { }
    }

    // --- 1. LAUNCHER GAMBAR UTAMA (Single) ---
    private val mainImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imgUploadIcon.visibility = View.GONE
            imgPreviewReal.visibility = View.VISIBLE
            imgPreviewReal.setImageURI(uri)
            tvUploadLabel.text = "Gambar Utama Terpilih"
            tvUploadInfo2.text = "Tekan lagi untuk mengganti gambar"
            selectedMainFile = FileUtils.getFileFromUri(this, uri)
        }
    }

    // --- 2. LAUNCHER GAMBAR ALBUM (Multiple) ---
    private val albumImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            rvAlbumPreview.visibility = View.VISIBLE
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
        val year = etYear.text.toString().trim()
        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

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
            btnSave.isEnabled = false
            btnSave.text = "Uploading..."
        } else {
            btnSave.isEnabled = true
            btnSave.text = "Tambah Riwayat"
        }
    }
}