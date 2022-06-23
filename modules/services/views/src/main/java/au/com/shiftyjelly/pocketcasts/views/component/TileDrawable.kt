package au.com.shiftyjelly.pocketcasts.views.component

/*
 *  Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

class TileDrawable(drawable: Drawable, tileMode: Shader.TileMode, val tilePadding: Int = 0) : Drawable() {

    private val paint: Paint

    init {
        paint = Paint().apply {
            shader = BitmapShader(getBitmap(drawable), tileMode, tileMode)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPaint(paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    private fun getBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth + tilePadding * 2, drawable.intrinsicHeight + tilePadding * 2,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(bmp)
        drawable.setBounds(tilePadding, tilePadding, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(c)
        return bmp
    }
}
