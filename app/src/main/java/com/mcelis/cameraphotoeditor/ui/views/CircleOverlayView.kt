package com.mcelis.cameraphotoeditor.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout
import com.mcelis.cameraphotoeditor.R


open class CircleOverlayView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (bitmap == null) {
            createWindowFrame()
        }
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    private fun createWindowFrame() {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(bitmap!!)
        val outerRectangle = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = resources.getColor(R.color.grey)
        paint.alpha = 99
        osCanvas.drawRect(outerRectangle, paint)
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        val centerX = (width / 2).toFloat()
        val centerY = (height / 2).toFloat()
        val radius = resources.getDimensionPixelSize(R.dimen.radius).toFloat()
        osCanvas.drawCircle(centerX, centerY, radius, paint)
    }

    override fun isInEditMode(): Boolean {
        return true
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        bitmap = null
    }
}