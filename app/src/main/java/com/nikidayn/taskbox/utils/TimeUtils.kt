package com.nikidayn.taskbox.utils

import java.util.Locale

// Перетворює хвилини (наприклад, 90) у стрічку ("01:30")
fun minutesToTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}