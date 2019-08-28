package com.lukelorusso.calendartrendview

import android.content.res.Resources
import android.os.Build
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
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

@Suppress("DEPRECATION")
internal fun Month.getDisplayName(resources: Resources): String = this.getDisplayName(
    TextStyle.FULL,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) resources.configuration.locales[0]
    else resources.configuration.locale
)
