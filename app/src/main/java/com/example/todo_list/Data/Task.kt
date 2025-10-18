package com.example.todo_list.Data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity
data class Task (
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "isCompleted")
    var isCompleted: Boolean = false,
    @ColumnInfo(name = "reminderDate")
    val reminderDate: Long? = null,    // Epoch millis, null ise hatırlatıcı yok
    @ColumnInfo(name = "reminderTime")
    val reminderTime: String? = null   // "HH:mm" formatında, null ise hatırlatıcı yok
){
    @PrimaryKey(autoGenerate = true)
    var id = 0

    fun getReminderEpochMillis(): Long? {
        if(reminderDate == null || reminderTime.isNullOrEmpty()) return null
        val parts = reminderTime.split(":")
        if(parts.size != 2) return null

        val cal = Calendar.getInstance().apply {
            timeInMillis = reminderDate
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}