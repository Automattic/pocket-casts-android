package au.com.shiftyjelly.pocketcasts.images.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.PocketCastsIcons

val PocketCastsIcons.StarEmpty: ImageVector
    get() {
        if (_starEmpty != null) {
            return _starEmpty!!
        }
        _starEmpty = Builder(
            name = "StarEmpty", defaultWidth = 16.0.dp, defaultHeight = 16.0.dp,
            viewportWidth = 16.0f, viewportHeight = 16.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF808080)),
                strokeLineWidth = 1.0f, strokeLineCap = Round,
                strokeLineJoin =
                StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f, pathFillType = NonZero,
            ) {
                moveTo(8.402f, 2.2885f)
                lineTo(10.1827f, 5.8149f)
                lineTo(13.6098f, 6.1501f)
                curveTo(13.8439f, 6.166f, 14.0195f, 6.373f, 13.9983f, 6.607f)
                curveTo(13.9876f, 6.7081f, 13.945f, 6.8039f, 13.8705f, 6.873f)
                lineTo(11.0501f, 9.6674f)
                lineTo(12.0931f, 13.4603f)
                curveTo(12.1517f, 13.6891f, 12.0133f, 13.9232f, 11.7845f, 13.987f)
                curveTo(11.678f, 14.0136f, 11.5716f, 13.9976f, 11.4758f, 13.9551f)
                lineTo(8.0013f, 12.2315f)
                lineTo(4.5316f, 13.9444f)
                curveTo(4.3134f, 14.0508f, 4.058f, 13.9657f, 3.9462f, 13.7476f)
                curveTo(3.8984f, 13.6519f, 3.8824f, 13.5401f, 3.9143f, 13.4391f)
                lineTo(4.9574f, 9.6408f)
                lineTo(2.1284f, 6.8422f)
                verticalLineTo(6.8416f)
                curveTo(1.9581f, 6.6719f, 1.9581f, 6.4006f, 2.1231f, 6.2357f)
                curveTo(2.1923f, 6.1612f, 2.288f, 6.1187f, 2.3891f, 6.108f)
                lineTo(5.8163f, 5.7676f)
                lineTo(7.5937f, 2.2406f)
                curveTo(7.7001f, 2.0225f, 7.9609f, 1.9374f, 8.1791f, 2.0491f)
                curveTo(8.2589f, 2.0864f, 8.3228f, 2.1555f, 8.3653f, 2.2353f)
                lineTo(8.402f, 2.2885f)
                close()
            }
        }
            .build()
        return _starEmpty!!
    }

private var _starEmpty: ImageVector? = null
