package com.example.booki

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Pinta o "!" de laranja
        val tvAppTitle = findViewById<TextView>(R.id.tvAppTitle)
        val text = "Booki!"
        val spannable = SpannableStringBuilder(text)
        val orangeColor = ForegroundColorSpan(Color.parseColor("#F2A33A"))

        // Aplica a cor laranja no último caractere (o "!")
        spannable.setSpan(orangeColor, text.length - 1, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvAppTitle.text = spannable

        // 2. Lógica do botão de entrar
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}