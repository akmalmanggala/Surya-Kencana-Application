package com.example.suryakencanaapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.suryakencanaapp.adapter.MisiAdapter
import com.example.suryakencanaapp.api.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VisiMisiFragment : Fragment(R.layout.fragment_visi_misi) {

    private lateinit var etVisi: TextInputEditText
    private lateinit var rvMisi: RecyclerView
    private lateinit var btnAddMisiPoint: MaterialButton
    private lateinit var btnSave: MaterialButton

    private lateinit var misiAdapter: MisiAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init View
        etVisi = view.findViewById(R.id.etVisi)
        rvMisi = view.findViewById(R.id.rvMisi)
        btnAddMisiPoint = view.findViewById(R.id.btnAddMisiPoint)
        btnSave = view.findViewById(R.id.btnSaveVisiMisi)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // 2. Setup RecyclerView Misi
        rvMisi.layoutManager = LinearLayoutManager(context)
        // Init adapter dengan list kosong dulu
        misiAdapter = MisiAdapter(mutableListOf())
        rvMisi.adapter = misiAdapter

        // 3. Tombol Tambah Poin Misi
        btnAddMisiPoint.setOnClickListener {
            misiAdapter.addEmptyItem()
        }

        // 4. Tombol Simpan
        btnSave.setOnClickListener {
            saveData()
        }
        swipeRefresh.setOnRefreshListener {
            fetchData()
        }
    }

    private fun fetchData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getVisiMisi()

                if (response.isSuccessful && response.body() != null) {
                    val listData = response.body()!!

                    if (listData.isNotEmpty()) {
                        val data = listData[0]

                        // 1. ISI VISI (Ini sudah benar)
                        etVisi.setText(data.vision)

                        // 2. ISI MISI (PERBAIKAN LOGIKA DI SINI)
                        val rawMission = data.mission ?: ""

                        val missionList = try {
                            // Coba parsing sebagai JSON Array ["A", "B", "C"]
                            val type = object : TypeToken<List<String>>() {}.type
                            Gson().fromJson<List<String>>(rawMission, type).toMutableList()
                        } catch (e: Exception) {
                            // Jika gagal (mungkin format teks biasa dipisah Enter), coba split manual
                            if (rawMission.contains("\n")) {
                                rawMission.split("\n").map { it.trim() }.toMutableList()
                            } else {
                                // Jika tidak ada enter dan bukan JSON, anggap 1 baris string
                                mutableListOf(rawMission)
                            }
                        }

                        // Update Adapter agar muncul banyak baris
                        misiAdapter.updateData(missionList)
                    }
                }
            } catch (e: Exception) {
                Log.e("VISIMISI", "Error: ${e.message}")
            }finally {
                swipeRefresh.isRefreshing = false
                }
        }
    }

    override fun onResume() {
        super.onResume()
        swipeRefresh.post {
            fetchData()
        }
    }
    private fun saveData() {
        val visi = etVisi.text.toString().trim()

        // Ambil list misi dari adapter
        val listMisi = misiAdapter.getMisiData()

        // Filter: Hapus baris yang kosong agar tidak nyampah
        val validMisi = listMisi.filter { it.isNotBlank() }

        val misiJsonString = Gson().toJson(validMisi)

        if (visi.isEmpty()) {
            Toast.makeText(context, "Visi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil Token
        val prefs = requireActivity().getSharedPreferences("AppSession", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        lifecycleScope.launch {
            try {
                btnSave.isEnabled = false
                btnSave.text = "Updating..."

                val response = ApiClient.instance.updateVisiMisi(
                    "Bearer $token",
                    visi,
                    misiJsonString // KIRIM SEBAGAI JSON STRING
                )

                if (response.isSuccessful) {
                    Toast.makeText(context, "Visi Misi Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Simpan Perubahan"
            }
        }
    }
}