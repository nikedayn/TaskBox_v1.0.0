package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    val haptics = LocalHapticFeedback.current // Для вібрації
    val heightPerMinute = 4.dp
    val pixelsPerMinute = with(density) { heightPerMinute.toPx() }

    // Визначаємо висоту
    val computedHeight = (task.durationMinutes * heightPerMinute.value).dp.coerceAtLeast(60.dp)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    // Колір смужки (основний колір завдання)
    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Показуємо час, якщо він є АБО якщо ми зараз перетягуємо (щоб бачити новий час)
    val showTimePill = task.startTimeMinutes != null || isDragging

    // Визначаємо, чи ми в темній темі
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // Колір рамки: у темній темі ледь помітний сірий, у світлій - прозорий
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Transparent

    // Container box для обробки перетягування та відображення тіні
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .padding(bottom = 4.dp, end = 16.dp) // Відступ знизу та справа
            .pointerInput(task.startTimeMinutes) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress) // Вібрація при старті
                        isDragging = true
                        val start = task.startTimeMinutes ?: minTime
                        previewTime = minutesToTime(start)
                    },
                    onDragEnd = {
                        isDragging = false
                        val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                        val baseTime = task.startTimeMinutes ?: minTime
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
                        val minutesChange = (offsetY / pixelsPerMinute).roundToInt()
                        val baseTime = task.startTimeMinutes ?: minTime
                        val rawNewTime = (baseTime + minutesChange).coerceIn(0, 1439)
                        previewTime = minutesToTime(rawNewTime)
                    }
                )
            }
            .scale(if (isDragging) 1.05f else 1f) // Ефект збільшення
            .shadow(
                elevation = if (isDragging) 12.dp else 2.dp,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(computedHeight)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                // Тепер Surface береться з Theme.kt (білий для світлої, сірий для темної)
                containerColor = MaterialTheme.colorScheme.surface
            ),
            // --- ДОДАНО РАМКУ ---
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(0.dp) // Тінь малює Box вище
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. КОЛЬОРОВА СМУЖКА ЗЛІВА
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(if (task.isCompleted) Color.LightGray else taskColor)
                )

                // 2. ВМІСТ КАРТКИ
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // ЛІВА ЧАСТИНА: Текст
                        Column(modifier = Modifier.weight(1f)) {
                            // Відступ, щоб текст не перекривав час, якщо він показаний
                            if (showTimePill) Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )

                            // Тривалість показуємо, якщо немає "таблетки" часу або завдання довге
                            if (task.durationMinutes > 45 || !showTimePill) {
                                Text(
                                    text = formatDuration(task.durationMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ПРАВА ЧАСТИНА: Замок та Чекбокс
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (task.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onCheck() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = taskColor,
                                        uncheckedColor = taskColor.copy(alpha = 0.5f),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 3. ЧАС (Time Pill)
                    if (showTimePill) {
                        val timeToShow = if (isDragging) previewTime else minutesToTime(task.startTimeMinutes ?: minTime)

                        // Колір фону часу: якщо тягнемо - колір завдання, інакше - нейтральний
                        val pillColor = if (isDragging) taskColor else MaterialTheme.colorScheme.surfaceVariant
                        val pillContentColor = if (isDragging) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            color = pillColor,
                            contentColor = pillContentColor,
                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = timeToShow,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}