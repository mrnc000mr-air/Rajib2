package com.example.rootclipboardsaver

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.DataOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var pathEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val savedPath = prefs.getString("save_path", "/sdcard/ClipboardLogs") ?: "/sdcard/ClipboardLogs"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val labelText = TextView(this).apply {
            text = "ফাইল সেভ করার লোকেশন (Path) দিন:"
            textSize = 14f
        }

        pathEditText = EditText(this).apply {
            setText(savedPath)
            hint = "/sdcard/YourFolderName"
        }

        val savePathBtn = Button(this).apply {
            text = "Save Path Location"
            setOnClickListener {
                val newPath = pathEditText.text.toString().trim()
                if (newPath.isNotEmpty()) {
                    prefs.edit().putString("save_path", newPath).apply()
                    Toast.makeText(this@MainActivity, "লোকেশন সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "সঠিক পাথ লিখুন!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val statusText = TextView(this).apply {
            text = "\nরুট সার্ভিস চালু করতে নিচের বাটনে চাপ দিন"
            textSize = 15f
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
        }

        val startBtn = Button(this).apply {
            text = "Start Root Clipboard Service"
            setOnClickListener {
                if (checkAndRequestRoot()) {
                    val intent = Intent(this@MainActivity, ClipboardService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    statusText.text = "রুট সার্ভিস সক্রিয় রয়েছে।"
                    Toast.makeText(this@MainActivity, "Root Service Started!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Root Permission পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(labelText)
        layout.addView(pathEditText)
        layout.addView(savePathBtn)
        layout.addView(statusText)
        layout.addView(startBtn)

        setContentView(layout)
    }

    private fun checkAndRequestRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
