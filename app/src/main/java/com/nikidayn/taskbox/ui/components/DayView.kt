package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Task

@Composable
fun DayView(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskCheck: (Task) -> Unit,
    onTaskTimeChange: (Task, Int) -> Unit
) {
    // ВАЖЛИВО: Залишаємо 120.dp, якщо ви хочете "великий" масштаб
    val hourHeight = 120.dp
    val hoursInDay = 24

    val totalHeight = hourHeight * hoursInDay
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { hourHeight.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .verticalScroll(scrollState)
            .background(Color.White)
    ) {
        // 1. Сітка часу
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            for (i in 0..hoursInDay) {
                val y = i * pxPerHour
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(60.dp.toPx(), y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // 2. Цифри годин
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(hoursInDay + 1) { hour ->
                Box(modifier = Modifier.height(hourHeight)) {
                    Text(
                        text = String.format("%02d:00", hour),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .offset(y = (-8).dp)
                    )
                }
            }
        }

        // 3. Завдання
        tasks.forEach { task ->
            val startMinutes = task.startTimeMinutes ?: 0
            val offsetDp = (startMinutes.toFloat() / 60f) * hourHeight.value

            Box(
                modifier = Modifier
                    .padding(start = 60.dp, end = 16.dp)
                    .fillMaxWidth()
                    .offset(y = offsetDp.dp)
            ) {
                TimelineItem(
                    task = task,
                    onCheck = { onTaskCheck(task) },
                    onClick = { onTaskClick(task) },
                    onLongClick = {},
                    onTimeChange = { newMinutes ->
                        // --- ВИПРАВЛЕННЯ ТУТ ---
                        // Ми прибрали snapping ((newMinutes / 15) * 15)
                        // Тепер передаємо "сирі" хвилини, лише обмежуємо діапазоном доби
                        val clampedMinutes = newMinutes.coerceIn(0, 1439)
                        onTaskTimeChange(task, clampedMinutes)
                    }
                )
            }
        }
    }
}