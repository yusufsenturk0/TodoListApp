package com.example.todo_list.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object AlarmHelper {

    fun setReminder(context: Context, taskId: Int, reminderTime: Long, title: String, description: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("description", description)
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            when {
                // Android 12+ (API 31)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        // En güçlü yöntem - Doze mode'u atlar
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(reminderTime, pendingIntent),
                            pendingIntent
                        )
                        Log.d("AlarmHelper", "AlarmClock kuruldu (Android 12+): $reminderTime")
                    } else {
                        // İzin yoksa fallback
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                        Log.w("AlarmHelper", "Exact alarm izni yok, fallback kullanıldı")
                    }
                }

                // Android 6-11 (API 23-30)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // setAlarmClock en güvenilir yöntem - Doze mode'u bypass eder
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(reminderTime, pendingIntent),
                        pendingIntent
                    )
                    Log.d("AlarmHelper", "AlarmClock kuruldu (Android 6-11): $reminderTime")
                }

                // Android 5 ve altı
                else -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                    Log.d("AlarmHelper", "Exact alarm kuruldu (Android 5-): $reminderTime")
                }
            }

            // Alarmın kurulduğunu SharedPreferences'e kaydet
            saveAlarmToPrefs(context, taskId, reminderTime, title, description)

        } catch (e: SecurityException) {
            Log.e("AlarmHelper", "Alarm kurma izni yok", e)
        } catch (e: Exception) {
            Log.e("AlarmHelper", "Alarm kurma hatası", e)
        }
    }

    fun cancelReminder(context: Context, taskId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmHelper", "Alarm iptal edildi: $taskId")
        }

        // SharedPreferences'ten de sil
        removeAlarmFromPrefs(context, taskId)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // Alarmları SharedPreferences'e kaydet (BootReceiver için yedek)
    private fun saveAlarmToPrefs(context: Context, taskId: Int, time: Long, title: String, description: String) {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("alarm_time_$taskId", time)
            putString("alarm_title_$taskId", title)
            putString("alarm_desc_$taskId", description)
            apply()
        }
        Log.d("AlarmHelper", "Alarm prefs'e kaydedildi: $taskId")
    }

    private fun removeAlarmFromPrefs(context: Context, taskId: Int) {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("alarm_time_$taskId")
            remove("alarm_title_$taskId")
            remove("alarm_desc_$taskId")
            apply()
        }
    }

    // BootReceiver'dan çağrılacak - tüm alarmları restore et
    fun restoreAllAlarms(context: Context) {
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val allPrefs = prefs.all
        val now = System.currentTimeMillis()

        for ((key, value) in allPrefs) {
            if (key.startsWith("alarm_time_")) {
                val taskId = key.removePrefix("alarm_time_").toIntOrNull() ?: continue
                val time = value as? Long ?: continue

                if (time > now) {
                    val title = prefs.getString("alarm_title_$taskId", "Hatırlatma") ?: "Hatırlatma"
                    val desc = prefs.getString("alarm_desc_$taskId", "") ?: ""

                    setReminder(context, taskId, time, title, desc)
                    Log.d("AlarmHelper", "Alarm restore edildi: $taskId")
                }
            }
        }
    }
}