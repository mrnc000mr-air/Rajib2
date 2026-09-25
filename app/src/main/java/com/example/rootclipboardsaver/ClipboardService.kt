package com.example.rootclipboardsaver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private var lastCopiedText: String = ""

    override fun onCreate() {
        super.onCreate()

        // Android 10-17 এ ব্যাকগ্রাউন্ড ক্লিপবোর্ড এক্সেসের জন্য রুট পারমিশন অ্যাক্টিভেট
        grantBackgroundClipboardPermission()

        startForegroundService()

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboardManager.addPrimaryClipChangedListener {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty() && text != lastCopiedText) {
                    lastCopiedText = text
                    saveToCustomLocation(text)
                }
            }
        }
    }

    private fun grantBackgroundClipboardPermission() {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            val command = "cmd appops set $packageName READ_CLIPBOARD allow\n"
            os.write(command.toByteArray())
            os.write("exit\n".toByteArray())
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToCustomLocation(text: String) {
        try {
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            val customPath = prefs.getString("save_path", "/sdcard/ClipboardLogs") ?: "/sdcard/ClipboardLogs"

            val targetDir = File(customPath)

            if (!targetDir.exists()) {
                targetDir.mkdirs()
                Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p $customPath"))
            }

            // ফাইল নেম: তারিখ_সময়_সেকেন্ড_rajib.txt
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${timeStamp}_rajib.txt"

            val file = File(targetDir, fileName)

            val writer = FileWriter(file)
            writer.write(text)
            writer.flush()
            writer.close()

            Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 666 ${file.absolutePath}"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundService() {
        val channelId = "clipboard_saver_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Clipboard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ক্লিপবোর্ড সেভার চালু আছে")
            .setContentText("কপি করা টেক্সট কাস্টম লোকেশনে সেভ হচ্ছে...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(101, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
