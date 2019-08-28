package com.lukelorusso.calendartrendviewsample

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.lukelorusso.calendartrendview.CalendarTrendView
import com.lukelorusso.calendartrendview.parseToString
import com.lukelorusso.calendartrendview.toLocalDate
import com.lukelorusso.calendartrendview.todayToLocalDate
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MY_DATE_PATTERN = "dd/MM/yyyy"
    }

    private var colors = listOf<Int>()
    private var labels = listOf<String>()
    private var trends = listOf<CalendarTrendView.Trend>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initData()
        initView()
    }

    //region PRIVATE METHODS
    private fun initData() {
        labels = mutableListOf(
            getString(R.string.health),
            getString(R.string.welfare),
            getString(R.string.serenity),
            getString(R.string.relation)
        )

        colors = mutableListOf(
            ContextCompat.getColor(this, R.color.graphHealth),
            ContextCompat.getColor(this, R.color.graphWelfare),
            ContextCompat.getColor(this, R.color.graphSerenity),
            ContextCompat.getColor(this, R.color.graphRelation)
        )

        trends = mutableListOf(
            CalendarTrendView.Trend(
                labels[0],
                hashMapOf(
                    "11/05/2019" to 7F,
                    "12/05/2019" to 7F,
                    "13/05/2019" to 8F,
                    "14/05/2019" to 8F,
                    "15/05/2019" to 8F,
                    "16/05/2019" to 7.2F,
                    "17/05/2019" to 7.3F,
                    "18/05/2019" to 7.6F,
                    "19/05/2019" to 7.7F,
                    "20/05/2019" to 8.7F,
                    "21/05/2019" to 8.8F,
                    "22/05/2019" to 9.3F,
                    "23/05/2019" to 9.4F
                ),
                colors[0]
            ),

            CalendarTrendView.Trend(
                labels[1],
                hashMapOf(
                    "11/05/2019" to 4F,
                    "12/05/2019" to 4.3F,
                    "13/05/2019" to 4F,
                    "14/05/2019" to 4F,
                    "15/05/2019" to 4.3F,
                    "16/05/2019" to 5.2F,
                    "17/05/2019" to 5.3F,
                    "18/05/2019" to 5.6F,
                    "19/05/2019" to 5.7F,
                    "20/05/2019" to 6.7F,
                    "21/05/2019" to 6.8F,
                    "22/05/2019" to 7.3F,
                    "23/05/2019" to 6.9F
                ),
                colors[1]
            ),

            CalendarTrendView.Trend(
                labels[2],
                hashMapOf(
                    "11/05/2019" to 3F,
                    "12/05/2019" to 2.8F,
                    "13/05/2019" to 2.5F,
                    "14/05/2019" to 2.5F,
                    "15/05/2019" to 2.7F,
                    "16/05/2019" to 4.2F,
                    "17/05/2019" to 4.3F,
                    "18/05/2019" to 4.6F,
                    "19/05/2019" to 4.7F,
                    "20/05/2019" to 5F,
                    "21/05/2019" to 5F,
                    "22/05/2019" to 5.3F,
                    "23/05/2019" to 5.1F
                ),
                colors[2]
            ),

            CalendarTrendView.Trend(
                labels[3],
                hashMapOf(
                    "11/05/2019" to 1F,
                    "12/05/2019" to 1.1F,
                    "13/05/2019" to 0.9F,
                    "14/05/2019" to 0.9F,
                    "15/05/2019" to 0.8F,
                    "16/05/2019" to 1.8F,
                    "17/05/2019" to 1.8F,
                    "18/05/2019" to 2.2F,
                    "19/05/2019" to 2.3F,
                    "20/05/2019" to 3F,
                    "21/05/2019" to 3.1F,
                    "22/05/2019" to 3.7F,
                    "23/05/2019" to 3.3F
                ),
                colors[3]
            )
        )
    }

    private fun initView() {
        calendarTrendView.labelTypeFace = ResourcesCompat.getFont(this, R.font.proxima_nova_regular)
        calendarTrendView.dateFormatPattern = MY_DATE_PATTERN
        calendarTrendView.lineWeightsInDp = 4F
        calendarTrendView.numberOfDaysToShowAtLeast = 14
        if (isDayTracked(todayToLocalDate())) {
            calendarTrendView.todayLabelColor = calendarTrendView.dayLabelColor
        }
        calendarTrendView.setTrends(trends.toMutableList()) // in this way I set a copy of my list
        calendarTrendView.setOnDrawListener {
            calendarScrollView.post {
                calendarScrollView.fullScroll(
                    View.FOCUS_RIGHT
                )
            }
        }

        createRatioButtons()

        mainBtnAddValues.setOnClickListener {
            addTrendValues(
                todayToLocalDate().parseToString(MY_DATE_PATTERN),
                listOf(10F, 10F, 10F, 10F)
            )
        }
    }

    private fun createRatioButtons() {
        calendarTrendsCheckGroup.removeAllViews()

        for ((i, label) in labels.withIndex()) {
            val checkBox = CheckBox(this).apply {
                text = label
                isChecked = true
                setTextColor(colors[i])
            }
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) trends.forEach { trend ->
                    if (label == trend.label) calendarTrendView.addTrend(
                        trend
                    )
                }
                else calendarTrendView.removeTrendByLabel(label)
            }
            calendarTrendsCheckGroup.addView(checkBox)
        }
    }

    private fun isDayTracked(day: LocalDate): Boolean {
        for (trend in trends) for (entry in trend.valueMap) {
            if (entry.key.toLocalDate(MY_DATE_PATTERN) == day) return true
        }
        return false
    }

    private fun addTrendValues(dateAsString: String, newValues: List<Float>) {
        if (trends.size == newValues.size) {
            for (i in 0 until trends.size) {
                val valueMap: HashMap<String, Float?> = trends[i].valueMap
                valueMap[dateAsString] = newValues[i]
                trends[i].valueMap = valueMap
            }
            initView()
        }
    }
    //endregion

}
