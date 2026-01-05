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
    startHour: Int = 8,
    endHour: Int = 20,
    modifier: Modifier = Modifier, // 1. Приймаємо модифікатор (вагу)
    onTaskClick: (Task) -> Unit,
    onTaskCheck: (Task) -> Unit,
    onTaskTimeChange: (Task, Int) -> Unit
) {
    val hourHeight = 240.dp
    val hoursToShow = endHour - startHour
    val totalHeight = hourHeight * (hoursToShow + 1)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { hourHeight.toPx() }

    // 2. Головний контейнер тепер гнучкий і має скрол
    Box(
        modifier = modifier // Сюди прийде вага (weight) з MainActivity
            .fillMaxWidth()
            .verticalScroll(scrollState) // Скрол тут
            .background(Color.White)
    ) {
        // 3. Колонка для додавання відступу зверху
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- ОСЬ ВІН, ВІДСТУП ЗВЕРХУ (всередині скролу) ---
            Spacer(modifier = Modifier.height(32.dp))
            // -----------------------------------------------

            // 4. А ось тут вже сама величезна сітка
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight) // Фіксуємо висоту тільки для контенту
            ) {
                // СІТКА
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    for (i in 0..hoursToShow) {
                        val y = i * pxPerHour
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(60.dp.toPx(), y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // ЦИФРИ
                Column(modifier = Modifier.fillMaxSize()) {
                    repeat(hoursToShow + 1) { i ->
                        val displayHour = startHour + i
                        Box(modifier = Modifier.height(hourHeight)) {
                            Text(
                                text = String.format("%02d:00", displayHour),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .offset(y = (-8).dp) // Текст піднятий, але завдяки Spacer(32dp) він не обріжеться
                            )
                        }
                    }
                }

                // ЗАВДАННЯ
                tasks.forEach { task ->
                    val startMinutes = task.startTimeMinutes ?: 0
                    val startHourOfTask = startMinutes / 60.0

                    if (startHourOfTask >= startHour) {
                        val offsetDp = ((startHourOfTask - startHour).toFloat()) * hourHeight.value

                        Box(
                            modifier = Modifier
                                .padding(start = 60.dp, end = 16.dp)
                                .fillMaxWidth()
                                .offset(y = offsetDp.dp)
                        ) {
                            TimelineItem(
                                task = task,
                                minTime = startHour * 60, // <-- ПЕРЕДАЄМО МІНІМУМ (у хвилинах)
                                onCheck = { onTaskCheck(task) },
                                onClick = { onTaskClick(task) },
                                onLongClick = {},
                                onTimeChange = { newMinutes ->
                                    val clampedMinutes = newMinutes.coerceIn(0, 1439)
                                    onTaskTimeChange(task, clampedMinutes)
                                }
                            )
                        }
                    }
                }
            }

            // Можна додати відступ і знизу
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}