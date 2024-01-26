package au.com.shiftyjelly.pocketcasts.ui.images
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.Px
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.decode.DecodeUtils
import coil.size.Scale
import coil.size.Size
import coil.transform.Transformation

/**
 *
 * This is a modified version of the RoundedCornersTransformation class from Coil,
 * altered to remove cropping to the target's size and prevent scaling issues.
 *
 * https://github.com/coil-kt/coil/issues/421#issuecomment-640133205
 * https://github.com/Automattic/pocket-casts-android/issues/1719
 */

class RoundedCornersTransformation(
    @Px private val topLeft: Float = 0f,
    @Px private val topRight: Float = 0f,
    @Px private val bottomLeft: Float = 0f,
    @Px private val bottomRight: Float = 0f,
) : Transformation {

    constructor(@Px radius: Float) : this(radius, radius, radius, radius)

    init {
        require(topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0) {
            "All radii must be >= 0."
        }
    }

    override val cacheKey = "${javaClass.name}-$topLeft,$topRight,$bottomLeft,$bottomRight"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Do not crop to target's size to avoid scaling issues.
        // https://github.com/coil-kt/coil/issues/421#issuecomment-640133205
        val (outputWidth, outputHeight) = input.width to input.height

        val output = createBitmap(outputWidth, outputHeight, input.config)
        output.applyCanvas {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val matrix = Matrix()
            val multiplier = DecodeUtils.computeSizeMultiplier(
                srcWidth = input.width,
                srcHeight = input.height,
                dstWidth = outputWidth,
                dstHeight = outputHeight,
                scale = Scale.FILL,
            ).toFloat()
            val dx = (outputWidth - multiplier * input.width) / 2
            val dy = (outputHeight - multiplier * input.height) / 2
            matrix.setTranslate(dx, dy)
            matrix.preScale(multiplier, multiplier)

            val shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(matrix)
            paint.shader = shader

            val radii = floatArrayOf(
                topLeft,
                topLeft,
                topRight,
                topRight,
                bottomRight,
                bottomRight,
                bottomLeft,
                bottomLeft,
            )
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            drawPath(path, paint)
        }

        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RoundedCornersTransformation &&
            topLeft == other.topLeft &&
            topRight == other.topRight &&
            bottomLeft == other.bottomLeft &&
            bottomRight == other.bottomRight
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + topRight.hashCode()
        result = 31 * result + bottomLeft.hashCode()
        result = 31 * result + bottomRight.hashCode()
        return result
    }
}
