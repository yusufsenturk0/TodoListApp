package com.example.todo_list.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object AlarmPermissionHelper {

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun requestExactAlarmPermission(context: Context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toast.makeText(context, "Exact Alarm izni verilmemiş, hatırlatıcı tam zamanında çalışmayabilir", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    }
}
