package com.ctb.vcardtest_project.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.ctb.vcardtest_project.R
import com.ctb.vcardtest_project.util.GraphicUtils.getDefaultAvatarBitmap
import com.ctb.vcardtest_project.util.GraphicUtils.getNormalAvatarBitmap
import com.ctb.vcardtest_project.util.GraphicUtils.getSmallAvatarBitmap
import com.ctb.vcardtest_project.util.VerifyUtils.isEnglishLetterString
import com.ctb.vcardtest_project.util.VerifyUtils.isKoreanLetterString
import kotlin.math.abs

class ContactAvatarDrawable(
    private val context: Context,
    private val name: String,
    private var sizeType: Int,
    hashCode: Int,
    check: Boolean
) : Drawable() {

    companion object{
        private val TAG = ContactAvatarDrawable::class.java.simpleName
        const val TYPE_LETTER = 0
        const val TYPE_CHECK = 1
        const val TYPE_PERSON = 2
        const val TYPE_SMALL = 3
        const val TYPE_NORMAL = 4
        const val TYPE_BIG = 5
    }
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = Rect()
    private val colorArray: TypedArray
    private val defaultAvatar: Drawable
    private val tileFontColor: Int
    private var mainColor = 0
    private var avatarType: Int
    private var offset = 0.0f

    init {
        paint.isFilterBitmap = true
        paint.isDither = true
        colorArray = context.resources.obtainTypedArray(R.array.letter_tile_colors)

        tileFontColor = ContextCompat.getColor(context, R.color.letter_tile_font_color)
        defaultAvatar = if(check){
            avatarType = TYPE_CHECK
            ContextCompat.getDrawable(context, R.drawable.ic_selected)!!
        } else{
            avatarType = TYPE_PERSON
            ContextCompat.getDrawable(context, R.drawable.ic_person_24)!!
        }
        if(isEnglishLetterString(name) || isKoreanLetterString(name)){
            avatarType = TYPE_LETTER
            paint.typeface =
                Typeface.create(
                    context.getString(R.string.letter_tile_letter_font_family),
                    Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            paint.isAntiAlias = true
        }
        generateUniqueColor(hashCode)
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (!isVisible || bounds.isEmpty) {
            return
        }
        paint.color = mainColor
        val minDimension = bounds.width().coerceAtMost(bounds.height())
        canvas.drawCircle(
            bounds.centerX().toFloat(),
            bounds.centerY().toFloat(),
            (minDimension / 2).toFloat(),
            paint
        )
        when(avatarType){
            TYPE_LETTER -> {
                val letterToTileRatio: Float = if (isKoreanLetterString(name)) 0.45f else 0.5f
                val firstStr = if(name.isNotEmpty()) name.substring(0,1).uppercase() else ""
                paint.textSize = letterToTileRatio * minDimension
                paint.getTextBounds(firstStr, 0, 1, rect)
                paint.color = tileFontColor
                canvas.drawText(firstStr, 0, 1, bounds.centerX().toFloat(),
                    bounds.centerY() + offset * bounds.height() + rect.height() / 2,
                    paint
                )
            }
            TYPE_CHECK -> {
                val bitmap: Bitmap = getDefaultAvatarBitmap(1f, defaultAvatar)
                canvas.drawBitmap(
                    bitmap,
                    (bounds.centerX() - bitmap.width / 2).toFloat(),
                    (bounds.centerY() - bitmap.height / 2).toFloat(),
                    paint
                )
            }
            TYPE_PERSON -> {
                val bitmap = when(sizeType){
                    TYPE_SMALL -> {
                        getSmallAvatarBitmap(defaultAvatar)
                    }
                    TYPE_NORMAL -> {
                        getNormalAvatarBitmap(defaultAvatar)
                    }
                    else -> {
                        getNormalAvatarBitmap(defaultAvatar)
                    }
                }
                canvas.drawBitmap(
                    bitmap,
                    (bounds.centerX() - bitmap.width / 2).toFloat(),
                    (bounds.centerY() - bitmap.height / 2).toFloat(),
                    paint
                )
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.OPAQUE", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    private fun generateUniqueColor(hashCode: Int) {
        val color = abs(hashCode) % colorArray.length()
        mainColor = colorArray.getColor(color, ContextCompat.getColor(context,
            R.color.primary_color_dark
        ))
    }

}