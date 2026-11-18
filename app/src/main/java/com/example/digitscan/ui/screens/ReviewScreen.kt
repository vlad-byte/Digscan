package com.example.digitscan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.digitscan.data.PhotoEntity
import com.example.digitscan.data.Repository
import com.example.digitscan.domain.TextProcessing
import com.example.digitscan.ui.ReviewViewModel
import com.example.digitscan.ui.VmFactory
import java.io.File

/** Этап 2: проверка текста, найденного на каждой фотографии, и завершение сессии. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    repo: Repository,
    sessionId: Long,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val vm = viewModel<ReviewViewModel>(factory = VmFactory { ReviewViewModel(repo, sessionId) })
    val photos by vm.photos.collectAsStateWithLifecycle()
    val canFinish by vm.canFinish.collectAsStateWithLifecycle()
    var zoomed by remember { mutableStateOf<PhotoEntity?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Шаг 2 из 2: проверка текста") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к съёмке")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { vm.finish(onFinished) },
                        enabled = canFinish,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Завершить сессию") }
                    if (!canFinish) {
                        Text(
                            if (photos.isEmpty()) "В сессии нет фотографий."
                            else "Дождитесь распознавания и заполните все поля (до ${TextProcessing.MAX_LEN} символов).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Сверьте текст с фотографией и исправьте ошибки распознавания. " +
                        "При завершении «/» заменится на «00», строки отсортируются по возрастанию.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(photos, key = { it.id }) { photo ->
                ReviewItem(
                    photo = photo,
                    onZoom = { zoomed = photo },
                    onTextChange = { vm.updateText(photo.id, it) },
                    onDelete = { vm.delete(photo) }
                )
            }
        }
    }

    zoomed?.let { PhotoDialog(file = File(it.filePath), onDismiss = { zoomed = null }) }
}

@Composable
private fun ReviewItem(
    photo: PhotoEntity,
    onZoom: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val recognized = photo.recognizedText
    // Ключ меняется один раз — когда приходит результат OCR; дальше поле хранит собственное состояние,
    // чтобы ввод не «прыгал» при записи в БД.
    var text by remember(photo.id, recognized == null) { mutableStateOf(recognized.orEmpty()) }

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = File(photo.filePath),
                contentDescription = "Фото, нажмите для увеличения",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onZoom)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (recognized == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Распознаю текст…")
                    }
                } else {
                    val tooLong = text.length > TextProcessing.MAX_LEN
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = TextProcessing.normalize(it)
                            onTextChange(text)
                        },
                        singleLine = true,
                        isError = text.isEmpty() || tooLong,
                        label = { Text("Текст на фото") },
                        supportingText = {
                            Text(
                                when {
                                    text.isEmpty() -> "Введите текст"
                                    tooLong -> "Слишком длинно: ${text.length}/${TextProcessing.MAX_LEN}"
                                    else -> "${text.length}/${TextProcessing.MAX_LEN}"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Убрать фото из сессии")
            }
        }
    }
}
