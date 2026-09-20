package com.sky.widget.ratingbar

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.annotation.FloatRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iface.SkyIconType
import kotlin.math.floor
import kotlin.math.round

/**
 * 评分条组件：支持点击 / 滑动打分、整星与半星、只读展示。
 *
 * 布局为「底层 normal 图标行 + 上层 active 图标行」叠加，active 行按 [value] 绘制；
 * 半星通过 [GenericShape] 按宽度百分比裁剪 active 图标实现，对任意图标类型均生效。
 *
 * 使用示例：
 * ```kotlin
 * // 默认内置五角星（[Painter]），开箱即用
 * SkyRatingBar(value = rating, onChange = { rating = it })
 *
 * // 自定义矢量图标（ImageVector 需经 rememberVectorPainter 包装为 Painter）
 * SkyRatingBar(
 *     value = rating,
 *     activeIcon = SkyIconType.painter(rememberVectorPainter(Icons.Filled.Favorite)),
 *     normalIcon = SkyIconType.painter(rememberVectorPainter(Icons.Filled.FavoriteBorder)),
 *     onChange = { rating = it }
 * )
 *
 * // iconfont 图标（需先注册字体）
 * SkyRatingBar(
 *     value = rating,
 *     activeIcon = SkyIconType.iconFont("skywujiaoxingxuanzhong"),
 *     normalIcon = SkyIconType.iconFont("skywujiaoxingweixuanzhong"),
 *     onChange = { rating = it }
 * )
 * ```
 *
 * @param count 图标数量（总分）
 * @param size 单个图标尺寸
 * @param horizontalSpacing 图标水平间距
 * @param value 当前分值；传 [Float.NaN] 时不绘制选中层（仅展示 normal 图标）
 * @param activeColor 选中图标着色
 * @param normalColor 未选中图标着色
 * @param activeIcon 选中图标，支持 [SkyIconType.PainterIcon]（任意 [Painter]，如 `rememberVectorPainter` 包装的 [ImageVector]）与 [SkyIconType.FontIcon]（iconfont 图标名），默认内置五角星
 * @param normalIcon 未选中图标，支持 [SkyIconType.PainterIcon] 与 [SkyIconType.FontIcon]，默认内置五角星
 * @param allowHalf 是否允许半星（0.5 步进）
 * @param readOnly 是否只读（禁用点击与滑动手势）
 * @param onChange 分值变化回调
 */
@Composable
fun SkyRatingBar(
    count: Int = 5,
    size: DpSize = DpSize(20.dp, 20.dp),
    horizontalSpacing: Dp = 4.dp,
    @SuppressLint("Range") @FloatRange(from = 0.0) value: Float = Float.NaN,
    activeColor: Color = Color(0xFFF7BA2A),
    normalColor: Color = Color(0xFFC6D1DE),
    activeIcon: SkyIconType = DefaultStarIconType,
    normalIcon: SkyIconType = DefaultStarIconType,
    allowHalf: Boolean = false,
    readOnly: Boolean = false,
    onChange: (value: Float) -> Unit
) {
    val density = LocalDensity.current
    val widthPx = with(density) { size.width.toPx() }
    val spacingPx = with(density) { horizontalSpacing.toPx() }
    val currentOnChange by rememberUpdatedState(onChange)
    var boxWidthPx by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .onSizeChanged { boxWidthPx = it.width }
            .pointerInput(readOnly, count, widthPx, spacingPx, allowHalf) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onDragCancel = {},
                    onDragStart = {},
                    onHorizontalDrag = { change, _ ->
                        if (readOnly) {
                            return@detectHorizontalDragGestures
                        }
                        currentOnChange(
                            resolveRating(
                                x = change.position.x,
                                boxWidthPx = boxWidthPx,
                                iconWidthPx = widthPx,
                                spacingPx = spacingPx,
                                count = count,
                                allowHalf = allowHalf
                            )
                        )
                    },
                )
            }
            .pointerInteropFilter {
                if (readOnly) {
                    return@pointerInteropFilter false
                }
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        currentOnChange(
                            resolveRating(
                                x = it.x,
                                boxWidthPx = boxWidthPx,
                                iconWidthPx = widthPx,
                                spacingPx = spacingPx,
                                count = count,
                                allowHalf = allowHalf
                            )
                        )
                    }
                }
                true
            },
    ) {
        // 底层：未选中图标行
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
            repeat(count) {
                SkyRatingIconSlot(normalIcon, normalColor, size)
            }
        }
        // 上层：选中图标行（按 value 叠加绘制）
        if (!value.isNaN()) {
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                repeat(floor(value).toInt()) {
                    SkyRatingIconSlot(activeIcon, activeColor, size)
                }
                if (allowHalf && value > floor(value)) {
                    val percent = value - floor(value)
                    Box {
                        SkyRatingIconSlot(normalIcon, normalColor, size)
                        SkyRatingIconSlot(
                            icon = activeIcon,
                            tint = activeColor,
                            size = size,
                            modifier = Modifier.clip(
                                GenericShape { clipSize, _ ->
                                    moveTo(0f, 0f)
                                    lineTo(clipSize.width * percent, 0f)
                                    lineTo(clipSize.width * percent, clipSize.height)
                                    lineTo(0f, clipSize.height)
                                    close()
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 渲染单个评分图标：按 [SkyIconType] 类型分派到 [Painter] 图片或 iconfont 渲染
 */
@Composable
private fun SkyRatingIconSlot(
    icon: SkyIconType,
    tint: Color,
    size: DpSize,
    modifier: Modifier = Modifier
) {
    when (icon) {
        is SkyIconType.PainterIcon -> Image(
            painter = icon.painter,
            contentDescription = "SkyRatingIcon",
            colorFilter = ColorFilter.tint(tint),
            modifier = modifier.size(size)
        )

        is SkyIconType.FontIcon -> {
            // iconfont 以字号控制图标大小，取宽高中较小值换算，保证 glyph 完整落在尺寸内
            val fontSize = with(LocalDensity.current) { minOf(size.width, size.height).toSp() }
            SkyIconFont(
                iconName = icon.iconName,
                fontName = icon.fontName,
                tint = tint,
                fontSize = fontSize,
                modifier = modifier.size(size)
            )
        }
    }
}

/**
 * 将触摸横坐标换算为合法分值：坐标先收敛到组件宽度内，再按是否允许半星做步进 / 四舍五入取整
 */
private fun resolveRating(
    x: Float,
    boxWidthPx: Int,
    iconWidthPx: Float,
    spacingPx: Float,
    count: Int,
    allowHalf: Boolean
): Float {
    val offset = x.coerceIn(0f, boxWidthPx.toFloat())
    val index = offset / (iconWidthPx + spacingPx)
    return (if (allowHalf) index.stepToHalf() else round(index)).coerceIn(0f, count.toFloat())
}

/**
 * 将连续分值步进到半星刻度：非 0 时最小单位为 0.5（不足半星按半星计），超过半星的部分四舍五入取整
 */
private fun Float.stepToHalf(): Float {
    val base = this.toInt().toFloat()
    if (this < base + 0.5f) {
        if (this == 0f) {
            return 0f
        }
        return base + 0.5f
    }
    return round(this)
}

/**
 * 内置默认五角星 [ImageVector]（Material Star 路径），无需注册字体即可开箱即用。
 *
 * 渲染层在 [DefaultStarIconType] 中通过 [rememberVectorPainter] 转为 [Painter]，
 * 再以 [SkyIconType.PainterIcon] 装配；着色调色由 [ColorFilter.tint] 统一应用 `activeColor` / `normalColor`。
 */
private val DefaultStarIcon: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "SkyRatingStar",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"
        ),
        fill = SolidColor(Color.Black)
    ).build()
}

/**
 * 默认五角星 [PainterIcon]：在 Composable 内用 [rememberVectorPainter] 包装 [DefaultStarIcon]。
 *
 * 仅 [SkyRatingBar] 内部消费（`internal`），避免泄漏到公开 API。
 */
internal val DefaultStarIconType: SkyIconType
    @Composable
    get() {
        val painter = rememberVectorPainter(DefaultStarIcon)
        return remember(painter) { SkyIconType.PainterIcon(painter) }
    }
