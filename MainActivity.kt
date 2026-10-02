package com.wolf.iptv

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tela = TextView(this)

        tela.text = "WOLF IPTV"
        tela.textSize = 32f
        tela.setTextColor(Color.WHITE)
        tela.setBackgroundColor(Color.BLACK)
        tela.gravity = Gravity.CENTER

        setContentView(tela)
    }
}
