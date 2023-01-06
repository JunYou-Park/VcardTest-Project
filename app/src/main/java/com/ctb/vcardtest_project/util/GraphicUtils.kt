package com.ctb.vcardtest_project.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

object GraphicUtils {
    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output: Bitmap = if (bitmap.width > bitmap.height) {
            Bitmap.createBitmap(bitmap.height, bitmap.height, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(bitmap.width, bitmap.width, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        var r = 0f
        r = if (bitmap.width > bitmap.height) {
            (bitmap.height / 2).toFloat()
        } else {
            (bitmap.width / 2).toFloat()
        }
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(r, r, r, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    fun getSmallAvatarBitmap(vector: Drawable): Bitmap {
        return getDefaultAvatarBitmap(2.5f, vector)
    }

    fun getNormalAvatarBitmap(vector: Drawable): Bitmap {
        return getDefaultAvatarBitmap(1f, vector)
    }

    fun getDefaultAvatarBitmap(divide: Float, vector: Drawable): Bitmap {
        val bitmap: Bitmap = Bitmap.createBitmap(
            (vector.intrinsicWidth/divide).toInt(),
            (vector.intrinsicHeight/divide).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(bitmap)
        vector.setBounds(0, 0, c.width, c.height)
        vector.draw(c)
        return bitmap
    }


    fun getVectorDrawableToBitmap(vector: Drawable): Bitmap? {
        return try {
            val bitmap: Bitmap = Bitmap.createBitmap(
                vector.intrinsicWidth,
                vector.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(bitmap)
            vector.setBounds(0, 0, c.width, c.height)
            vector.draw(c)
            bitmap
        } catch (e: OutOfMemoryError) {
            // Handle the error
            null
        }
    }

    private fun getRoundDrawable(context: Context, bitmap: Bitmap): Drawable {
        val roundDrawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
        roundDrawable.setAntiAlias(true)
        roundDrawable.cornerRadius = (bitmap.height / 2).toFloat()
        return roundDrawable
    }
}
