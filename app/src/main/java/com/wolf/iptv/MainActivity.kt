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

        botao.text = "TESTAR PLAYER"
        botao.textSize = 18f

        botao.setOnClickListener {

            val intent = Intent(this, PlayerActivity::class.java)

            intent.putExtra(
                "VIDEO_URL",
                "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
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

        val espaco = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            40
        )

        layout.addView(TextView(this), espaco)

        layout.addView(
            botao,
            LinearLayout.LayoutParams(
                300,
                80
            )
        )

        setContentView(layout)
    }
}
