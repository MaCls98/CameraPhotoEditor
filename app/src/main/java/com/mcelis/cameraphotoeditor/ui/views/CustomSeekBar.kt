package com.mcelis.cameraphotoeditor.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.SeekBar

@SuppressLint("AppCompatCustomView")
class CustomSeekBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : SeekBar(context, attrs, defStyleAttr) {

    private var rect: Rect? = null
    private var paint: Paint? = null
    private var seekbarHeight = 0

    init {
        rect = Rect()
        paint = Paint()
        seekbarHeight = 6
    }

    override fun onDraw(canvas: Canvas?) {
        rect?.set(
                0 + thumbOffset,
                (height/2) - (seekbarHeight/2),
                width-thumbOffset,
                (height/2)+seekbarHeight)
        paint?.color = Color.GRAY
        canvas?.drawRect(rect!!, paint!!)

        if (this.progress > 50){
            rect?.set(width / 2,
                    (height / 2) - (seekbarHeight/2),
                    width / 2 + (width / 100) * (progress - 50),
                    height / 2 + (seekbarHeight/2));

            paint?.color = Color.CYAN;
            canvas?.drawRect(rect!!, paint!!);
        }

        if (this.progress < 50){
            rect?.set(width / 2 - ((width / 100) * (50 - progress)),
                    (height / 2) - (seekbarHeight/2),
                    width / 2,
                    height / 2 + (seekbarHeight/2));

            paint?.color = Color.CYAN;
            canvas?.drawRect(rect!!, paint!!);
        }
    }
}