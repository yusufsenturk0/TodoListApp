package com.example.todo_list.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.todo_list.Data.Task
import java.util.*

object ReminderHelper {

    fun scheduleReminder(context: Context, task: Task) {
        // Eğer hatırlatıcı yoksa çık
        if (task.reminderDate == null || task.reminderTime == null) return

        // Calendar ile epoch millis hesapla
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = task.reminderDate
        val timeParts = task.reminderTime.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val triggerAt = calendar.timeInMillis
        if (triggerAt < System.currentTimeMillis()) return // geçmiş tarih olmasın

        val intent = Intent(context, ReminderReceiver::class.java)
        intent.putExtra("title", task.title)
        intent.putExtra("desc", task.description)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id, // her task için unique
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Android 12+ exact alarm izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // izin yok, kullanıcıyı yönlendir
            Toast.makeText(context, "Exact alarm izni verilmemiş, hatırlatıcı çalışmayabilir", Toast.LENGTH_LONG).show()
            return
        }

        // Alarmı ayarla
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelReminder(context: Context, task: Task) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
