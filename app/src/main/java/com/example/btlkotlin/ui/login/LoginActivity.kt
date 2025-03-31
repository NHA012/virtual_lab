package com.example.btlkotlin.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.btlkotlin.MainActivity
import com.example.btlkotlin.R

// LoginActivity.kt
class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        etEmail = findViewById(R.id.emailEditText)
        etPassword = findViewById(R.id.passwordEditText)
        btnLogin = findViewById(R.id.loginButton)

        // Set click listener for login button
        btnLogin.setOnClickListener {
            // Here you would typically validate inputs
            // For now, just navigate to main page
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}