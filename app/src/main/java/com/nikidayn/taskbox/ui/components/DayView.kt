package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Task
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Виносимо це значення, щоб знати його в MainActivity
val DayViewHourHeight = 240.dp

@Composable
fun DayView(
    tasks: List<Task>,
    startHour: Int = 8,
    endHour: Int = 20,
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    onTaskClick: (Task) -> Unit,
    onTaskCheck: (Task) -> Unit,
    onTaskTimeChange: (Task, Int) -> Unit
) {
    val hourHeight = DayViewHourHeight
    val hoursToShow = endHour - startHour
    val totalHeight = hourHeight * (hoursToShow + 1)

    val density = LocalDensity.current
    val pxPerHour = with(density) { hourHeight.toPx() }

    // Визначаємо кольори з теми для правильного вигляду в Dark Mode
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val hourTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Стан для поточного часу (оновлюється щохвилини)
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000L) // Оновлення кожну хвилину
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                // 1. СІТКА (Canvas)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width

                    // Горизонтальні лінії
                    for (i in 0..hoursToShow) {
                        val y = i * pxPerHour
                        drawLine(
                            color = gridColor,
                            start = Offset(60.dp.toPx(), y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // ЧЕРВОНА ЛІНІЯ (тільки лінія і кружечок, текст малюємо окремо)
                    val currentMinutes = currentTime.hour * 60 + currentTime.minute
                    val startMinutes = startHour * 60
                    val endMinutes = endHour * 60

                    if (currentMinutes in startMinutes..endMinutes) {
                        val minutesFromStart = currentMinutes - startMinutes
                        val yPosition = (minutesFromStart.toFloat() / 60f) * pxPerHour

                        // Лінія
                        drawLine(
                            color = Color.Red,
                            start = Offset(60.dp.toPx(), yPosition),
                            end = Offset(width, yPosition),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Кружечок на початку лінії
                        drawCircle(
                            color = Color.Red,
                            radius = 4.dp.toPx(),
                            center = Offset(60.dp.toPx(), yPosition)
                        )
                    }
                }

                // 2. ЦИФРИ (Години зліва)
                Column(modifier = Modifier.fillMaxSize()) {
                    repeat(hoursToShow + 1) { i ->
                        val displayHour = startHour + i
                        Box(modifier = Modifier.height(hourHeight)) {
                            Text(
                                text = String.format("%02d:00", displayHour),
                                fontSize = 12.sp,
                                color = hourTextColor,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .offset(y = (-8).dp)
                            )
                        }
                    }
                }

                // 3. ЗАВДАННЯ
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
                                minTime = startHour * 60,
                                onCheck = { onTaskCheck(task) },
                                onClick = { onTaskClick(task) },
                                onTimeChange = { newMinutes ->
                                    val clampedMinutes = newMinutes.coerceIn(0, 1439)
                                    onTaskTimeChange(task, clampedMinutes)
                                }
                            )
                        }
                    }
                }

                // 4. ПОТОЧНИЙ ЧАС (Текстова мітка) - малюємо поверх усього
                val currentMinutes = currentTime.hour * 60 + currentTime.minute
                val startMinutes = startHour * 60
                val endMinutes = endHour * 60

                if (currentMinutes in startMinutes..endMinutes) {
                    val minutesFromStart = currentMinutes - startMinutes
                    val offsetDp = (minutesFromStart.toFloat() / 60f) * hourHeight.value

                    Box(
                        modifier = Modifier
                            .offset(y = offsetDp.dp - 12.dp) // -12dp щоб вирівняти центр по лінії
                            .padding(start = 8.dp) // Відступ зліва
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}