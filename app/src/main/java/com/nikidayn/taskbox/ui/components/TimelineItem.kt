package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
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
import com.nikidayn.taskbox.ui.theme.getContrastColor
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.formatDuration
import com.nikidayn.taskbox.utils.minutesToTime
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    task: Task,
    minTime: Int = 0,
    isLast: Boolean = false,
    isSelected: Boolean = false,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimeChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val heightPerMinute = 4.dp
    val pixelsPerMinute = with(density) { heightPerMinute.toPx() }

    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    // 1. ВИЗНАЧАЄМО КОЛЬОРИ
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else if (task.isCompleted) Color(0xFFF0F0F0) // Сірий для виконаних
    else Color(android.graphics.Color.parseColor(task.colorHex))

    // 2. ВИЗНАЧАЄМО КОНТРАСТНИЙ КОЛІР (Чорний або Білий)
    // Якщо завдання виконане або виділене - беремо системний колір, інакше - рахуємо контраст
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else if (task.isCompleted) Color.Gray
    else getContrastColor(task.colorHex)

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    val showTimePill = task.startTimeMinutes != null || isDragging

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        Spacer(modifier = Modifier.width(0.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                .padding(bottom = 2.dp, end = 16.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor // <--- ЗАСТОСОВУЄМО КОЛІР ДО ВСЬОГО ВМІСТУ
            ),
            border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // ТЕКСТ
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        if (showTimePill) Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor // Явно задаємо колір
                        )
                        if (task.durationMinutes > 45 || !showTimePill) {
                            Text(
                                text = formatDuration(task.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = contentColor.copy(alpha = 0.7f) // Трохи прозоріший
                            )
                        }
                    }

                    // ПРАВА ЧАСТИНА (Чекбокс + Ручка)
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (task.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                // --- ВИПРАВЛЕНИЙ ЧЕКБОКС ---
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onCheck() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                        uncheckedColor = contentColor.copy(alpha = 0.6f) // <--- РАМКА СТАЄ ЧОРНОЮ НА ЖОВТОМУ
                                    )
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                // --- ВИПРАВЛЕНА РУЧКА (Drag Handle) ---
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Move",
                                    tint = contentColor.copy(alpha = 0.5f), // <--- РУЧКА ТЕЖ АДАПТУЄТЬСЯ
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(task.startTimeMinutes) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    isDragging = true
                                                    val start = task.startTimeMinutes ?: minTime
                                                    previewTime = minutesToTime(start)
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                                                    val baseTime = task.startTimeMinutes ?: minTime
                                                    val newStart = (baseTime + minutesChange).coerceIn(minTime, 1439)
                                                    offsetY = 0f
                                                    onTimeChange(newStart)
                                                },
                                                onDragCancel = { isDragging = false; offsetY = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    offsetY += dragAmount.y
                                                    val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                                                    val baseTime = task.startTimeMinutes ?: minTime
                                                    val rawNewTime = (baseTime + minutesChange).coerceIn(minTime, 1439)
                                                    previewTime = minutesToTime(rawNewTime)
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                // ... TimePill (код без змін) ...
                if (showTimePill) {
                    val timeToShow = if (isDragging) previewTime else minutesToTime(task.startTimeMinutes ?: minTime)
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = timeToShow,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}