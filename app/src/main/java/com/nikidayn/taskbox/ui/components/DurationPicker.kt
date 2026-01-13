package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun DurationWheelPicker(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit
) {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp), // Висота контейнера
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- ГОДИНИ (0-23) ---
        WheelPicker(
            count = 24,
            initialValue = hours,
            label = "год",
            onValueChange = { newHours ->
                onDurationChange(newHours * 60 + minutes)
            }
        )

        // Відступ між колонками
        Spacer(modifier = Modifier.width(50.dp))

        // --- ХВИЛИНИ (0-59) ---
        WheelPicker(
            count = 60,
            initialValue = minutes,
            label = "хв",
            onValueChange = { newMinutes ->
                onDurationChange(hours * 60 + newMinutes)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    count: Int,
    initialValue: Int,
    label: String,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 35.dp
    val visibleItemsCount = 3

    // Створюємо "нескінченний" список
    val listCount = Int.MAX_VALUE
    val centerIndex = listCount / 2

    // Розрахунок початкової позиції, щоб обране число було по центру
    // Формула (base + initialValue - 1) гарантує, що при initialValue=35 ми станемо на 35, а не на 36
    val base = centerIndex - (centerIndex % count)
    val initialListIndex = base + initialValue - 1

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Відстежуємо зміну значення при скролі
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index ->
                val centerItemIndex = index + 1
                val value = centerItemIndex % count
                if (value < 0) value + count else value
            }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    Box(
        modifier = Modifier.width(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Фонова смужка виділення по центру
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(itemHeight * visibleItemsCount),
            contentPadding = PaddingValues(vertical = 0.dp),
            userScrollEnabled = true
        ) {
            items(listCount) { index ->
                val value = index % count
                val displayValue = if (value < 0) value + count else value

                // Визначаємо, чи цей елемент зараз по центру (активний)
                val isSelected = index == listState.firstVisibleItemIndex + 1

                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", displayValue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isSelected) 24.sp else 18.sp, // Активний шрифт більший
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.5f)
                    )
                }
            }
        }

        // Підпис (год/хв) зсунутий праворуч, щоб не налізав на цифри
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 28.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}