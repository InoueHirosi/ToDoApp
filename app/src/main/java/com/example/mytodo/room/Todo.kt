package com.example.mytodo.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mytodo.model.Priority

@Entity
data class Todo(
    @PrimaryKey(autoGenerate = true) @ColumnInfo var id: Int,
    @ColumnInfo var displayOrder: Int,
    @ColumnInfo val title: String,
    @ColumnInfo val detail: String,
    @ColumnInfo val deadline: String,
    @ColumnInfo val priority: Priority,
)