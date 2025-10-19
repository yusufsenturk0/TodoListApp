package com.example.todo_list.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Ekranı uyandır (Doze mode için önemli)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "TodoApp::ReminderWakeLock"
        )
        wakeLock.acquire(3000) // 3 saniye

        try {
            val title = intent.getStringExtra("title") ?: "Hatırlatma"
            val description = intent.getStringExtra("description") ?: ""
            val taskId = intent.getIntExtra("taskId", 0)

            Log.d("ReminderReceiver", "Bildirim gösteriliyor: $title")

            // Bildirimi göster
            NotificationHelper.showNotification(context, taskId, title, description)

            // Alarmı SharedPreferences'ten sil (artık geçti)
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("alarm_time_$taskId")
                remove("alarm_title_$taskId")
                remove("alarm_desc_$taskId")
                apply()
            }

        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Bildirim gösterme hatası", e)
        } finally {
            wakeLock.release()
        }
    }
}