package com.example.todo_list.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todo_list.helper.AlarmHelper
import com.example.todo_list.Data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d("BootReceiver", "Cihaz başlatıldı - alarmlar restore ediliyor")

            context?.let { ctx ->
                // Önce SharedPreferences'ten restore et (hızlı)
                AlarmHelper.restoreAllAlarms(ctx)

                // Sonra veritabanından da kontrol et (yedek)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val tasks = TaskDatabase.getInstance(ctx).taskDao().getAllOnce()
                        val now = System.currentTimeMillis()

                        for (task in tasks) {
                            if (task.reminderDate != null && task.reminderTime != null) {
                                val reminderMillis = combineDateAndTime(task.reminderDate, task.reminderTime)

                                if (reminderMillis > now) {
                                    AlarmHelper.setReminder(
                                        ctx,
                                        task.id,
                                        reminderMillis,
                                        task.title,
                                        task.description
                                    )
                                    Log.d("BootReceiver", "Alarm veritabanından restore edildi: ${task.id}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Veritabanından alarm restore hatası", e)
                    }
                }
            }
        }
    }

    private fun combineDateAndTime(dateMillis: Long, timeString: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }
        val parts = timeString.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        calendar.set(Calendar.MINUTE, parts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}