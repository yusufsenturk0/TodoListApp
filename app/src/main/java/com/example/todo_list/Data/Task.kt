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


}