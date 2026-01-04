package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Task
import kotlin.math.roundToInt

@Composable
fun DayView(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskCheck: (Task) -> Unit,
    onTaskTimeChange: (Task, Int) -> Unit
) {
    // КОНСТАНТА: Висота однієї години в пікселях/dp
    // Чим більше число - тим довша шкала
    val hourHeight = 60.dp
    val hoursInDay = 24

    // Загальна висота полотна = 24 години * висоту години
    val totalHeight = hourHeight * hoursInDay

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { hourHeight.toPx() }

    // Контейнер з прокруткою
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight) // Фіксуємо висоту
            .verticalScroll(scrollState) // Додаємо скрол
            .background(Color.White)
    ) {
        // 1. МАЛЮЄМО СІТКУ ЧАСУ (ФОН)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            for (i in 0..hoursInDay) {
                val y = i * pxPerHour
                // Малюємо лінію
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(60.dp.toPx(), y), // Відступ зліва для тексту
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // 2. МАЛЮЄМО ЦИФРИ ГОДИН
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(hoursInDay + 1) { hour ->
                Box(modifier = Modifier.height(hourHeight)) {
                    Text(
                        text = String.format("%02d:00", hour),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .offset(y = (-8).dp) // Трохи піднімаємо, щоб було на лінії
                    )
                }
            }
        }

        // 3. РОЗМІЩУЄМО ЗАВДАННЯ
        tasks.forEach { task ->
            // Вираховуємо позицію Y на основі часу початку
            // Час (хвилини) / 60 = Години. Години * ВисотаГодини = Y
            val startMinutes = task.startTimeMinutes ?: 0
            val offsetDp = (startMinutes.toFloat() / 60f) * hourHeight.value

            Box(
                modifier = Modifier
                    .padding(start = 60.dp, end = 16.dp) // Відступ від шкали часу
                    .fillMaxWidth()
                    .offset(y = offsetDp.dp) // <--- ГОЛОВНА МАГІЯ ПОЗИЦІОНУВАННЯ
            ) {
                TimelineItem(
                    task = task,
                    onCheck = { onTaskCheck(task) },
                    onClick = { onTaskClick(task) },
                    onLongClick = {}, // Можна додати пізніше
                    onTimeChange = { newMinutes ->
                        // Додаємо "прилипання" (Snapping) до 15 хвилин
                        // Це вирішує проблему "рандомайзера"
                        val snappedMinutes = ((newMinutes / 15) * 15).coerceIn(0, 1439)
                        onTaskTimeChange(task, snappedMinutes)
                    }
                )
            }
        }
    }
}