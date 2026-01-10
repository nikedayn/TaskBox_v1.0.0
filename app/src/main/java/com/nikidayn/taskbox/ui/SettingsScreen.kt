package com.nikidayn.taskbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.viewmodel.TaskViewModel
import kotlin.math.roundToInt
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val workHours by viewModel.workHours.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Стан для діалогу підтвердження видалення
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Лаунчер для ЕКСПОРТУ (збереження файлу)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.createBackupJson()
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(json.toByteArray())
                }
                Toast.makeText(context, "Бекап збережено!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Лаунчер для ІМПОРТУ (відкриття файлу)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                viewModel.restoreBackup(
                    inputStream = inputStream,
                    onSuccess = { Toast.makeText(context, "Дані відновлено!", Toast.LENGTH_SHORT).show() },
                    onError = { Toast.makeText(context, "Помилка файлу!", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Налаштування",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. ТЕМА
        SettingsSection(title = "Зовнішній вигляд") {
            Text("Тема застосунку", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThemeOption(label = "Системна", selected = themeMode == 0, onClick = { viewModel.updateTheme(0) })
                ThemeOption(label = "Світла", selected = themeMode == 1, onClick = { viewModel.updateTheme(1) })
                ThemeOption(label = "Темна", selected = themeMode == 2, onClick = { viewModel.updateTheme(2) })
            }
        }

        // 2. РОБОЧІ ГОДИНИ
        SettingsSection(title = "Робочий час") {
            Text(
                text = "Інтервал: ${workHours.first.roundToInt()}:00 - ${workHours.second.roundToInt()}:00",
                style = MaterialTheme.typography.titleMedium
            )

            RangeSlider(
                value = workHours.first..workHours.second,
                onValueChange = { range ->
                    viewModel.updateWorkHours(range.start, range.endInclusive)
                },
                valueRange = 0f..24f,
                steps = 23, // Крок у 1 годину
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "Це впливає на відображення сітки таймлайну",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // 3. ДАНІ
        SettingsSection(title = "Керування даними") {
            Button(
                onClick = {
                    // Пропонуємо назву файлу з датою
                    exportLauncher.launch("taskbox_backup_${System.currentTimeMillis()}.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Експорт даних (JSON)")
            }

            Button(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Імпорт даних")
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Видалити всі дані")
            }
        }
    }

    // Діалог підтвердження видалення
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити все?") },
            text = { Text("Ця дія незворотна. Всі завдання, нотатки та шаблони будуть видалені.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(androidx.compose.material.icons.Icons.Default.Check, null) }
        } else null
    )
}