package com.example.potatoapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object Utils {

    // Convert Uri to File
    fun uriToFile(uri: Uri, context: Context): File {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: throw IOException("Cannot open input stream from URI")

        val tempFile = createFile(context)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        return tempFile
    }

    // Create a temporary file
    fun createFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // Compress Image to ensure it's under 2MB
    fun compressImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: throw IOException("Failed to decode image")
        var compressQuality = 100
        var streamLength: Int

        do {
            val bmpStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
            val bmpPicByteArray = bmpStream.toByteArray()
            streamLength = bmpPicByteArray.size
            compressQuality -= 5
            bmpStream.close()
        } while (streamLength > 2_000_000 && compressQuality > 5)

        // Save the compressed image
        val compressedFile = File(file.parent, "COMPRESSED_${file.name}")
        compressedFile.outputStream().use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
        }

        // Pastikan ukuran file setelah kompresi
        if (compressedFile.length() > 2_000_000) {
            throw IOException("Gagal mengompresi gambar di bawah 2MB")
        }

        return compressedFile
    }
}
