package com.example.potatoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.potatoapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()


    private val cameraActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra("imageUri")
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                viewModel.setImageUri(uri)
            }
        }
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { permission ->
            Log.d("PermissionsDebug", "Permission ${permission.key} granted: ${permission.value}")
        }

        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d("PermissionsDebug", "All permissions granted.")
            Toast.makeText(this, "Permissions are granted.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("PermissionsDebug", "Not all permissions granted.")
            Toast.makeText(this, "Not all permissions granted.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.INTERNET
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Memeriksa apakah harus memperlihatkan alasan meminta izin
        var shouldShowRationale = false
        for (permission in permissionsToRequest) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                shouldShowRationale = true
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            if (shouldShowRationale) {
                Toast.makeText(this, "Izin diperlukan agar aplikasi dapat berfungsi.", Toast.LENGTH_SHORT).show()
            }
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            return false
        }

        // Semua izin sudah diberikan
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()

        binding.buttonCamera.setOnClickListener {
            if (checkPermissions()) {
                // Hanya menjalankan CameraActivity jika semua izin sudah diberikan
                val intent = Intent(this, CameraActivity::class.java)
                cameraActivityLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Izin diperlukan agar aplikasi dapat berfungsi.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.buttonGallery.setOnClickListener { startGallery() }

        observeViewModel()
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun observeViewModel() {
        viewModel.imageUri.observe(this, Observer { uri ->
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(binding.objectImage)
                binding.labelHelper.visibility = View.GONE
                binding.txtLoading.visibility = View.VISIBLE
                binding.frame.visibility = View.GONE
                Log.d("MainActivity", "Image URI diterima: $uri")
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading) {
                binding.txtLoading.visibility = View.VISIBLE
                binding.frame.visibility = View.GONE
                Log.d("MainActivity", "Proses klasifikasi dimulai.")
            } else {
                binding.txtLoading.visibility = View.GONE
                Log.d("MainActivity", "Proses klasifikasi selesai.")
            }
        })

        viewModel.classificationResult.observe(this, Observer { result ->
            if (result != null) {
                binding.frame.visibility = View.VISIBLE
                binding.detectedDisease.text = result
                Log.d("MainActivity", "Hasil klasifikasi: $result")
            } else {
                binding.frame.visibility = View.GONE
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                binding.txtLoading.visibility = View.GONE
                binding.frame.visibility = View.GONE
                Log.e("MainActivity", "Error: $error")
            }
        })
    }
}