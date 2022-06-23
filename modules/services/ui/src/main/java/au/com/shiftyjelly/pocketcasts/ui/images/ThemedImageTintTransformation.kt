package au.com.shiftyjelly.pocketcasts.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil.size.Size
import coil.transform.Transformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import timber.log.Timber
import java.security.MessageDigest

class ThemedImageTintTransformation(context: Context) : BitmapTransformation(), Transformation {
    private val isActive = Theme.isImageTintEnabled(context)
    private val themeTag = Theme.imageTintThemeTag(context)

    private val density = context.resources.displayMetrics.density
    private val id = "au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation.$themeTag" // + Date().time
    private val idBytes: ByteArray = id.toByteArray()
    private val paint = Paint().apply {
        color = 0xFF119B00.toInt()
        alpha = 58
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(idBytes)
    }

    override val cacheKey: String
        get() = id

    override fun equals(other: Any?): Boolean {
        return other is ThemedImageTintTransformation
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        if (!isActive) return input

        // Create a Canvas backed by the result Bitmap.
        val canvas = Canvas(result)
        version1(input, canvas)
        return result
    }

    private val bitmapPaint = Paint().apply {
        val colorMatrix = ColorMatrix()
        val sat = Color.alpha(context.getThemeColor(R.attr.image_filter_01)).toFloat() / 255f
        colorMatrix.setSaturation(sat)
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }

    private val darkenPaint = Paint().apply {
        color = context.getThemeColor(R.attr.image_filter_02)
        style = Paint.Style.FILL
        alpha = Color.alpha(this.color)
    }

    private val paintBlendColor = Paint().apply {
        this.style = Paint.Style.FILL
        this.color = context.getThemeColor(R.attr.image_filter_03)
        this.alpha = Color.alpha(this.color)
        this.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    private val paintBlend2Color = Paint().apply {
        this.style = Paint.Style.FILL
        this.color = context.getThemeColor(R.attr.image_filter_04)
        this.alpha = Color.alpha(this.color)
        this.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    fun version1(original: Bitmap, canvas: Canvas) {
        // all green but there is lots of white in there
        canvas.drawBitmap(original, 0f, 0f, bitmapPaint)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), darkenPaint)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintBlendColor)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintBlend2Color)
    }

    override fun transform(bitmapPool: BitmapPool, original: Bitmap, width: Int, height: Int): Bitmap {
        val result: Bitmap = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
        if (!isActive) return original

        Timber.i("Transform width $width height $height density $density q? ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q} sdk ${Build.VERSION.SDK_INT}")

        // Create a Canvas backed by the result Bitmap.
        val canvas = Canvas(result)
        version1(original, canvas)
        return result
    }
}
