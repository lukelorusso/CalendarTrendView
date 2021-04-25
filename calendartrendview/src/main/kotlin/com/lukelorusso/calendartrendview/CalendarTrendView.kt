@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.lukelorusso.calendartrendview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import com.lukelorusso.simplepaperview.SimplePaperView
import org.threeten.bp.LocalDate
import java.util.*
import kotlin.math.max

/**
 * Show a cartesian trend graph based on calendar dates
 */
class CalendarTrendView constructor(context: Context, attrs: AttributeSet) :
    SimplePaperView(context, attrs) {

    companion object {
        private const val DEFAULT_MAX_VALUE = 10F
        private const val DEFAULT_MIN_VALUE = 0F
        private const val INSIDER_DOT_MULTIPLIER = 0.8F
    }

    enum class StartFrom {
        NOWHERE,
        ORIGIN,
        FIRST_VALUE
    }

    private var trends = mutableListOf<Trend>()
    private var startFrom = StartFrom.NOWHERE
    private var showToday = false
    var maxValue = DEFAULT_MAX_VALUE
    var minValue = DEFAULT_MIN_VALUE
    var numberOfDaysToShowAtLeast = 0
    var xUnitMeasureInDp = 0F
    var yUnitMeasureInDp = 0F
    var lineWeightsInDp = 1F
    var paddingBottomInDp = 40F
    var paddingRightInDp = 18F
    var stepLineColor = Color.GRAY
    var dayLabelColor = Color.BLACK
    var monthLabelColor = Color.BLACK
    var todayLabelColor = Color.BLACK
    var labelTypeFace: Typeface? = null
    var dateFormatPattern: String = "yyyy-MM-dd"

    init {
        invertY = true

        // Load the styled attributes and set their properties
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CalendarTrendView, 0, 0)
        startFrom =
            StartFrom.values()[attributes.getInt(
                R.styleable.CalendarTrendView_ctv_startFrom,
                startFrom.ordinal
            )]
        showToday = attributes.getBoolean(R.styleable.CalendarTrendView_ctv_showToday, showToday)
        stepLineColor =
            attributes.getColor(R.styleable.CalendarTrendView_ctv_stepLineColor, stepLineColor)
        dayLabelColor =
            attributes.getColor(R.styleable.CalendarTrendView_ctv_dayLabelColor, dayLabelColor)
        monthLabelColor =
            attributes.getColor(R.styleable.CalendarTrendView_ctv_monthLabelColor, monthLabelColor)
        todayLabelColor =
            attributes.getColor(R.styleable.CalendarTrendView_ctv_todayLabelColor, todayLabelColor)
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

    //region MODELS
    /**
     * Object for this class.
     * @param label The label for the trend.
     * @param valueMap HashMap of date-value in the form of: <"yyyy-MM-dd", 1.0F>.
     * @param color The color hor the trend.
     * @param lineWeightInDp The line weight.
     */
    class Trend(
        var label: String,
        var valueMap: HashMap<String, Float?> = hashMapOf(),
        var color: Int = Color.BLACK,
        var lineWeightInDp: Float? = null
    )
    //endregion

    //region EXPOSED METHODS
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

    fun getUniqueDates(): Set<LocalDate> {
        var setOfDates = mutableSetOf<LocalDate>()
        trends.forEach { trend ->
            trend.valueMap.forEach { value ->
                setOfDates.add(value.key.toLocalDate(dateFormatPattern))
            }
        }
        if (showToday) setOfDates.add(todayToLocalDate())
        setOfDates = setOfDates.toSortedSet()

        if (setOfDates.size < numberOfDaysToShowAtLeast) {
            var lastDay = setOfDates.elementAt(setOfDates.size - 1)
            while (setOfDates.size < numberOfDaysToShowAtLeast) {
                while (setOfDates.contains(lastDay)) lastDay = lastDay.minusDays(1)
                setOfDates.add(lastDay)
            }
        }
        setOfDates = setOfDates.toSortedSet()

        return setOfDates.sorted().toSet()
    }
    //endregion

    //region PRIVATE METHODS
    private fun drawTrends() {
        clearPaper(false)

        var maxWidth = 0F
        var maxWeight = lineWeightsInDp
        val trendsToDraw = arrayListOf<DrawableItem>()
        val dotsToDraw = arrayListOf<Circle>()
        val setOfDates = getUniqueDates()
        var numberOfStepLines = 0

        // Collecting all the trend lines to draw
        for (trend in trends) {
            val sortedMap = hashMapOf<LocalDate, Float?>().apply {
                trend.valueMap.forEach { entry ->
                    this[entry.key.toLocalDate(dateFormatPattern)] = entry.value
                }
            }.toSortedMap()
            var lastValue = 0F // used to know where to start drawing a value's line
            var countFromFirstDay = 0 // will concur to the "numberOfStepLines"
            var lastDrawnCount = 0
            var i = 0 // is the count of the values inside a trend
            for ((key, value) in sortedMap) {
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

                    val weight = trend.lineWeightInDp ?: lineWeightsInDp
                    val line = Line(ax, ay, bx, by, trend.color, weight)
                    trendsToDraw.add(line)
                    maxWeight = max(maxWeight, weight)

                    maxWidth = max(maxWidth, max(ax, bx))
                    //maxHeight = max(maxHeight, max(y, dy))
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
                            line.weight * INSIDER_DOT_MULTIPLIER,
                            line.color
                        )
                    )
                }
            }

            numberOfStepLines = max(numberOfStepLines, countFromFirstDay)
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
            maxWidth = max(maxWidth, max(ax, bx))

            // Creating day and month labels
            if (i > 0) {
                val date = setOfDates.elementAt(i - 1)
                val isToday = date == todayToLocalDate()

                // Today particular case
                if (isToday) {
                    var background = getBackgroundColor()
                    if (background == Color.TRANSPARENT) background = Color.WHITE

                    labelsToDraw.add(
                        Circle(
                            bx, 18F, 14F, todayLabelColor
                        )
                    )

                    labelsToDraw.add(
                        Circle(
                            bx, 18F, 13F, background
                        )
                    )
                }

                labelsToDraw.add(
                    TextLabel(
                        date.dayOfMonth.toString(),
                        10F,
                        ax,
                        18F,
                        if (isToday)
                            todayLabelColor
                        else dayLabelColor,
                        true,
                        labelTypeFace
                    )
                )

                labelsToDraw.add(
                    TextLabel(
                        date.month
                            .getDisplayName(resources)
                            .substring(0, 3)
                            .toUpperCase(Locale.getDefault()),
                        8F,
                        bx,
                        12F,
                        if (isToday)
                            todayLabelColor
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
                (maxValue * yUnitMeasureInDp) + paddingBottomInDp,
                Color.TRANSPARENT,
                maxWeight
            ), false
        )
        drawInDp(
            Circle(
                maxWidth + paddingRightInDp,
                (maxValue * yUnitMeasureInDp) + paddingBottomInDp,
                maxWeight,
                Color.TRANSPARENT
            ), true
        )
    }
    //endregion

}
