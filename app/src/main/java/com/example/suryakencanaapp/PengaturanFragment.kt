package com.example.suryakencanaapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
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

class PengaturanFragment : Fragment(R.layout.fragment_pengaturan) { // Pastikan XML benar

    // UI Variables
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etHeroTitle: TextInputEditText
    private lateinit var etHeroSubtitle: TextInputEditText

    private lateinit var etVisionLabel: TextInputEditText
    private lateinit var etVisionTitle: TextInputEditText

    private lateinit var etProductLabel: TextInputEditText
    private lateinit var etProductTitle: TextInputEditText

    private lateinit var etClientLabel: TextInputEditText
    private lateinit var etClientTitle: TextInputEditText

    private lateinit var etHistoryLabel: TextInputEditText
    private lateinit var etHistoryTitle: TextInputEditText

    private lateinit var etTestiLabel: TextInputEditText
    private lateinit var etTestiTitle: TextInputEditText

    private lateinit var etContactLabel: TextInputEditText
    private lateinit var etContactTitle: TextInputEditText

    private lateinit var btnUploadLogo: LinearLayout
    private lateinit var imgLogoPreview: ImageView
    private lateinit var imgUploadIcon: ImageView
    private lateinit var tvUploadInfo: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var swipeRefresh: SwipeRefreshLayout


    // Data File
    private var selectedLogoFile: File? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        // Listeners
        btnUploadLogo.setOnClickListener { openGallery() }
        btnSave.setOnClickListener { saveSettings() }

        swipeRefresh.setOnRefreshListener {
            fetchSettings()
        }

        // Fetch Data
        fetchSettings()
    }

    private fun initViews(view: View) {
        etCompanyName = view.findViewById(R.id.etCompanyName)
        etHeroTitle = view.findViewById(R.id.etHeroTitle)
        etHeroSubtitle = view.findViewById(R.id.etHeroSubtitle)

        etVisionLabel = view.findViewById(R.id.etVisionLabel)
        etVisionTitle = view.findViewById(R.id.etVisionTitle)

        etProductLabel = view.findViewById(R.id.etProductLabel)
        etProductTitle = view.findViewById(R.id.etProductTitle)

        etClientLabel = view.findViewById(R.id.etClientLabel)
        etClientTitle = view.findViewById(R.id.etClientTitle)

        etHistoryLabel = view.findViewById(R.id.etHistoryLabel)
        etHistoryTitle = view.findViewById(R.id.etHistoryTitle)

        etTestiLabel = view.findViewById(R.id.etTestiLabel)
        etTestiTitle = view.findViewById(R.id.etTestiTitle)

        etContactLabel = view.findViewById(R.id.etContactLabel)
        etContactTitle = view.findViewById(R.id.etContactTitle)

        btnUploadLogo = view.findViewById(R.id.btnUploadLogo)
        imgLogoPreview = view.findViewById(R.id.imgLogoPreview)
        imgUploadIcon = view.findViewById(R.id.imgUploadIcon)
        tvUploadInfo = view.findViewById(R.id.tvUploadInfo)
        btnSave = view.findViewById(R.id.btnSaveSettings)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }

    override fun onResume() {
        super.onResume()
        swipeRefresh.post {
            fetchSettings()
        }
    }

    // --- GET DATA ---
    private fun fetchSettings() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getSiteSettings()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Isi Text Fields
                    etCompanyName.setText(data.companyName)
                    etHeroTitle.setText(data.heroTitle)
                    etHeroSubtitle.setText(data.heroSubtitle)

                    etVisionLabel.setText(data.visionLabel)
                    etVisionTitle.setText(data.visionTitle)

                    etProductLabel.setText(data.productLabel)
                    etProductTitle.setText(data.productTitle)

                    etClientLabel.setText(data.clientLabel)
                    etClientTitle.setText(data.clientTitle)

                    etHistoryLabel.setText(data.historyLabel)
                    etHistoryTitle.setText(data.historyTitle)

                    etTestiLabel.setText(data.testiLabel)
                    etTestiTitle.setText(data.testiTitle)

                    etContactLabel.setText(data.contactLabel)
                    etContactTitle.setText(data.contactTitle)

                    // Isi Logo (Jika ada)
                    if (!data.companyLogoUrl.isNullOrEmpty()) {
                        showPreview(true)
                        Glide.with(this@PengaturanFragment)
                            .load(data.companyLogoUrl)
                            .into(imgLogoPreview)
                        tvUploadInfo.text = "Logo Saat Ini (Ketuk untuk ganti)"
                    }
                }
            } catch (e: Exception) {
                Log.e("SETTINGS_API", "Error: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // --- SAVE DATA ---
    private fun saveSettings() {
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: return

        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Menyimpan..."

                // 1. Siapkan semua text
                val reqCompany = createPart(etCompanyName)
                val reqHeroT = createPart(etHeroTitle)
                val reqHeroS = createPart(etHeroSubtitle)
                val reqVisL = createPart(etVisionLabel)
                val reqVisT = createPart(etVisionTitle)
                val reqProdL = createPart(etProductLabel)
                val reqProdT = createPart(etProductTitle)
                val reqCliL = createPart(etClientLabel)
                val reqCliT = createPart(etClientTitle)
                val reqHisL = createPart(etHistoryLabel)
                val reqHisT = createPart(etHistoryTitle)
                val reqTesL = createPart(etTestiLabel)
                val reqTesT = createPart(etTestiTitle)
                val reqConL = createPart(etContactLabel)
                val reqConT = createPart(etContactTitle)

                val reqMethod = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. Siapkan Logo (Jika ada file baru)
                var bodyLogo: MultipartBody.Part? = null
                if (selectedLogoFile != null) {
                    val mimeType = if (selectedLogoFile!!.extension.equals("png", true)) "image/png" else "image/jpeg"
                    val requestFile = selectedLogoFile!!.asRequestBody(mimeType.toMediaTypeOrNull())
                    // Nama field 'company_logo' sesuai Controller
                    bodyLogo = MultipartBody.Part.createFormData("company_logo", selectedLogoFile!!.name, requestFile)
                }

                // 3. Kirim
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
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Pengaturan"
            }
        }
    }

    // --- UPLOAD GAMBAR ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            showPreview(true)
            imgLogoPreview.setImageURI(uri)
            tvUploadInfo.text = "Logo Baru Terpilih"

            selectedLogoFile = FileUtils.getFileFromUri(requireContext(), uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // Helper untuk UI Preview
    private fun showPreview(show: Boolean) {
        if (show) {
            // Jika ada logo: Sembunyikan panah, Munculkan gambar logo
            imgUploadIcon.visibility = View.GONE
            imgLogoPreview.visibility = View.VISIBLE
        } else {
            // Jika kosong: Munculkan panah, Sembunyikan tempat logo
            imgUploadIcon.visibility = View.VISIBLE
            imgLogoPreview.visibility = View.GONE
        }
    }

    // Helper untuk Text RequestBody
    private fun createPart(editText: EditText): RequestBody {
        return editText.text.toString().trim().toRequestBody("text/plain".toMediaTypeOrNull())
    }
}