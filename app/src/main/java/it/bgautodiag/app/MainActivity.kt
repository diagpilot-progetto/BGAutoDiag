package it.bgautodiag.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "BG AutoDiag"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Diagnosi automotive tramite OBD-II"
            textSize = 16f
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Cerca adattatori BLE"
            isEnabled = false
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(button)

        setContentView(layout)
    }
}
