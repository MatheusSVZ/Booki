package com.example.booki

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Encontra o texto do cabeçalho e pinta o "!" de laranja
        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val text = "Booki!"
        val spannable = SpannableStringBuilder(text)
        val orangeColor = ForegroundColorSpan(Color.parseColor("#F2A33A"))

        spannable.setSpan(orangeColor, text.length - 1, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvHeaderTitle.text = spannable

        // 2. Mantém o funcionamento do botão flutuante (+)
        val fabAddBook = findViewById<FloatingActionButton>(R.id.fabAddBook)
        fabAddBook.setOnClickListener {
            Toast.makeText(this, "Abrir tela de cadastro de livro", Toast.LENGTH_SHORT).show()
        }
    }
}