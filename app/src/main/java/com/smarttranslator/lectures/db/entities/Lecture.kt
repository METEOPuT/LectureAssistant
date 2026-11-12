package com.smarttranslator.lectures.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val summary: String = "",
    val isActive: Boolean = false,
    val intervalMinutes: Int = 5
)