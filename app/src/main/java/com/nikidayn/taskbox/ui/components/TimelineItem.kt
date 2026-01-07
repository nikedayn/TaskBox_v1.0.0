package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

@Composable
fun TimelineItem(
    task: Task,
    minTime: Int = 0,
    isLast: Boolean = false,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onTimeChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val heightPerMinute = 4.dp
    val pixelsPerMinute = with(density) { heightPerMinute.toPx() }

    // Визначаємо висоту
    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    // Колір (без логіки виділення)
    val containerColor = if (task.isCompleted) Color(0xFFF0F0F0) else Color(android.graphics.Color.parseColor(task.colorHex))
    val contentColor = if (task.isCompleted) Color.Gray else getContrastColor(task.colorHex)

    // Показуємо час, якщо він є АБО якщо ми зараз перетягуємо (щоб бачити новий час)
    val showTimePill = task.startTimeMinutes != null || isDragging

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                .padding(bottom = 2.dp, end = 16.dp)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clip(RoundedCornerShape(16.dp))
                // --- ЗМІНА 1: Використовуємо просте натискання для редагування ---
                .clickable(onClick = onClick)
                // --- ЗМІНА 2: Довге натискання активує перетягування на всій картці ---
                .pointerInput(task.startTimeMinutes) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            // При старті беремо поточний час або minTime (якщо з Вхідних)
                            val start = task.startTimeMinutes ?: minTime
                            previewTime = minutesToTime(start)
                        },
                        onDragEnd = {
                            isDragging = false
                            val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                            val baseTime = task.startTimeMinutes ?: minTime
                            // Обмежуємо час в межах доби
                            val newStart = (baseTime + minutesChange).coerceIn(0, 1439)
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
                            // Рахуємо попередній перегляд часу
                            val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                            val baseTime = task.startTimeMinutes ?: minTime
                            val rawNewTime = (baseTime + minutesChange).coerceIn(0, 1439)
                            previewTime = minutesToTime(rawNewTime)
                        }
                    )
                }
                // Додаємо візуальний ефект збільшення при перетягуванні
                .scale(if (isDragging) 1.02f else 1f),

            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            // При перетягуванні додаємо тінь (elevation)
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // ТЕКСТ ЗАВДАННЯ
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)) {

                        // Відступ для "таблетки" часу, щоб текст не наліз
                        if (showTimePill) Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor
                        )
                        if (task.durationMinutes > 45 || !showTimePill) {
                            Text(
                                text = formatDuration(task.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // ПРАВА ЧАСТИНА (Замок + Чекбокс)
                    // --- ЗМІНА 3: Видалено DragHandle та CheckCircle ---
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

                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onCheck() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = contentColor, // Адаптуємо чекбокс під колір тексту
                                    checkmarkColor = containerColor, // Галочка кольору фону картки
                                    uncheckedColor = contentColor.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                // ВІДОБРАЖЕННЯ ЧАСУ (Time Pill)
                if (showTimePill) {
                    val timeToShow = if (isDragging) previewTime else minutesToTime(task.startTimeMinutes ?: minTime)
                    Surface(
                        color = if(isDragging) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, // Змінюємо колір при перетягуванні
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