package com.example.btlkotlin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.btlkotlin.ui.face_recognize.ObjectRecognitionActivity
import com.example.btlkotlin.ui.image_recognize.ImageRecognitionActivity

class MainActivity : AppCompatActivity() {
    private lateinit var cardFaceRecognition: CardView
    private lateinit var cardImageRecognition: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "Chức năng chính"  // Set empty title

        // Initialize views
        cardFaceRecognition = findViewById(R.id.cardObjectRecognition)
        cardImageRecognition = findViewById(R.id.cardImageRecognition)

        // Set click listeners
        cardFaceRecognition.setOnClickListener {
            val intent = Intent(this, ObjectRecognitionActivity::class.java)
            startActivity(intent)
        }

        cardImageRecognition.setOnClickListener {
            val intent = Intent(this, ImageRecognitionActivity::class.java)
            startActivity(intent)
        }
    }
}