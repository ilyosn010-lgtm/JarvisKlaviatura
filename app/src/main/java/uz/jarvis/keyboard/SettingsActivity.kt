package uz.jarvis.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val p = (20 * d).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(p, p * 2, p, p)
        root.setBackgroundColor(Color.rgb(10, 14, 28))

        fun label(text: String, size: Float, color: Int, bold: Boolean): TextView {
            val t = TextView(this)
            t.text = text
            t.textSize = size
            t.setTextColor(color)
            if (bold) t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
            t.setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
            return t
        }

        root.addView(label("JARVIS Klaviatura", 26f, Color.rgb(0, 229, 255), true))
        root.addView(label("1. «Klaviaturani yoqish» ni bosing va JARVIS Klaviatura ni yoqing.", 15f, Color.WHITE, false))
        root.addView(label("2. «Klaviaturani tanlash» ni bosing va uni tanlang.", 15f, Color.WHITE, false))

        val b1 = Button(this)
        b1.text = "Klaviaturani yoqish"
        b1.setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        root.addView(b1)

        val b2 = Button(this)
        b2.text = "Klaviaturani tanlash"
        b2.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        root.addView(b2)

        val et = EditText(this)
        et.hint = "Shu yerda sinab ko'ring"
        et.setTextColor(Color.WHITE)
        et.setHintTextColor(Color.GRAY)
        et.gravity = Gravity.TOP
        et.minLines = 4
        root.addView(et)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }
}
