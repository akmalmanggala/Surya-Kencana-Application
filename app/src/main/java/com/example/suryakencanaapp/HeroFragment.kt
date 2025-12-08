package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.suryakencanaapp.adapter.HeroAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.utils.FileUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class HeroFragment : Fragment(R.layout.fragment_hero) {

    // ... (Variabel UI tetap sama) ...
    private lateinit var etLocation: TextInputEditText
    private lateinit var etHeroTitle: TextInputEditText
    private lateinit var etStatMachine: TextInputEditText
    private lateinit var etStatCustomer: TextInputEditText
    private lateinit var etStatClient: TextInputEditText
    private lateinit var etStatExp: TextInputEditText
    private lateinit var etStatTrust: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnAddImage: LinearLayout
    private lateinit var rvHeroImages: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var heroImageAdapter: HeroAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        rvHeroImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        heroImageAdapter = HeroAdapter(listOf(), listOf()) { pathToDelete ->
            showDeleteConfirmation(pathToDelete)
        }
        rvHeroImages.adapter = heroImageAdapter

        btnAddImage.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveChanges() }

        swipeRefresh.setOnRefreshListener {
            fetchHeroData()
        }
    }

    // ... (initViews, onResume, fetchHeroData TETAP SAMA) ...
    private fun initViews(view: View) {
        etLocation = view.findViewById(R.id.etLocation)
        etHeroTitle = view.findViewById(R.id.etHeroTitle)
        etStatMachine = view.findViewById(R.id.etStatMachine)
        etStatCustomer = view.findViewById(R.id.etStatCustomer)
        etStatClient = view.findViewById(R.id.etStatClient)
        etStatExp = view.findViewById(R.id.etStatExp)
        etStatTrust = view.findViewById(R.id.etStatTrust)
        btnSave = view.findViewById(R.id.btnSaveHero)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        rvHeroImages = view.findViewById(R.id.rvHeroImages)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }

    override fun onResume() {
        super.onResume()
        swipeRefresh.post { fetchHeroData() }
    }

    private fun fetchHeroData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getHero()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    if (list.isNotEmpty()) {
                        val data = list[0]
                        etLocation.setText(data.location)
                        etHeroTitle.setText(data.title)
                        etStatMachine.setText(data.machines?.toString() ?: "0")
                        etStatClient.setText(data.clients?.toString() ?: "0")
                        etStatCustomer.setText(data.customers?.toString() ?: "0")
                        etStatExp.setText(data.experienceYears?.toString() ?: "0")
                        etStatTrust.setText(data.trustYears?.toString() ?: "0")

                        val urls = data.backgroundUrls ?: listOf()
                        val paths = data.backgroundPaths ?: listOf()
                        heroImageAdapter.updateData(urls, paths)
                    }
                }
            } catch (e: Exception) {
                Log.e("HERO_API", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // --- 1. SIMPAN PERUBAHAN TEKS ---
    private fun saveChanges() {
        updateHeroApi(
            newImageFile = null,
            deletedPath = null,
            actionType = "save_text" // Tipe Aksi: Simpan Teks
        )
    }

    // --- 2. TAMBAH GAMBAR ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = FileUtils.getFileFromUri(requireContext(), uri)
            if (file != null) {
                Toast.makeText(context, "Mengupload gambar...", Toast.LENGTH_SHORT).show()
                updateHeroApi(
                    newImageFile = file,
                    deletedPath = null,
                    actionType = "upload_image" // Tipe Aksi: Upload Gambar
                )
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // --- 3. HAPUS GAMBAR ---
    private fun showDeleteConfirmation(path: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Gambar")
            .setMessage("Yakin ingin menghapus gambar ini dari slider?")
            .setPositiveButton("Hapus") { _, _ ->
                Toast.makeText(context, "Menghapus gambar...", Toast.LENGTH_SHORT).show()
                updateHeroApi(
                    newImageFile = null,
                    deletedPath = path,
                    actionType = "delete_image" // Tipe Aksi: Hapus Gambar
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- UPDATE HERO API DENGAN PESAN DINAMIS ---
    // Tambahkan parameter 'actionType'
    private fun updateHeroApi(newImageFile: File?, deletedPath: String?, actionType: String) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Updating..."

                // 1. Siapkan Text Body
                val location = createPart(etLocation.text.toString())
                val title = createPart(etHeroTitle.text.toString())
                val machine = createPart(etStatMachine.text.toString())
                val client = createPart(etStatClient.text.toString())
                val customer = createPart(etStatCustomer.text.toString())
                val exp = createPart(etStatExp.text.toString())
                val trust = createPart(etStatTrust.text.toString())

                // 2. Siapkan Gambar Baru
                val newImagesList = if (newImageFile != null) {
                    val reqFile = newImageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    listOf(MultipartBody.Part.createFormData("backgrounds[]", newImageFile.name, reqFile))
                } else {
                    null
                }

                // 3. Siapkan Path Hapus
                val deletedImagesList = if (deletedPath != null) {
                    val pathBody = createPart(deletedPath)
                    listOf(MultipartBody.Part.createFormData("deleted_backgrounds[]", null, pathBody))
                } else {
                    null
                }

                val response = ApiClient.instance.updateHero(
                    "Bearer $token",
                    location, title, machine, client, customer, exp, trust,
                    newImagesList,
                    deletedImagesList
                )

                if (response.isSuccessful) {
                    // --- LOGIKA PESAN SUKSES ---
                    val successMsg = when (actionType) {
                        "upload_image" -> "Berhasil Menambah Gambar!"
                        "delete_image" -> "Berhasil Menghapus Gambar!"
                        else -> "Hero Berhasil Diperbarui!" // save_text
                    }
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()

                    fetchHeroData()
                } else {
                    // --- LOGIKA PESAN GAGAL ---
                    val errorMsg = when (actionType) {
                        "upload_image" -> "Gagal Mengupload Gambar!"
                        "delete_image" -> "Gagal Menghapus Gambar!"
                        else -> "Gagal Menyimpan Data!"
                    }

                    // (Opsional) Tampilkan error spesifik dari server di Logcat
                    val serverError = response.errorBody()?.string()
                    Log.e("HERO_UPDATE", "Server Error: $serverError")

                    Toast.makeText(context, "$errorMsg (${response.code()})", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                // --- LOGIKA PESAN ERROR KONEKSI ---
                val exMsg = when (actionType) {
                    "upload_image" -> "Error Upload: ${e.message}"
                    "delete_image" -> "Error Hapus: ${e.message}"
                    else -> "Error Simpan: ${e.message}"
                }
                Toast.makeText(context, exMsg, Toast.LENGTH_SHORT).show()
                Log.e("HERO_UPDATE", "Exception: ${e.message}")
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
            }
        }
    }

    private fun createPart(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}