package com.example.todo_list.Data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

@Dao
interface TaskDAO {

    @Query("SELECT * FROM Task")
    fun getAll(): Flowable<List<Task>>

    @Query("SELECT * FROM Task WHERE id = :id")
    fun findById(id: Int): Flowable<Task>

    @Insert
    fun insert(task: Task): Single<Long>

    @Delete
    fun delete(task: Task): Completable

    @Update
    fun update(task: Task): Completable

    // BootReceiver / Worker için tek seferlik liste (suspend)
    @Query("SELECT * FROM Task")
    suspend fun getAllOnce(): List<Task>
}