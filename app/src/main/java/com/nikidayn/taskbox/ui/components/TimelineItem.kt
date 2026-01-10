package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
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
    val haptics = LocalHapticFeedback.current
    val heightPerMinute = 4.dp
    val pixelsPerMinute = with(density) { heightPerMinute.toPx() }

    // Якщо < 15 хв — компактний вигляд
    val isCompact = task.durationMinutes < 15

    // Висота картки
    val physicalHeight = (task.durationMinutes * heightPerMinute.value).dp
    val minVisualHeight = if (isCompact) 32.dp else 60.dp
    val computedHeight = physicalHeight.coerceAtLeast(minVisualHeight)

    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var previewTime by remember { mutableStateOf("") }

    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // ЛОГІКА ВІДОБРАЖЕННЯ "ТАБЛЕТКИ" (Overlay Pill):
    val showOverlayTime = if (isCompact) {
        isDragging
    } else {
        task.startTimeMinutes != null || isDragging
    }

    val isDarkTheme = isSystemInDarkTheme()
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(computedHeight)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .padding(bottom = 2.dp, end = 16.dp)
            .pointerInput(task.startTimeMinutes) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
            .scale(if (isDragging) 1.05f else 1f)
            .shadow(
                elevation = if (isDragging) 12.dp else 2.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .zIndex(if (isDragging) 10f else if (isCompact) 1f else 0f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Кольорова смужка
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(if (task.isCompleted) Color.LightGray else taskColor)
                )

                // Вміст картки
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    if (isCompact) {
                        // --- КОМПАКТНИЙ РЕЖИМ (< 15 хв) ---
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Час (якщо не тягнемо)
                                if (task.startTimeMinutes != null && !isDragging) {
                                    Text(
                                        text = minutesToTime(task.startTimeMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = taskColor,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }

                                // СМАЙЛИК (Compact)
                                Text(
                                    text = task.iconEmoji,
                                    modifier = Modifier.padding(end = 6.dp),
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Іконки
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (task.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onCheck() },
                                    modifier = Modifier.scale(0.7f).size(32.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = taskColor,
                                        uncheckedColor = taskColor.copy(alpha = 0.5f),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    } else {
                        // --- СТАНДАРТНИЙ РЕЖИМ (>= 15 хв) ---
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Відступ під "таблетку" часу
                                if (showOverlayTime) Spacer(modifier = Modifier.height(18.dp))

                                // Рядок з Смайликом і Назвою
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // СМАЙЛИК (Standard)
                                    Text(
                                        text = task.iconEmoji,
                                        modifier = Modifier.padding(end = 8.dp),
                                        fontSize = 18.sp
                                    )

                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = if (task.durationMinutes < 45) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = formatDuration(task.durationMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Іконки справа
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
                    }

                    // 3. OVERLAY PILL (Плаваюча таблетка з часом)
                    if (showOverlayTime) {
                        val timeToShow = if (isDragging) previewTime else minutesToTime(task.startTimeMinutes ?: minTime)
                        val pillColor = if (isDragging) taskColor else MaterialTheme.colorScheme.surfaceVariant
                        val pillContentColor = if (isDragging) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            color = pillColor,
                            contentColor = pillContentColor,
                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                            modifier = Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = (-0).dp)
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

@Composable
fun InboxItem(
    task: Task,
    onCheck: () -> Unit,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit = {}
) {
    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(60.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                )
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кольорова смужка зліва
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(if (task.isCompleted) Color.LightGray else taskColor)
            )

            // СМАЙЛИК (Inbox)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.iconEmoji,
                    fontSize = 22.sp
                )
            }

            // Контент
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp), // Трохи змінили відступи
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = formatDuration(task.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Чекбокс
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCheck() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = taskColor,
                        uncheckedColor = taskColor.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}