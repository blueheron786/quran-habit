package com.quranhabit.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC") // Force UTC timezone
    }

    fun getTodayDate(): String = formatter.format(Date())

    // Add this to help debugging
    fun getCurrentDeviceTime(): String {
        return "Device time: ${Date()} | Timezone: ${TimeZone.getDefault().id}"
    }
}
