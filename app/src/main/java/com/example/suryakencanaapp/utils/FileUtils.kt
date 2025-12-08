package com.example.suryakencanaapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver

            // 1. CEK TIPE FILE (PNG atau JPG?)
            val mimeType = contentResolver.getType(uri)
            val isPng = mimeType == "image/png"
            val extension = if (isPng) ".png" else ".jpg"

            // 2. Buat nama file sesuai ekstensi aslinya
            val fileName = "upload_temp_${System.currentTimeMillis()}$extension"
            val tempFile = File(context.cacheDir, fileName)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // 3. PROSES KOMPRESI (Kirim info apakah ini PNG)
            val compressedFile = compressImage(tempFile, isPng)

            Log.d("FileUtils", "Size Akhir: ${compressedFile.length() / 1024} KB")

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImage(file: File, isPng: Boolean): File {
        try {
            // 1. Cek Ukuran Asli
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.path, options)

            // 2. Hitung Skala (Resize tetap dilakukan agar HP tidak berat)
            options.inSampleSize = calculateInSampleSize(options, 1280, 1280)

            // 3. Decode Ulang
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(file.path, options)

            // 4. Siapkan Stream
            val outStream = ByteArrayOutputStream()

            // --- PERBAIKAN UTAMA DI SINI ---
            if (isPng) {
                // Jika PNG, gunakan CompressFormat.PNG agar transparan TIDAK HILANG
                // Note: PNG mengabaikan parameter quality (tetap lossless), jadi file mungkin agak lebih besar dari JPG
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            } else {
                // Jika JPG, kompres 70% biar kecil
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, outStream)
            }

            // 5. Timpa file lama
            val fileOut = FileOutputStream(file)
            fileOut.write(outStream.toByteArray())
            fileOut.flush()
            fileOut.close()

            return file
        } catch (e: Exception) {
            Log.e("FileUtils", "Gagal kompres: ${e.message}")
            return file
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}