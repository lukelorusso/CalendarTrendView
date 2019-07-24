package com.lukelorusso.calendartrendviewsample

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.lukelorusso.calendartrendview.CalendarTrendView
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

class MainActivity : AppCompatActivity() {

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
                    "2019-05-11" to 7F,
                    "2019-05-12" to 7F,
                    "2019-05-13" to 8F,
                    "2019-05-14" to 8F,
                    "2019-05-15" to 8F,
                    "2019-05-16" to 7.2F,
                    "2019-05-17" to 7.3F,
                    "2019-05-18" to 7.6F,
                    "2019-05-19" to 7.7F,
                    "2019-05-20" to 8.7F,
                    "2019-05-21" to 8.8F,
                    "2019-05-22" to 9.3F,
                    "2019-05-23" to 9.4F
                ),
                colors[0]
            ),

            CalendarTrendView.Trend(
                labels[1],
                hashMapOf(
                    "2019-05-11" to 4F,
                    "2019-05-12" to 4.3F,
                    "2019-05-13" to 4F,
                    "2019-05-14" to 4F,
                    "2019-05-15" to 4.3F,
                    "2019-05-16" to 5.2F,
                    "2019-05-17" to 5.3F,
                    "2019-05-18" to 5.6F,
                    "2019-05-19" to 5.7F,
                    "2019-05-20" to 6.7F,
                    "2019-05-21" to 6.8F,
                    "2019-05-22" to 7.3F,
                    "2019-05-23" to 6.9F
                ),
                colors[1]
            ),

            CalendarTrendView.Trend(
                labels[2],
                hashMapOf(
                    "2019-05-11" to 3F,
                    "2019-05-12" to 2.8F,
                    "2019-05-13" to 2.5F,
                    "2019-05-14" to 2.5F,
                    "2019-05-15" to 2.7F,
                    "2019-05-16" to 4.2F,
                    "2019-05-17" to 4.3F,
                    "2019-05-18" to 4.6F,
                    "2019-05-19" to 4.7F,
                    "2019-05-20" to 5F,
                    "2019-05-21" to 5F,
                    "2019-05-22" to 5.3F,
                    "2019-05-23" to 5.1F
                ),
                colors[2]
            ),

            CalendarTrendView.Trend(
                labels[3],
                hashMapOf(
                    "2019-05-11" to 1F,
                    "2019-05-12" to 1.1F,
                    "2019-05-13" to 0.9F,
                    "2019-05-14" to 0.9F,
                    "2019-05-15" to 0.8F,
                    "2019-05-16" to 1.8F,
                    "2019-05-17" to 1.8F,
                    "2019-05-18" to 2.2F,
                    "2019-05-19" to 2.3F,
                    "2019-05-20" to 3F,
                    "2019-05-21" to 3.1F,
                    "2019-05-22" to 3.7F,
                    "2019-05-23" to 3.3F
                ),
                colors[3]
            )
        )
    }

    private fun initView() {
        calendarTrendView.labelTypeFace = ResourcesCompat.getFont(this, R.font.proxima_nova_regular)
        calendarTrendView.lineWeightsInDp = 4F
        calendarTrendView.numberOfDaysToShowAtLeast = 14
        if (isDayTracked(calendarTrendView.today())) {
            calendarTrendView.todayLabelColor = calendarTrendView.dayLabelColor
        }
        calendarTrendView.setTrends(trends.toMutableList()) // in this way I set a copy of my list
        calendarTrendView.setOnDrawListener { calendarScrollView.post { calendarScrollView.fullScroll(View.FOCUS_RIGHT) } }

        createRatioButtons()

        mainBtnAddValues.setOnClickListener {
            val todayAsString = calendarTrendView.dateTimeFormatter.format(calendarTrendView.today())
            addTrendValues(todayAsString, listOf(10F, 10F, 10F, 10F))
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
                if (isChecked) trends.forEach { trend -> if (label == trend.label) calendarTrendView.addTrend(trend) }
                else calendarTrendView.removeTrendByLabel(label)
            }
            calendarTrendsCheckGroup.addView(checkBox)
        }
    }

    private fun isDayTracked(day: LocalDate): Boolean {
        for (trend in trends) for (value in trend.values) {
            val formatter = calendarTrendView.dateTimeFormatter
            val date = LocalDate.parse(value.key.subSequence(0, 10), formatter)
            if (date == day) return true
        }
        return false
    }

    private fun addTrendValues(dateAsString: String, newValues: List<Float>) {
        if (trends.size == newValues.size) {
            for (i in 0 until trends.size) {
                val values: HashMap<String, Float?> = trends[i].values
                values[dateAsString] = newValues[i]
                trends[i].values = values
            }
            initView()
        }
    }
    //endregion

}
