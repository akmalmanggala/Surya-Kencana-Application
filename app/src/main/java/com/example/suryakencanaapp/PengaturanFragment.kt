package com.example.suryakencanaapp

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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.suryakencanaapp.api.ApiClient
import com.example.suryakencanaapp.databinding.FragmentPengaturanBinding
import com.example.suryakencanaapp.utils.FileUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PengaturanFragment : Fragment() {

    private var _binding: FragmentPengaturanBinding? = null
    private val binding get() = _binding!!
    private var selectedLogoFile: File? = null
    private var loadingDialog: android.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengaturanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUploadLogo.setOnClickListener { openGallery() }
        binding.btnSaveSettings.setOnClickListener { saveSettings() }

        binding.swipeRefresh.setOnRefreshListener {
            fetchSettings()
        }

        fetchSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- HELPER LOADING OVERLAY ---
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
            binding.btnSaveSettings.isEnabled = false
        } else {
            loadingDialog?.dismiss()
            binding.btnSaveSettings.isEnabled = true
        }
    }

    // --- GET DATA ---
    private fun fetchSettings() {
        _binding?.swipeRefresh?.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getSiteSettings()
                if (_binding != null && response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    binding.etCompanyName.setText(data.companyName)
                    binding.etHeroTitle.setText(data.heroTitle)
                    binding.etHeroSubtitle.setText(data.heroSubtitle)
                    binding.etVisionLabel.setText(data.visionLabel)
                    binding.etVisionTitle.setText(data.visionTitle)
                    binding.etProductLabel.setText(data.productLabel)
                    binding.etProductTitle.setText(data.productTitle)
                    binding.etClientLabel.setText(data.clientLabel)
                    binding.etClientTitle.setText(data.clientTitle)
                    binding.etHistoryLabel.setText(data.historyLabel)
                    binding.etHistoryTitle.setText(data.historyTitle)
                    binding.etTestiLabel.setText(data.testiLabel)
                    binding.etTestiTitle.setText(data.testiTitle)
                    binding.etContactLabel.setText(data.contactLabel)
                    binding.etContactTitle.setText(data.contactTitle)

                    if (!data.companyLogoUrl.isNullOrEmpty()) {
                        showPreview(true)
                        Glide.with(this@PengaturanFragment)
                            .load(data.companyLogoUrl)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.imgLogoPreview)
                        binding.tvUploadInfo.text = "Logo Saat Ini (Ketuk untuk ganti)"
                    }
                }
            } catch (e: Exception) {
                Log.e("SETTINGS_API", "Error: ${e.message}")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    // --- SAVE DATA ---
    private fun saveSettings() {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                // 1. Tampilkan Overlay
                setLoading(true)

                val reqCompany = createPart(binding.etCompanyName)
                val reqHeroT = createPart(binding.etHeroTitle)
                val reqHeroS = createPart(binding.etHeroSubtitle)
                val reqVisL = createPart(binding.etVisionLabel)
                val reqVisT = createPart(binding.etVisionTitle)
                val reqProdL = createPart(binding.etProductLabel)
                val reqProdT = createPart(binding.etProductTitle)
                val reqCliL = createPart(binding.etClientLabel)
                val reqCliT = createPart(binding.etClientTitle)
                val reqHisL = createPart(binding.etHistoryLabel)
                val reqHisT = createPart(binding.etHistoryTitle)
                val reqTesL = createPart(binding.etTestiLabel)
                val reqTesT = createPart(binding.etTestiTitle)
                val reqConL = createPart(binding.etContactLabel)
                val reqConT = createPart(binding.etContactTitle)

                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                var bodyLogo: MultipartBody.Part? = null
                if (selectedLogoFile != null) {
                    val isPng = selectedLogoFile!!.extension.equals("png", true)
                    val mimeType = if (isPng) "image/png" else "image/jpeg"
                    val extension = if (isPng) "png" else "jpg"
                    val safeFileName = "company_logo_${System.currentTimeMillis()}.$extension"
                    val requestFile = selectedLogoFile!!.asRequestBody(mimeType.toMediaTypeOrNull())
                    bodyLogo = MultipartBody.Part.createFormData("company_logo", safeFileName, requestFile)
                }

                val response = ApiClient.instance.updateSiteSettings(
                    "Bearer $token",
                    reqCompany, reqHeroT, reqHeroS,
                    reqVisL, reqVisT,
                    reqProdL, reqProdT,
                    reqCliL, reqCliT,
                    reqHisL, reqHisT,
                    reqTesL, reqTesT,
                    reqConL, reqConT,
                    bodyLogo,
                    reqMethod
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, "Pengaturan Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                    selectedLogoFile = null
                    fetchSettings()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UPDATE_FAIL", "Error Server: $errorBody")
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                // 2. Sembunyikan Overlay
                setLoading(false)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            showPreview(true)
            binding.imgLogoPreview.setImageURI(uri)
            binding.tvUploadInfo.text = "Logo Baru Terpilih (Belum Disimpan)"
            selectedLogoFile = FileUtils.getFileFromUri(requireContext(), uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun showPreview(show: Boolean) {
        if (show) {
            binding.imgUploadIcon.visibility = View.GONE
            binding.imgLogoPreview.visibility = View.VISIBLE
        } else {
            binding.imgUploadIcon.visibility = View.VISIBLE
            binding.imgLogoPreview.visibility = View.GONE
        }
    }

    private fun createPart(editText: android.widget.EditText): RequestBody {
        val text = editText.text.toString().trim()
        return text.toRequestBody("text/plain".toMediaTypeOrNull())
    }
}