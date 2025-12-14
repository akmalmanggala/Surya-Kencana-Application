package com.example.suryakencanaapp

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.suryakencanaapp.adapter.HeroAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentHeroBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class HeroFragment : Fragment() {

    private var _binding: FragmentHeroBinding? = null
    private val binding get() = _binding!!
    private lateinit var heroImageAdapter: HeroAdapter
    private var loadingDialog: android.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHeroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHeroImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        heroImageAdapter = HeroAdapter(listOf(), listOf()) { pathToDelete ->
            showDeleteConfirmation(pathToDelete)
        }
        binding.rvHeroImages.adapter = heroImageAdapter

        binding.btnAddImage.setOnClickListener { openGallery() }
        binding.btnSaveHero.setOnClickListener { saveChanges() }

        binding.swipeRefresh.setOnRefreshListener {
            fetchHeroData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.post { fetchHeroData() }
    }

    // --- HELPER LOADING ---
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            if (loadingDialog == null) {
                val builder = android.app.AlertDialog.Builder(requireContext())
                val view = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
                builder.setView(view)
                builder.setCancelable(false) // User tidak bisa back
                loadingDialog = builder.create()
                // Agar background transparan (hanya card yang terlihat)
                loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            loadingDialog?.show()
            binding.btnSaveHero.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSaveHero.isEnabled = true
        }
    }

    private fun fetchHeroData() {
        _binding?.swipeRefresh?.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getHero()
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    if (list.isNotEmpty()) {
                        val data = list[0]
                        binding.etLocation.setText(data.location)
                        binding.etHeroTitle.setText(data.title)
                        binding.etStatMachine.setText(data.machines?.toString() ?: "0")
                        binding.etStatClient.setText(data.clients?.toString() ?: "0")
                        binding.etStatCustomer.setText(data.customers?.toString() ?: "0")
                        binding.etStatExp.setText(data.experienceYears?.toString() ?: "0")
                        binding.etStatTrust.setText(data.trustYears?.toString() ?: "0")

                        val urls = data.backgroundUrls ?: listOf()
                        val paths = data.backgroundPaths ?: listOf()
                        heroImageAdapter.updateData(urls, paths)
                    }
                }
            } catch (e: Exception) {
                Log.e("HERO_API", "Error: ${e.message}")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun saveChanges() {
        updateHeroApi(null, null, "save_text")
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val file = FileUtils.getFileFromUri(requireContext(), uri)
            if (file != null) {
                updateHeroApi(file, null, "upload_image")
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showDeleteConfirmation(path: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Gambar")
            .setMessage("Yakin ingin menghapus gambar ini dari slider?")
            .setPositiveButton("Hapus") { _, _ ->
                updateHeroApi(null, path, "delete_image")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateHeroApi(newImageFile: File?, deletedPath: String?, actionType: String) {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay Loading
                setLoading(true)

                val location = createPart(binding.etLocation.text.toString())
                val title = createPart(binding.etHeroTitle.text.toString())
                val machine = createPart(binding.etStatMachine.text.toString())
                val client = createPart(binding.etStatClient.text.toString())
                val customer = createPart(binding.etStatCustomer.text.toString())
                val exp = createPart(binding.etStatExp.text.toString())
                val trust = createPart(binding.etStatTrust.text.toString())

                val newImagesList = if (newImageFile != null) {
                    val reqFile = newImageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    listOf(MultipartBody.Part.createFormData("backgrounds[]", newImageFile.name, reqFile))
                } else {
                    null
                }

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
                    val successMsg = when (actionType) {
                        "upload_image" -> "Berhasil Menambah Gambar!"
                        "delete_image" -> "Berhasil Menghapus Gambar!"
                        else -> "Hero Berhasil Diperbarui!"
                    }
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    fetchHeroData()
                } else {
                    val errorMsg = when (actionType) {
                        "upload_image" -> "Gagal Mengupload Gambar!"
                        "delete_image" -> "Gagal Menghapus Gambar!"
                        else -> "Gagal Menyimpan Data!"
                    }
                    val serverError = response.errorBody()?.string()
                    Log.e("HERO_UPDATE", "Server Error: $serverError")
                    Toast.makeText(context, "$errorMsg (${response.code()})", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HERO_UPDATE", "Exception: ${e.message}")
            } finally {
                // 2. Sembunyikan Overlay
                setLoading(false)
            }
        }
    }

    private fun createPart(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}