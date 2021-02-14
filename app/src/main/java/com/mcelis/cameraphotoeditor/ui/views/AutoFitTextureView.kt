package com.mcelis.cameraphotoeditor.ui.views

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.TextureView


class AutoFitTextureView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
) : TextureView(context, attrs, defStyle) {

    var maxwidth = 0
    var maxheight = 0
    private var ratioWidth = 0
    private var ratioHeight = 0
    private var previewSize: Size? = null

    fun setAspectRatio(width: Int, height: Int, maxWidth: Int, maxHeight: Int, preview: Size) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        this.maxwidth = maxwidth;
        this.maxheight = maxheight;
        this.previewSize = preview;
        enterTheMatrix()
        requestLayout()
    }

    private fun enterTheMatrix() {
        if (previewSize != null) {
            adjustAspectRatio(ratioWidth,
                    ratioHeight,
                    (context as Activity).windowManager.defaultDisplay.rotation)
        }
    }

    private fun adjustAspectRatio(
            previewWidth: Int,
            previewHeight: Int,
            rotation: Int,
    ) {
        val txform = Matrix()
        val viewWidth = width
        val viewHeight = height
        val rectView = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val viewCenterX = rectView.centerX()
        val viewCenterY = rectView.centerY()
        val rectPreview = RectF(0f, 0f, previewHeight.toFloat(), previewWidth.toFloat())
        val previewCenterX = rectPreview.centerX()
        val previewCenterY = rectPreview.centerY()
        if (Surface.ROTATION_90 === rotation ||
                Surface.ROTATION_270 === rotation) {
            rectPreview.offset(viewCenterX - previewCenterX,
                    viewCenterY - previewCenterY)
            txform.setRectToRect(rectView, rectPreview,
                    Matrix.ScaleToFit.FILL)
            val scale = Math.max(viewHeight.toFloat() / previewHeight,
                    viewWidth.toFloat() / previewWidth)
            txform.postScale(scale, scale, viewCenterX, viewCenterY)
            txform.postRotate(90f * (rotation - 2), viewCenterX,
                    viewCenterY)
        } else {
            if (Surface.ROTATION_180 === rotation) {
                txform.postRotate(180f, viewCenterX, viewCenterY)
            }
        }
        setTransform(txform)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, height);
                //Log.d("rlijeolid1",String.valueOf(width)+"\t"+String.valueOf(height));
            } else {
                setMeasuredDimension(width , height);
                //Log.d("rlijeolid2",String.valueOf(height * mRatioWidth / mRatioHeight)+"\t"+String.valueOf(height));
            }
        }
    }
}