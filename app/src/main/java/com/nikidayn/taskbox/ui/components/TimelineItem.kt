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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.minutesToTime
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    task: Task,
    isLast: Boolean = false,
    isSelected: Boolean = false,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimeChange: (Int) -> Unit // Новий параметр: коли відпустили картку
) {
    val density = LocalDensity.current
    // Масштаб: 1 хвилина = 2dp. Переводимо в пікселі для розрахунків
    val pixelsPerMinute = with(density) { 2.dp.toPx() }

    val heightPerMinute = 2.dp
    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    // Зміщення картки по вертикалі (поки тягнемо)
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Кольори
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else if (task.isCompleted) Color(0xFFF0F0F0)
    else Color(android.graphics.Color.parseColor(task.colorHex))

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // --- ЛІВА КОЛОНКА (ЧАС) ---
        // Просто пустий простір, бо час тепер намальований на сітці
        // Ми не малюємо тут текст, бо він вже є на шкалі DayView
        Spacer(modifier = Modifier.width(0.dp))

        // --- ПРАВА ЧАСТИНА (КАРТКА) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                .padding(bottom = 16.dp, end = 16.dp)
                // Зміщуємо картку візуально
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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp), // Трохи менший відступ, щоб влізла ручка
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Текст
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${task.durationMinutes} хв",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Праві елементи: Чекбокс або Drag Handle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Checkbox(checked = task.isCompleted, onCheckedChange = { onCheck() })

                        Spacer(modifier = Modifier.width(4.dp))

                        // --- DRAG HANDLE (РУЧКА) ---
                        // Тільки за неї можна тягати картку
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Move",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            // 1. Рахуємо зміну часу
                                            val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                                            val currentStart = task.startTimeMinutes ?: 0
                                            val newStart = currentStart + minutesChange

                                            // 2. Повертаємо картку на місце (візуально), бо список перемалюється
                                            offsetY = 0f

                                            // 3. Зберігаємо в базу
                                            onTimeChange(newStart)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY += dragAmount.y
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}