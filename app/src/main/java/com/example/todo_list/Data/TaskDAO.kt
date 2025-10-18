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
    fun finById(id : Int) : Flowable<Task>

    @Insert
    fun insert( task : Task) : Completable

    @Delete
    fun delete(task : Task) : Completable

    @Update
    fun update(task: Task) : Completable

}




