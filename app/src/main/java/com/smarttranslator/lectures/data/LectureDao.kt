package com.smarttranslator.lectures.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.smarttranslator.lectures.db.entities.Lecture
import com.smarttranslator.lectures.db.entities.TranscriptSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY startedAt DESC")
    fun observeAllLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    suspend fun getLecture(id: Long): Lecture?

    @Insert
    suspend fun insertLecture(lecture: Lecture): Long

    @Update
    suspend fun updateLecture(lecture: Lecture)

    @Insert
    suspend fun insertSegment(segment: TranscriptSegment): Long

    @Delete
    suspend fun deleteLecture(lecture: Lecture)

    @Query("SELECT * FROM segments WHERE lectureId = :lectureId ORDER BY timestamp ASC")
    fun observeSegments(lectureId: Long): Flow<List<TranscriptSegment>>

    @Query("SELECT * FROM segments WHERE lectureId = :lectureId AND timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getSegmentsInRange(lectureId: Long, from: Long, to: Long): List<TranscriptSegment>
}