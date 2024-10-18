package com.example.potatoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.potatoapp.databinding.ActivityReviewPhotoBinding

class ReviewPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewPhotoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            Glide.with(this)
                .load(imageUri)
                .into(binding.imageReview)

            binding.buttonUlangi.setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }

            binding.buttonLanjut.setOnClickListener {
                val returnIntent = Intent().apply {
                    putExtra("imageUri", imageUriString)
                }
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}