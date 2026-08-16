package com.sky.widget.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/**
 * 基准维度：决定以宽或高哪一边作为计算另一边的依据。
 */
enum class SkyPercentBasics {
    /** 以宽度为基准，高度 = 宽度 × percent */
    Width,

    /** 以高度为基准，宽度 = 高度 × percent */
    Height
}

/**
 * 按比例设置宽高的图片组件（纯图片）。
 *
 * 以 [basics] 指定的基准边，按 [percent] 比例自动计算另一条边的尺寸，
 * 从而保持固定宽高比显示图片。对应原 View 体系的 `SkyPercentImageView`。
 *
 * ```
 * SkyPercentImage(
 *     modifier = Modifier.fillMaxWidth(),
 *     painter = painterResource(R.drawable.banner),
 *     basics = SkyPercentBasics.Width,
 *     percent = 0.5f          // 高度为宽度的一半
 * )
 * ```
 *
 * 网络图片源同样支持，只要将加载库（如 Coil）返回的 [Painter] 传入 [painter] 即可：
 * ```
 * val painter = rememberAsyncImagePainter("https://example.com/image.jpg")
 * SkyPercentImage(
 *     modifier = Modifier.fillMaxWidth(),
 *     painter = painter,
 *     basics = SkyPercentBasics.Width,
 *     percent = 0.5f
 * )
 * ```
 *
 * @param modifier 外层修饰符
 * @param painter 图片绘制源，支持本地/矢量/网络等任意 [Painter]
 * @param contentDescription 无障碍描述
 * @param basics 基准边，默认 [SkyPercentBasics.Width]
 * @param percent 比例值（另一条边 = 基准边 × percent），范围通常 0f~1f，默认 1f
 * @param alignment 图片在占位框内的对齐方式
 * @param contentScale 图片在框内的缩放方式，默认 [Alignment.Center]
 */
@Composable
fun SkyPercentImage(
    modifier: Modifier = Modifier,
    painter: Painter,
    contentDescription: String? = null,
    basics: SkyPercentBasics = SkyPercentBasics.Width,
    percent: Float = 1.0f,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit
) {
    val ratio = percent.coerceAtLeast(0f)

    Layout(
        modifier = modifier,
        content = {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    ) { measurables, constraints ->
        val size = calculateSize(basics, ratio, constraints)
        val placeable = measurables.firstOrNull()?.measure(
            Constraints.fixed(size.width, size.height)
        )
        layout(size.width, size.height) {
            placeable?.placeRelative(0, 0)
        }
    }
}

/**
 * 根据基准边与比例计算实际宽高。
 *
 * - [SkyPercentBasics.Width]：宽度取父约束建议值，高度 = 宽 × percent
 * - [SkyPercentBasics.Height]：高度取父约束建议值，宽度 = 高 × percent
 * - 受父约束的 min/max 钳制
 */
internal fun calculateSize(
    basics: SkyPercentBasics,
    percent: Float,
    constraints: Constraints
): IntSize {
    return when (basics) {
        SkyPercentBasics.Width -> {
            val width = constraints.maxWidth
            val height = (width * percent).toInt()
            IntSize(
                width.coerceIn(constraints.minWidth, constraints.maxWidth),
                height.coerceIn(constraints.minHeight, constraints.maxHeight)
            )
        }

        SkyPercentBasics.Height -> {
            val height = constraints.maxHeight
            val width = (height * percent).toInt()
            IntSize(
                width.coerceIn(constraints.minWidth, constraints.maxWidth),
                height.coerceIn(constraints.minHeight, constraints.maxHeight)
            )
        }
    }
}
