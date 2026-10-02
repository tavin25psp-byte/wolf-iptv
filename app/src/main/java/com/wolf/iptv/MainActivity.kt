package com.wolf.iptv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.BLACK)
        layout.setPadding(40, 40, 40, 40)

        val titulo = TextView(this)

        titulo.text = "WOLF IPTV"
        titulo.textSize = 32f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER

        val botao = Button(this)

        botao.text = "TESTAR FILME"
        botao.textSize = 18f
        botao.isFocusable = true
        botao.isFocusableInTouchMode = true

        botao.setOnClickListener {

            val intent = Intent(this, PlayerActivity::class.java)

            intent.putExtra(
                "VIDEO_URL",
                "https://vz-c091a331-1f1.b-cdn.net/7e4da3e7-be60-47f7-ab66-bbc6c5c273d6/playlist.m3u8"
            )

            startActivity(intent)
        }

        layout.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val espaco = TextView(this)

        layout.addView(
            espaco,
            LinearLayout.LayoutParams(
                1,
                40
            )
        )

        layout.addView(
            botao,
            LinearLayout.LayoutParams(
                300,
                80
            )
        )

        setContentView(layout)

        botao.requestFocus()
    }
}
