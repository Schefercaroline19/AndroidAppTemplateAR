package com.ifpr.androidapptemplatear.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ifpr.androidapptemplatear.MainActivity

class GameOverActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOTIVO = "motivo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val motivo = intent.getStringExtra(EXTRA_MOTIVO) ?: "A partida terminou."

        val painel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(18, 24, 38))
        }

        painel.addView(TextView(this).apply {
            text = "GAME OVER"
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        })

        painel.addView(TextView(this).apply {
            text = motivo
            textSize = 20f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        })

        painel.addView(Button(this).apply {
            text = "Jogar novamente"
            setOnClickListener {
                val reinicio = Intent(this@GameOverActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(reinicio)
                finish()
            }
        })

        setContentView(painel)
    }
}
