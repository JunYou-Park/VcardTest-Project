package com.ctb.vcardtest_project

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView

class LetterImageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ImageView(context, attrs, defStyleAttr) {
    var mDrawable: Drawable? = null
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (mDrawable != null) mDrawable!!.draw(canvas)
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        mDrawable = null
    }

    override fun setBackground(background: Drawable) {
        super.setBackground(background)
        mDrawable = null
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mDrawable = drawable
    }
}
