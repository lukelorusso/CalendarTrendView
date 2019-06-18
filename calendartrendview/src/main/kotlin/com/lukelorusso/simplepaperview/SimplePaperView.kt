@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.lukelorusso.simplepaperview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.lukelorusso.calendartrendview.R
import com.lukelorusso.extensions.dpToPixel

/**
 * Simple View to draw lines, circles or text labels
 */
open class SimplePaperView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val itemList = mutableListOf<DrawableItem>()
    private var paint = Paint().apply { flags = Paint.ANTI_ALIAS_FLAG }
    private var maxDims = PointF(0F, 0F)
    private var onDrawListener: (() -> Unit)? = null
    var paddingTop = 0F
    var paddingStart = 0F
    var paddingEnd = 0F
    var paddingBottom = 0F
    var invertY = false // if true, you can think to Y dimension as a "classic" cartesian axis (values growing upwards)

    init {
        // Load the styled attributes and set their properties
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SimplePaperView, 0, 0)
        val padding = attributes.getDimension(R.styleable.SimplePaperView_android_padding, 0F)
        paddingTop = attributes.getDimension(R.styleable.SimplePaperView_android_paddingTop, padding)
        paddingStart = attributes.getDimension(R.styleable.SimplePaperView_android_paddingLeft, padding)
        paddingStart = attributes.getDimension(R.styleable.SimplePaperView_android_paddingStart, paddingStart)
        paddingEnd = attributes.getDimension(R.styleable.SimplePaperView_android_paddingRight, padding)
        paddingEnd = attributes.getDimension(R.styleable.SimplePaperView_android_paddingEnd, paddingEnd)
        paddingBottom = attributes.getDimension(R.styleable.SimplePaperView_android_paddingTop, padding)
        attributes.recycle()
    }

    //region PRIVATE METHODS
    override fun onDraw(canvas: Canvas?) {
        if (invertY) {
            invertY(canvas)
        } else {
            canvas?.translate(paddingStart, paddingTop)
        }

        // Iterate through items
        for (it in itemList) when (it) {
            is Line -> {
                paint.strokeWidth = it.weight
                paint.color = it.color
                canvas?.drawLine(
                    it.x,
                    it.y,
                    it.dx,
                    it.dy,
                    paint
                )
            }

            is Circle -> {
                paint.color = it.color
                canvas?.drawCircle(
                    it.x,
                    it.y,
                    it.radius,
                    paint
                )
            }

            is TextLabel -> {
                if (it.staticLayout != null && it.textPaint != null) {
                    if (invertY) invertY(canvas) // otherwise you got reversed texts
                    var x = it.x
                    val y = if (invertY) maxDims.y - it.y else it.y
                    x -= if (it.centerHorizontally) (it.staticLayout!!.width / 2) else 0
                    canvas?.drawText(it.text, x, y, it.textPaint!!)
                    if (invertY) invertY(canvas) // restoring Y axis
                }
            }
        }

        super.onDraw(canvas)
        onDrawListener?.invoke()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        calculateMaxXY()

        // Check against our minimum width and height
        val maxWidth = Math.max(maxDims.x, suggestedMinimumWidth.toFloat())
        val maxHeight = Math.max(maxDims.y, suggestedMinimumHeight.toFloat())
        val state = measuredState

        // Report our final dimensions
        setMeasuredDimension(
            resolveSizeAndState(maxWidth.toInt(), widthMeasureSpec, state),
            resolveSizeAndState(maxHeight.toInt(), heightMeasureSpec, state shl MEASURED_HEIGHT_STATE_SHIFT)
        )
    }

    private fun calculateMaxXY() {
        var maxX = 0F
        var maxY = 0F

        for (i in 0 until itemList.size) {
            when (val item = itemList[i]) {
                is Line -> {
                    maxX = Math.max(maxX, item.x)
                    maxX = Math.max(maxX, item.dx)

                    maxY = Math.max(maxY, item.y)
                    maxY = Math.max(maxY, item.dy)
                }

                is Circle -> {
                    maxX = Math.max(maxX, item.x + item.radius)
                    maxY = Math.max(maxY, item.y + item.radius)
                }

                is TextLabel -> {
                    if (item.staticLayout != null && item.textPaint != null) {
                        maxX = Math.max(
                            maxX, item.x +
                                    if (item.centerHorizontally) (item.staticLayout!!.width / 2)
                                    else item.staticLayout!!.width
                        )
                        maxY = Math.max(
                            maxY, item.y +
                                    if (invertY) item.staticLayout!!.height
                                    else 0
                        )
                    }
                }
            }
        }

        maxDims = PointF(maxX + paddingStart + paddingEnd, maxY + paddingTop + paddingBottom)
    }

    private fun invertY(canvas: Canvas?) {
        canvas?.translate(0F + paddingStart, height.toFloat() + paddingTop) // Reset where 0,0 is located
        canvas?.scale(1F, -1F) // Invert
    }
    //endregion

    //region EXPOSED METHODS
    fun drawInDp(itemList: List<DrawableItem> = mutableListOf(), invalidate: Boolean = true) {
        itemList.forEach { item -> drawInDp(item) }
        if (invalidate) redrawPaper()
    }

    fun drawInPx(itemList: List<DrawableItem> = mutableListOf(), invalidate: Boolean = true) {
        itemList.forEach { item -> drawInPx(item) }
        if (invalidate) redrawPaper()
    }

    fun drawInDp(item: DrawableItem, invalidate: Boolean = true) {
        when (item) {
            is Line -> drawLineInDp(item)
            is Circle -> drawCircleInDp(item)
            is TextLabel -> drawTextInDp(item)
        }
        if (invalidate) redrawPaper()
    }

    fun drawInPx(item: DrawableItem, invalidate: Boolean = true) {
        when (item) {
            is Line -> drawLineInPx(item)
            is Circle -> drawCircleInPx(item)
            is TextLabel -> drawTextInPx(item)
        }
        if (invalidate) redrawPaper()
    }

    fun clearPaper(invalidate: Boolean = true) {
        paint = Paint().apply { flags = Paint.ANTI_ALIAS_FLAG }
        itemList.clear()
        maxDims = PointF(0F, 0F)
        if (invalidate) redrawPaper()
    }

    fun redrawPaper() {
        invalidate()
        requestLayout()
    }

    fun getBackgroundColor(): Int {
        return if (background is ColorDrawable) (background as ColorDrawable).color
        else Color.TRANSPARENT
    }

    open fun setOnDrawListener(listener: (() -> Unit)?) {
        this.onDrawListener = listener
    }
    //endregion

    //region DRAW SPECIFIC ITEM
    private fun drawLineInDp(line: Line, invalidate: Boolean = true) = drawLineInPx(mapToPx(line), invalidate)

    private fun drawLineInPx(line: Line, invalidate: Boolean = true) {
        itemList.add(line)
        if (invalidate) redrawPaper()
    }

    private fun drawCircleInDp(circle: Circle, invalidate: Boolean = true) = drawCircleInPx(mapToPx(circle), invalidate)

    private fun drawCircleInPx(circle: Circle, invalidate: Boolean = true) {
        itemList.add(circle)
        if (invalidate) redrawPaper()
    }

    private fun drawTextInDp(textLabel: TextLabel, invalidate: Boolean = true) =
        drawTextInPx(mapToPx(textLabel), invalidate)

    @Suppress("DEPRECATION")
    private fun drawTextInPx(textLabel: TextLabel, invalidate: Boolean = true) {
        // Creating TextPaint
        val textPaint = TextPaint().apply {
            color = textLabel.color
            flags = Paint.ANTI_ALIAS_FLAG
            textSize = textLabel.textSize
            textLabel.typeface?.also { textTypeface -> typeface = textTypeface }
        }

        // Creating StaticLayout
        val textWidth = textPaint.measureText(textLabel.text).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(textLabel.text, 0, textLabel.text.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build()
        } else {
            StaticLayout(textLabel.text, textPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1F, 0F, false)
        }

        // Applying created objects
        textLabel.apply {
            this.staticLayout = staticLayout
            this.textPaint = textPaint
        }

        itemList.add(textLabel)
        if (invalidate) redrawPaper()
    }
    //endregion

    //region MAPPER
    private fun mapToPx(lineInDp: Line) =
        Line(
            context.dpToPixel(lineInDp.x),
            context.dpToPixel(lineInDp.y),
            context.dpToPixel(lineInDp.dx),
            context.dpToPixel(lineInDp.dy),
            lineInDp.color,
            context.dpToPixel(lineInDp.weight)
        )

    private fun mapToPx(circleInDp: Circle) =
        Circle(
            context.dpToPixel(circleInDp.x),
            context.dpToPixel(circleInDp.y),
            context.dpToPixel(circleInDp.radius),
            circleInDp.color
        )

    private fun mapToPx(textLabel: TextLabel) =
        TextLabel(
            textLabel.text,
            context.dpToPixel(textLabel.textSize),
            context.dpToPixel(textLabel.x),
            context.dpToPixel(textLabel.y),
            textLabel.color,
            textLabel.centerHorizontally,
            textLabel.typeface
        )
    //endregion

    //region MODELS
    open class DrawableItem(
        var x: Float = 0F,
        var y: Float = 0F,
        var color: Int = Color.BLACK
    )

    class Line(
        x: Float = 0F,
        y: Float = 0F,
        var dx: Float = 0F,
        var dy: Float = 0F,
        color: Int = Color.BLACK,
        var weight: Float = 4F
    ) : DrawableItem(x, y, color)

    class Circle(
        x: Float = 0F,
        y: Float = 0F,
        var radius: Float = 0F,
        color: Int = Color.BLACK
    ) : DrawableItem(x, y, color)

    class TextLabel(
        var text: String,
        var textSize: Float,
        x: Float = 0F,
        y: Float = 0F,
        color: Int = Color.BLACK,
        var centerHorizontally: Boolean = false,
        var typeface: Typeface? = null,
        internal var staticLayout: StaticLayout? = null,
        internal var textPaint: TextPaint? = null
    ) : DrawableItem(x, y, color)
    //endregion

}
