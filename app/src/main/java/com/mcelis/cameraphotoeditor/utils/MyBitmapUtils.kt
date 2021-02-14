package com.mcelis.cameraphotoeditor.utils

import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import java.io.IOException


object MyBitmapUtils {

    fun rotateImage(ivPhoto: ImageView, rotate: Float, axis: String) {
        when(axis){
            Z_AXIS -> {
                ivPhoto.rotation = ivPhoto.rotation + rotate
            }
            Y_AXIS -> {
                ivPhoto.rotationY = ivPhoto.rotationY + rotate
            }
            X_AXIS -> {
                ivPhoto.rotationX = ivPhoto.rotationX + rotate
            }
        }
    }

    fun getCameraPhotoOrientation(imagePath: String?): Int {
        var rotate = 0
        try {
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(imagePath!!)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            val orientation = exif!!.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0)
            rotate = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_270 -> 90
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rotate
    }

    fun getRandomFilter(progress: Int): PorterDuffColorFilter {
        return if (progress >= 100) {
            val value = (progress - 100) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
        } else {
            val value = (100 - progress) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun setBrightness(progress: Int): PorterDuffColorFilter {
        return if (progress >= 100) {
            val value = (progress - 100) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
        } else {
            val value = (100 - progress) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        }
    }

    fun changeImageViewContrast(bmp: Bitmap, contrast: Float): Bitmap? {
        val brightness = 1f
        val cm = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness, 0f, contrast, 0f, 0f, brightness, 0f, 0f, contrast, 0f, brightness, 0f, 0f, 0f, 1f, 0f))

        val ret = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)

        val canvas = Canvas(ret)

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bmp, 0f, 0f, paint)

        return ret
    }
}