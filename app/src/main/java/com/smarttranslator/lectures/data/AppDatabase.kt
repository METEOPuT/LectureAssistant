package com.smarttranslator.lectures.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smarttranslator.lectures.db.entities.Lecture
import com.smarttranslator.lectures.db.entities.TranscriptSegment

@Database(entities = [Lecture::class, TranscriptSegment::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "speech_notes.db"
            ).build().also { INSTANCE = it }
        }
    }
}