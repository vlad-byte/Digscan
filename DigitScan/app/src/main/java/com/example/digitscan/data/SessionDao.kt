package com.example.digitscan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    // ----- сессии -----
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE sessions SET status = :status, finishedAt = :finishedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SessionStatus, finishedAt: Long?)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query(
        """
        SELECT s.id AS id, s.createdAt AS createdAt, s.status AS status,
               (SELECT COUNT(*) FROM photos p WHERE p.sessionId = s.id) AS photoCount,
               (SELECT COUNT(*) FROM result_lines r WHERE r.sessionId = s.id) AS resultCount
        FROM sessions s
        ORDER BY s.createdAt DESC
        """
    )
    fun observeSummaries(): Flow<List<SessionSummary>>

    // ----- фотографии -----
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY createdAt, id")
    fun observePhotos(sessionId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY createdAt, id")
    suspend fun getPhotos(sessionId: Long): List<PhotoEntity>

    @Query("UPDATE photos SET recognizedText = :text WHERE id = :id")
    suspend fun setPhotoText(id: Long, text: String)

    /** Записывает результат OCR, только если пользователь ещё ничего не вводил. */
    @Query("UPDATE photos SET recognizedText = :text WHERE id = :id AND recognizedText IS NULL")
    suspend fun setPhotoTextIfEmpty(id: Long, text: String)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    // ----- результат -----
    @Insert
    suspend fun insertResultLines(lines: List<ResultLineEntity>)

    @Query("DELETE FROM result_lines WHERE sessionId = :sessionId")
    suspend fun clearResult(sessionId: Long)

    @Query("SELECT * FROM result_lines WHERE sessionId = :sessionId ORDER BY position")
    fun observeResult(sessionId: Long): Flow<List<ResultLineEntity>>
}
