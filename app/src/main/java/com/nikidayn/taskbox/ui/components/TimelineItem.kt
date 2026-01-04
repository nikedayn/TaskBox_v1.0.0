package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.minutesToTime
import kotlin.math.roundToInt

import com.nikidayn.taskbox.utils.formatDuration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    task: Task,
    isLast: Boolean = false,
    isSelected: Boolean = false,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimeChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val pixelsPerMinute = with(density) { 2.dp.toPx() }

    val heightPerMinute = 2.dp
    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else if (task.isCompleted) Color(0xFFF0F0F0)
    else Color(android.graphics.Color.parseColor(task.colorHex))

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    // Визначаємо, чи показувати плашку часу (якщо є час або тягнемо)
    val showTimePill = task.startTimeMinutes != null || isDragging

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Spacer(modifier = Modifier.width(0.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                // ВИПРАВЛЕННЯ ТУТ:
                // Було: .padding(bottom = 16.dp, end = 16.dp)
                // Стало: bottom = 2.dp (мікро-відступ), end = 16.dp (відступ справа)
                .padding(bottom = 2.dp, end = 16.dp)

                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        // ЗМЕНШЕНО ВІДСТУПИ:
                        // vertical = 4.dp (було 8), щоб дати більше місця тексту по висоті
                        // horizontal = 8.dp (збоку)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // ТЕКСТ
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        // Якщо є плашка часу:
                        if (showTimePill) {
                            // Якщо завдання дуже коротке (<= 30 хв), Spacer завеликий.
                            // Робимо його меншим (20.dp), щоб текст вліз.
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // ЗМІНА ТУТ: Використовуємо formatDuration замість просто тексту "хв"
                        if (task.durationMinutes > 45 || !showTimePill) {
                            Text(
                                text = formatDuration(task.durationMinutes), // <--- Оновлено
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }

                    // ПРАВА ЧАСТИНА (Чекбокс і Ручка)
                    // Центруємо їх по вертикалі
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Checkbox(checked = task.isCompleted, onCheckedChange = { onCheck() })

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Move",
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(task.startTimeMinutes) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    isDragging = true
                                                    previewTime = minutesToTime(task.startTimeMinutes ?: 0)
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                                                    val currentStart = task.startTimeMinutes ?: 0
                                                    val newStart = currentStart + minutesChange
                                                    offsetY = 0f
                                                    onTimeChange(newStart)
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    offsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    offsetY += dragAmount.y
                                                    val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                                                    val currentStart = task.startTimeMinutes ?: 0
                                                    val rawNewTime = (currentStart + minutesChange).coerceIn(0, 1439)
                                                    previewTime = minutesToTime(rawNewTime)
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                // ПЛАШКА З ЧАСОМ
                if (showTimePill) {
                    val timeToShow = if (isDragging) previewTime else minutesToTime(task.startTimeMinutes ?: 0)

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        // Трохи менший шрифт і відступи, щоб було компактніше
                        shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = timeToShow,
                            style = MaterialTheme.typography.labelMedium, // labelMedium менший за labelLarge
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}