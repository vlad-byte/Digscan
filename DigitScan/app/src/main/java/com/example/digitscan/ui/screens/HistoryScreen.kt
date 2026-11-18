package com.example.digitscan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitscan.data.Repository
import com.example.digitscan.data.SessionStatus
import com.example.digitscan.data.SessionSummary
import com.example.digitscan.ui.HistoryViewModel
import com.example.digitscan.ui.VmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repo: Repository,
    onOpen: (SessionSummary) -> Unit,
    onCreated: (Long) -> Unit
) {
    val vm = viewModel<HistoryViewModel>(factory = VmFactory { HistoryViewModel(repo) })
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<SessionSummary?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("История сессий") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.create(onCreated) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Новая сессия") }
            )
        }
    ) { padding ->
        val list = sessions
        when {
            list == null -> Unit
            list.isEmpty() -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Сессий пока нет.\nНажмите «Новая сессия», чтобы начать.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { s ->
                    SessionCard(
                        s = s,
                        onClick = { onOpen(s) },
                        onDelete = { toDelete = s }
                    )
                }
            }
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить сессию №${s.id}?") },
            text = { Text("Фотографии и результат будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(s.id); toDelete = null }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SessionCard(s: SessionSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Сессия №${s.id}", style = MaterialTheme.typography.titleMedium)
                Text(formatDate(s.createdAt), style = MaterialTheme.typography.bodyMedium)
                val status = if (s.status == SessionStatus.DONE) {
                    "Завершена · фото: ${s.photoCount} · строк: ${s.resultCount}"
                } else {
                    "Не завершена · фото: ${s.photoCount} — нажмите, чтобы продолжить"
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить сессию")
            }
        }
    }
}
