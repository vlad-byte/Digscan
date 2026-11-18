package com.example.digitscan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.digitscan.data.PhotoEntity
import com.example.digitscan.data.Repository
import com.example.digitscan.domain.TextProcessing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Простая фабрика: создаём ViewModel вручную, передавая репозиторий. */
class VmFactory(private val make: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = make() as T
}

private const val STOP_TIMEOUT_MS = 5_000L

// ---------------- История ----------------
class HistoryViewModel(private val repo: Repository) : ViewModel() {
    /** null — ещё грузится. */
    val sessions = repo.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun create(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.createSession()) }
    }

    fun delete(sessionId: Long) {
        viewModelScope.launch { repo.deleteSession(sessionId) }
    }
}

// ---------------- Этап 1: съёмка ----------------
class CaptureViewModel(
    private val repo: Repository,
    private val sessionId: Long
) : ViewModel() {
    val photos = repo.observePhotos(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun newPhotoFile(): File = repo.newPhotoFile(sessionId)

    fun onPhotoSaved(file: File) {
        viewModelScope.launch { repo.addPhoto(sessionId, file) }
    }

    fun delete(photo: PhotoEntity) {
        viewModelScope.launch { repo.deletePhoto(photo) }
    }
}

// ---------------- Этап 2: проверка текста ----------------
class ReviewViewModel(
    private val repo: Repository,
    private val sessionId: Long
) : ViewModel() {
    val photos = repo.observePhotos(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Завершить можно, когда на всех фото текст распознан и каждая строка корректна (1..20 символов). */
    val canFinish = photos
        .map { list -> list.isNotEmpty() && list.all { TextProcessing.isValid(it.recognizedText) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    // Mutex справедливый (FIFO): правки применяются к БД в том порядке, в котором их сделал пользователь,
    // а завершение сессии ждёт все предыдущие правки.
    private val writeLock = Mutex()
    private var finishing = false

    init {
        viewModelScope.launch { repo.recognizePending(sessionId) }
    }

    fun updateText(photoId: Long, text: String) {
        viewModelScope.launch { writeLock.withLock { repo.updateText(photoId, text) } }
    }

    fun delete(photo: PhotoEntity) {
        viewModelScope.launch { writeLock.withLock { repo.deletePhoto(photo) } }
    }

    fun finish(onDone: () -> Unit) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            writeLock.withLock { repo.finishSession(sessionId) }
            onDone()
        }
    }
}

// ---------------- Просмотр завершённой сессии ----------------
class SessionDetailViewModel(
    private val repo: Repository,
    private val sessionId: Long
) : ViewModel() {
    val session = repo.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)
    val photos = repo.observePhotos(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
    val result = repo.observeResult(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteSession(sessionId)
            onDone()
        }
    }
}
