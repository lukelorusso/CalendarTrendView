@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.lukelorusso.calendartrendview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import com.lukelorusso.extensions.pixelToDp
import com.lukelorusso.simplepaperview.SimplePaperView
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.TextStyle
import java.util.*

/**
 * Show a cartesian trend graph based on calendar dates
 */
class CalendarTrendView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, listener: (() -> Unit)? = null
) : SimplePaperView(context, attrs, defStyleAttr, listener) {

    companion object {
        private const val DEFAULT_MAX_VALUE = 10F
        private const val DEFAULT_MIN_VALUE = 0F
    }

    enum class StartFrom {
        NOWHERE,
        ORIGIN,
        FIRST_VALUE
    }

    var maxValue = DEFAULT_MAX_VALUE
    var minValue = DEFAULT_MIN_VALUE
    var xUnitMeasureInDp = 0F
    var yUnitMeasureInDp = 0F
    var paddingBottomInDp = 40F
    var paddingRightInDp = 18F
    var stepLineColor = Color.GRAY
    var dayLabelColor = Color.BLACK
    var monthLabelColor = Color.BLACK
    var todayLabelColor = Color.BLACK
    private var trends = mutableListOf<Trend>()
    private var startFrom = StartFrom.NOWHERE
    private var showToday = false
    var labelTypeFace: Typeface? = null
    var zoneOffset: ZoneOffset = ZoneOffset.of("+01:00")

    init {
        invertY = true

        // Load the styled attributes and set their properties
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CalendarTrendView, 0, 0)
        startFrom =
            StartFrom.values()[attributes.getInt(R.styleable.CalendarTrendView_ctv_startFrom, startFrom.ordinal)]
        showToday = attributes.getBoolean(R.styleable.CalendarTrendView_ctv_showToday, showToday)
        stepLineColor = attributes.getColor(R.styleable.CalendarTrendView_ctv_stepLineColor, stepLineColor)
        dayLabelColor = attributes.getColor(R.styleable.CalendarTrendView_ctv_dayLabelColor, dayLabelColor)
        monthLabelColor = attributes.getColor(R.styleable.CalendarTrendView_ctv_monthLabelColor, monthLabelColor)
        todayLabelColor = attributes.getColor(R.styleable.CalendarTrendView_ctv_todayLabelColor, todayLabelColor)
        xUnitMeasureInDp =
            context.pixelToDp(
                attributes.getDimensionPixelSize(
                    R.styleable.CalendarTrendView_ctv_xUnitMeasure,
                    0
                ).toFloat()
            )
        yUnitMeasureInDp =
            context.pixelToDp(
                attributes.getDimensionPixelSize(
                    R.styleable.CalendarTrendView_ctv_yUnitMeasure,
                    0
                ).toFloat()
            )
        attributes.recycle()
    }

    /**
     * Trend values can be ONLY in the minValue..maxValue range
     */
    fun addTrend(trend: Trend) {
        trends.add(trend)
        drawTrends()
    }

    fun removeTrend(trend: Trend) {
        trends.remove(trend)
        drawTrends()
    }

    fun removeTrendAt(position: Int) {
        trends.removeAt(position)
        drawTrends()
    }

    fun removeTrendByLabel(label: String) {
        trends.forEach { trend ->
            if (label == trend.label) {
                removeTrend(trend)
                return
            }
        }
    }

    /**
     * Trends values can be ONLY in the minValue..maxValue range
     */
    fun setTrends(trends: MutableList<Trend>) {
        this.trends = trends
        drawTrends()
    }

    fun clearTrends() {
        trends.clear()
        drawTrends()
    }

    private fun drawTrends() {
        clearPaper(false)

        var maxWidth = 0F
        //var maxHeight = 0F
        val trendsToDraw = arrayListOf<DrawableItem>()
        val dotsToDraw = arrayListOf<Circle>()
        val setOfDates = getUniqueDates()
        var numberOfStepLines = 0

        // Collecting all the trend lines to draw
        for (trend in trends) {
            val values = trend.values.toSortedMap()
            var lastValue = 0F // used to know where to start drawing a value's line
            var countFromFirstDay = 0 // will concur to the "numberOfStepLines"
            var lastDrawnCount = 0
            var i = 0 // is the count of the values inside a trend
            for ((key, value) in values) {
                i++
                countFromFirstDay = setOfDates.indexOf(key) + 1
                var croppedValue: Float
                value?.also {

                    // Avoiding cheating the imposed range
                    croppedValue = when {
                        value > maxValue -> maxValue
                        value < minValue -> minValue
                        else -> value
                    }

                    // Applying starting behaviour
                    if (i == 1) {
                        when (startFrom) {
                            StartFrom.NOWHERE -> {
                                lastDrawnCount = countFromFirstDay
                                lastValue = croppedValue
                            }
                            StartFrom.FIRST_VALUE -> lastValue = croppedValue
                            else -> {
                            }
                        }
                    }

                    val ax = lastDrawnCount * xUnitMeasureInDp
                    var ay = lastValue * yUnitMeasureInDp
                    if (paddingBottomInDp > 0) ay += paddingBottomInDp
                    val bx = countFromFirstDay * xUnitMeasureInDp
                    var by = croppedValue * yUnitMeasureInDp
                    if (paddingBottomInDp > 0) by += paddingBottomInDp
                    lastValue = croppedValue

                    val line = Line(ax, ay, bx, by, trend.color, trend.lineWeightsInDp)
                    trendsToDraw.add(line)

                    maxWidth = Math.max(maxWidth, Math.max(ax, bx))
                    //maxHeight = Math.max(maxHeight, Math.max(y, dy))
                    lastDrawnCount = countFromFirstDay

                    // Collecting trend's final dots
                    dotsToDraw.add(
                        Circle(
                            line.dx,
                            line.dy,
                            line.weight,
                            Color.WHITE
                        )
                    )
                    dotsToDraw.add(
                        Circle(
                            line.dx,
                            line.dy,
                            line.weight * 0.8F,
                            line.color
                        )
                    )
                }
            }

            numberOfStepLines = Math.max(numberOfStepLines, countFromFirstDay)
        }

        // Collecting vertical step lines
        val stepLinesToDraw = arrayListOf<Line>()
        val labelsToDraw = arrayListOf<DrawableItem>()
        @Suppress("UnnecessaryVariable")
        for (i in 0..setOfDates.size) {
            val ax = i * xUnitMeasureInDp
            val ay = 10 * yUnitMeasureInDp + paddingBottomInDp
            val bx = ax
            var by = 0F
            if (paddingBottomInDp > 0) by += paddingBottomInDp

            stepLinesToDraw.add(Line(ax, ay, bx, by, stepLineColor, 0.8F))
            maxWidth = Math.max(maxWidth, Math.max(ax, bx))

            // Creating day and month labels
            if (i > 0) {
                val date = setOfDates.elementAt(i - 1)
                val isToday = date == today()

                // Today particular case
                if (isToday) {
                    var background = getBackgroundColor()
                    if (background == Color.TRANSPARENT) background = Color.WHITE

                    labelsToDraw.add(
                        Circle(
                            bx, 19F, 14F, todayLabelColor
                        )
                    )

                    labelsToDraw.add(
                        Circle(
                            bx, 19F, 13F, background
                        )
                    )
                }

                labelsToDraw.add(
                    TextLabel(
                        date.dayOfMonth.toString(),
                        10F,
                        ax,
                        8F,
                        if (date == today()) todayLabelColor
                        else dayLabelColor,
                        true,
                        labelTypeFace
                    )
                )

                labelsToDraw.add(
                    TextLabel(
                        date.month.getDisplayName(
                            TextStyle.FULL,
                            resources.configuration.locales[0]
                        ).substring(0, 3).toUpperCase(),
                        8F,
                        bx,
                        2F,
                        if (isToday) todayLabelColor
                        else monthLabelColor,
                        true,
                        labelTypeFace
                    )
                )
            }
        }

        // Drawing collected items
        drawInDp(stepLinesToDraw, false)
        var i = 0
        trendsToDraw.forEach { trendLine ->
            drawInDp(trendLine, false)

            // drawing previous line's dots (avoiding overlapping)
            if (i > 0 && dotsToDraw.size > 1) {
                val dot1 = dotsToDraw[0]
                val dot2 = dotsToDraw[1]
                drawInDp(dot1, false)
                drawInDp(dot2, false)
                dotsToDraw.remove(dot1)
                dotsToDraw.remove(dot2)
            }
            i++
        }
        drawInDp(dotsToDraw, false) // drawing eventual remaining dots
        drawInDp(labelsToDraw, false)

        // Forcing padding right by drawing transparent line
        // + invalidating PaperView
        drawInDp(
            Line(
                0F,
                0F,
                maxWidth + paddingRightInDp,
                10 * yUnitMeasureInDp + paddingBottomInDp,
                Color.TRANSPARENT,
                1F
            ), true
        )
    }

    private fun getUniqueDates(): Set<LocalDate> {
        val setOfDates = mutableSetOf<LocalDate>()
        trends.forEach { trend ->
            trend.values.forEach { value -> setOfDates.add(value.key) }
        }
        if (showToday) setOfDates.add(today())
        return setOfDates.sorted().toSet()
    }

    private fun today(): LocalDate {
        val now = Calendar.getInstance().timeInMillis
        return LocalDateTime.ofEpochSecond(now / 1000, 0, zoneOffset).toLocalDate()
    }

    class Trend(
        var label: String,
        var values: HashMap<LocalDate, Float?> = hashMapOf(),
        var color: Int = Color.BLACK,
        var lineWeightsInDp: Float = 4F
    )

}
