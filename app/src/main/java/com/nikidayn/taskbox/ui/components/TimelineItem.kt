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
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.formatDuration
import com.nikidayn.taskbox.utils.minutesToTime
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    task: Task,
    minTime: Int = 0, // 1. НОВИЙ ПАРАМЕТР: Мінімальний час (у хвилинах)
    isLast: Boolean = false,
    isSelected: Boolean = false,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimeChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val heightPerMinute = 4.dp // Ваш масштаб
    val pixelsPerMinute = with(density) { heightPerMinute.toPx() }

    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else if (task.isCompleted) Color(0xFFF0F0F0)
    else Color(android.graphics.Color.parseColor(task.colorHex))

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    val showTimePill = task.startTimeMinutes != null || isDragging

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Spacer(modifier = Modifier.width(0.dp))

        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else if (task.isCompleted) Color.Gray
        else getContentColorForHex(task.colorHex)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                .padding(bottom = 2.dp, end = 16.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = textColor // <--- ДОДАЙТЕ ЦЕЙ РЯДОК
            ),
            border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
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
                        if (showTimePill) {
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.durationMinutes > 45 || !showTimePill) {
                            Text(
                                text = formatDuration(task.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }

                    // ПРАВА ЧАСТИНА
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (task.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error, // Червоний колір для помітності
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
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
                                                    // Якщо час null (Вхідні), показуємо minTime як старт
                                                    val start = task.startTimeMinutes ?: minTime
                                                    previewTime = minutesToTime(start)
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    val minutesChange =
                                                        (offsetY / pixelsPerMinute).roundToInt()

                                                    // 2. БАЗОВИЙ ЧАС: Якщо null, беремо minTime
                                                    val baseTime = task.startTimeMinutes ?: minTime

                                                    // 3. ОБМЕЖЕННЯ: Не менше minTime
                                                    val newStart =
                                                        (baseTime + minutesChange).coerceIn(
                                                            minTime,
                                                            1439
                                                        )

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

                                                    val minutesChange =
                                                        (offsetY / pixelsPerMinute).roundToInt()
                                                    val baseTime = task.startTimeMinutes ?: minTime

                                                    // Обмежуємо прев'ю теж
                                                    val rawNewTime =
                                                        (baseTime + minutesChange).coerceIn(
                                                            minTime,
                                                            1439
                                                        )
                                                    previewTime = minutesToTime(rawNewTime)
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                if (showTimePill) {
                    // Якщо тягнемо - показуємо прев'ю, якщо ні - реальний час (або minTime для краси, якщо раптом null проскочить)
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

// Проста функція: якщо фон світлий -> текст чорний, інакше -> білий
fun getContentColorForHex(hex: String): Color {
    val color = try {
        android.graphics.Color.parseColor(hex)
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }
    val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255
    return if (darkness < 0.5) Color.Black else Color.White
}