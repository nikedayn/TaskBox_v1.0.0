package com.nikidayn.taskbox.utils

import android.content.Context

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("taskbox_settings", Context.MODE_PRIVATE)

    // ТЕМА: 0 = Системна, 1 = Світла, 2 = Темна
    fun getThemeMode(): Int = prefs.getInt("theme_mode", 0)
    fun setThemeMode(mode: Int) = prefs.edit().putInt("theme_mode", mode).apply()

    // РОБОЧІ ГОДИНИ (0..24)
    fun getStartHour(): Float = prefs.getFloat("start_hour", 8f) // За замовчуванням 8:00
    fun setStartHour(hour: Float) = prefs.edit().putFloat("start_hour", hour).apply()

    fun getEndHour(): Float = prefs.getFloat("end_hour", 18f) // За замовчуванням 18:00
    fun setEndHour(hour: Float) = prefs.edit().putFloat("end_hour", hour).apply()
}