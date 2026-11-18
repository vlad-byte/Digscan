package com.example.digitscan.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.digitscan.data.PhotoEntity
import com.example.digitscan.data.Repository
import com.example.digitscan.ui.SessionDetailViewModel
import com.example.digitscan.ui.VmFactory
import java.io.File

/** Завершённая сессия: итоговый список и сделанные фотографии. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    repo: Repository,
    sessionId: Long,
    onBack: () -> Unit
) {
    val vm = viewModel<SessionDetailViewModel>(factory = VmFactory { SessionDetailViewModel(repo, sessionId) })
    val session by vm.session.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var confirmDelete by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf<PhotoEntity?>(null) }
    val resultText = result.joinToString("\n") { it.value }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сессия №$sessionId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(resultText)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Скопировать результат")
                    }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, resultText)
                        }
                        context.startActivity(Intent.createChooser(send, "Поделиться результатом"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Поделиться результатом")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить сессию")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                session?.let {
                    Text(
                        "Создана ${formatDate(it.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Text(
                    "Результат (${result.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(result, key = { "line-${it.id}" }) { line ->
                Row {
                    Text(
                        "${line.position + 1}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(line.value, fontFamily = FontFamily.Monospace)
                }
            }
            item {
                Text(
                    "Фотографии (${photos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(photos.chunked(3), key = { "photos-${it.first().id}" }) { rowPhotos ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowPhotos.forEach { photo ->
                        Column(Modifier.weight(1f)) {
                            AsyncImage(
                                model = File(photo.filePath),
                                contentDescription = "Фото, нажмите для увеличения",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { zoomed = photo }
                            )
                            Text(
                                photo.recognizedText.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(3 - rowPhotos.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    zoomed?.let { PhotoDialog(file = File(it.filePath), onDismiss = { zoomed = null }) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить сессию №$sessionId?") },
            text = { Text("Фотографии и результат будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onBack)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
    }
}
