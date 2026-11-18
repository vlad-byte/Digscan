package com.example.digitscan.data

import android.content.Context
import androidx.room.withTransaction
import com.example.digitscan.domain.TextProcessing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class Repository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val dao = db.dao()
    private val ocr = OcrEngine()

    fun observeSummaries() = dao.observeSummaries()
    fun observeSession(id: Long) = dao.observeSession(id)
    fun observePhotos(sessionId: Long) = dao.observePhotos(sessionId)
    fun observeResult(sessionId: Long) = dao.observeResult(sessionId)

    // ---------- сессия ----------
    suspend fun createSession(): Long =
        dao.insertSession(SessionEntity(createdAt = System.currentTimeMillis()))

    suspend fun deleteSession(id: Long) {
        dao.deleteSession(id) // фото и результат удалятся каскадом
        withContext(Dispatchers.IO) { File(context.filesDir, "sessions/$id").deleteRecursively() }
    }

    // ---------- этап 1: фото ----------
    fun newPhotoFile(sessionId: Long): File {
        val dir = File(context.filesDir, "sessions/$sessionId").apply { mkdirs() }
        return File(dir, "${System.currentTimeMillis()}.jpg")
    }

    suspend fun addPhoto(sessionId: Long, file: File) {
        dao.insertPhoto(
            PhotoEntity(
                sessionId = sessionId,
                filePath = file.absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePhoto(photo: PhotoEntity) {
        dao.deletePhoto(photo.id)
        withContext(Dispatchers.IO) { File(photo.filePath).delete() }
    }

    // ---------- этап 2: текст ----------
    /** Распознаёт все фото сессии, у которых текст ещё не определён. */
    suspend fun recognizePending(sessionId: Long) {
        for (photo in dao.getPhotos(sessionId)) {
            if (photo.recognizedText != null) continue
            val text = try {
                ocr.recognize(File(photo.filePath))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                "" // не получилось (в т.ч. нехватка памяти) — пользователь введёт вручную
            }
            dao.setPhotoTextIfEmpty(photo.id, text)
        }
    }

    suspend fun updateText(photoId: Long, text: String) = dao.setPhotoText(photoId, text)

    /** Завершение: «/» → «00», сортировка по возрастанию, сохранение результата, статус DONE. */
    suspend fun finishSession(sessionId: Long) {
        val texts = dao.getPhotos(sessionId).map { it.recognizedText.orEmpty() }
        val lines = TextProcessing.buildResult(texts)
        db.withTransaction {
            dao.clearResult(sessionId)
            dao.insertResultLines(
                lines.mapIndexed { index, value ->
                    ResultLineEntity(sessionId = sessionId, position = index, value = value)
                }
            )
            dao.updateStatus(sessionId, SessionStatus.DONE, System.currentTimeMillis())
        }
    }
}
