package com.smarttranslator.lectures.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "segments",
    foreignKeys = [ForeignKey(
        entity = Lecture::class,
        parentColumns = ["id"],
        childColumns = ["lectureId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lectureId"), Index("timestamp")]
)
data class TranscriptSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: Long,
    val text: String,
    val timestamp: Long,
    val isFinal: Boolean = true
)