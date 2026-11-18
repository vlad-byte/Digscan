package com.example.digitscan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SessionStatus { ACTIVE, DONE }

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val finishedAt: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val filePath: String,
    val createdAt: Long,
    /** null — текст ещё не распознан; "" — распознать не удалось, нужно ввести вручную. */
    val recognizedText: String? = null
)

/** Одна строка итогового результата сессии (уже после замены «/» и сортировки). */
@Entity(
    tableName = "result_lines",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ResultLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val position: Int,
    val value: String
)

/** Строка списка истории. */
data class SessionSummary(
    val id: Long,
    val createdAt: Long,
    val status: SessionStatus,
    val photoCount: Int,
    val resultCount: Int
)
