package com.lukelorusso.calendartrendview

import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

fun LocalDate.parseToString(pattern: String = "yyyy-MM-dd"): String =
    DateTimeFormatter.ofPattern(pattern).format(this)

fun String.toLocalDate(pattern: String = "yyyy-MM-dd"): LocalDate =
    substring(0, pattern.length - pattern.filter { s -> s == '\'' }.count()).let {
        LocalDate.parse(it, DateTimeFormatter.ofPattern(pattern))
    }

fun todayToLocalDate(): LocalDate = LocalDate.ofEpochDay(
    Calendar.getInstance().timeInMillis / 86400000L // number of milliseconds in a day
)
