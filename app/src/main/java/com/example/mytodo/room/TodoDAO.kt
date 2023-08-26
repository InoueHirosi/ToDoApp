package com.example.mytodo.room

import androidx.room.*

@Dao
interface TodoDAO {

    @Query("select * from todo order by displayOrder")
    fun getAll(): List<Todo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(todo: Todo): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(todo: Todo)

    @Transaction
    fun upsert(todo: Todo) {
        val id = insert(todo)
        if (id == -1L) {
            update(todo)
        }
    }

    @Query("delete from todo where id = :id")
    fun delete(id: Int)
}