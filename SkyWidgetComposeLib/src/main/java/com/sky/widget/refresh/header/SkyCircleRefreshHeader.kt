/*
 * 圆弧与箭头绘制逻辑改编自 AOSP CircularProgressDrawable / Accompanist SwipeRefreshIndicator
 * （Copyright 2021 The Android Open Source Project, Apache License 2.0），
 * 经 Compose 化后适配到本库的状态机与容器模型。
 */
package com.sky.widget.refresh.header

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private val CircleSize = 40.dp
private val ArcRadius = 7.5.dp
private val ArcStrokeWidth = 2.5.dp
private val ArrowWidth = 10.dp
private val ArrowHeight = 5.dp
private val HeaderHeight = 64.dp
private const val MaxProgressArc = 0.8f
private const val CrossfadeDurationMs = 100

/**
 * [SkyCircleRefreshHeader] 的可定制资源状态。所有字段为 null 时使用库内默认值。
 *
 * @param backgroundColor 圆圈底色；null 时取 `MaterialTheme.colorScheme.surface`
 * @param contentColor 圆弧/箭头颜色；null 时取底色对应的内容色
 */
@Stable
class SkyCircleRefreshHeaderState(
    val backgroundColor: Color? = null,
    val contentColor: Color? = null
)

/** 创建并记住 [SkyCircleRefreshHeaderState]。 */
@Composable
fun rememberSkyCircleRefreshHeaderState(
    backgroundColor: Color? = null,
    contentColor: Color? = null
): SkyCircleRefreshHeaderState = remember(backgroundColor, contentColor) {
    SkyCircleRefreshHeaderState(backgroundColor, contentColor)
}

/**
 * 官方 SwipeRefreshLayout 风格的圆圈刷新 Header（适用于 FixedContent 样式场景）：
 * 白底圆形悬浮指示器，下拉过程中圆弧随进度拉伸并带箭头，刷新时切换为转圈动画。
 *
 * 位移由外层 [SkyRefreshLayout] 容器完成，本组件只负责根据下拉进度驱动内部绘制，
 * 因此与 [com.sky.widget.refresh.SkyRefreshStyle.FixedContent]（内容固定、Header 滑入覆盖）
 * 搭配效果最佳，也可用于默认的 Translate 样式。
 *
 * 使用方式（直接传入容器同一个 state）：
 *
 * ```kotlin
 * SkyRefreshLayout(
 *     state = state,
 *     style = SkyRefreshStyle.FixedContent,
 *     header = { SkyCircleRefreshHeader(state) },
 *     ...
 * )
 * ```
 *
 * 仅支持纵向（Vertical）排版。
 *
 * @param state 外层 `SkyRefreshLayout` 的同一个 [SkyRefreshState]
 * @param headerState 可定制资源状态，默认全用库内默认值
 */
@Composable
fun SkyCircleRefreshHeader(
    state: SkyRefreshState,
    headerState: SkyCircleRefreshHeaderState = rememberSkyCircleRefreshHeaderState()
) {
    val backgroundColor = headerState.backgroundColor ?: MaterialTheme.colorScheme.surface
    val contentColor = headerState.contentColor ?: contentColorFor(backgroundColor)
    val refreshing = state.refreshFlag == SkyRefreshFlag.REFRESHING
    val offset = state.indicatorOffset.coerceAtLeast(0f)
    // 触发阈值即自身声明的高度（容器测量后写入 headerBound，二者恒等）
    val trigger = state.headerBound.coerceAtLeast(1f)

    // ── 弹弓（slingshot）进度模型：改编自官方 SwipeRefreshLayout#moveSpinner ──
    // 注：位移部分由容器统一处理，这里只计算弧线起止、旋转与箭头缩放
    val offsetPercent = min(1f, offset / trigger)
    val adjustedPercent = max(offsetPercent - 0.4f, 0f) * 5 / 3
    val extraOffset = abs(offset) - trigger
    val tensionSlingshotPercent = max(0f, min(extraOffset, trigger * 2) / trigger)
    val tensionPercent = ((tensionSlingshotPercent / 4) - (tensionSlingshotPercent / 4).pow(2)) * 2

    val painter = remember { CircleProgressPainter() }
    painter.color = contentColor
    painter.alpha = offsetPercent
    painter.startTrim = 0f
    painter.endTrim = (adjustedPercent * 0.8f).coerceAtMost(MaxProgressArc)
    painter.rotation = (-0.25f + 0.4f * adjustedPercent + tensionPercent * 2) * 0.5f
    painter.arrowScale = min(1f, adjustedPercent)
    painter.arrowEnabled = !refreshing

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(CircleSize)
                // 整体随下拉进度淡入（空闲时完全透明）：
                // 保证 FixedFront 样式下圆圈固定在前方也不会常驻一个空白圆
                .graphicsLayer { alpha = if (refreshing) 1f else offsetPercent },
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = if (refreshing || offset > 0.5f) 6.dp else 0.dp
        ) {
            Crossfade(
                targetState = refreshing,
                animationSpec = tween(durationMillis = CrossfadeDurationMs),
                label = "SkyCircleRefreshHeader"
            ) { isRefreshing ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size((ArcRadius + ArcStrokeWidth) * 2),
                            color = contentColor,
                            strokeWidth = ArcStrokeWidth
                        )
                    } else {
                        Image(painter = painter, contentDescription = null)
                    }
                }
            }
        }
    }
}

/**
 * 圆圈指示器的画笔：负责绘制进度圆弧与末端箭头，将绘制与动画分离。
 * 所有属性均为 Compose 状态，动画更新自动触发重绘。
 */
private class CircleProgressPainter : Painter() {
    var color by mutableStateOf(Color.Unspecified)
    var alpha by mutableFloatStateOf(1f)
    var arrowEnabled by mutableStateOf(false)
    var arrowScale by mutableFloatStateOf(1f)
    var startTrim by mutableFloatStateOf(0f)
    var endTrim by mutableFloatStateOf(0f)
    var rotation by mutableFloatStateOf(0f)

    private val arrow: Path by lazy { Path().apply { fillType = PathFillType.EvenOdd } }

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun DrawScope.onDraw() {
        rotate(degrees = rotation) {
            val arcRadius = ArcRadius.toPx() + ArcStrokeWidth.toPx() / 2f
            val arcBounds = Rect(
                size.center.x - arcRadius,
                size.center.y - arcRadius,
                size.center.x + arcRadius,
                size.center.y + arcRadius
            )
            val startAngle = (startTrim + rotation) * 360
            val endAngle = (endTrim + rotation) * 360
            val sweepAngle = endAngle - startAngle
            drawArc(
                color = color,
                alpha = alpha,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcBounds.topLeft,
                size = arcBounds.size,
                style = Stroke(width = ArcStrokeWidth.toPx(), cap = StrokeCap.Square)
            )
            if (arrowEnabled) {
                drawArrow(startAngle, sweepAngle, arcBounds)
            }
        }
    }

    private fun DrawScope.drawArrow(startAngle: Float, sweepAngle: Float, bounds: Rect) {
        arrow.reset()
        arrow.moveTo(0f, 0f)
        arrow.lineTo(x = ArrowWidth.toPx() * arrowScale, y = 0f)
        arrow.lineTo(x = ArrowWidth.toPx() * arrowScale / 2, y = ArrowHeight.toPx() * arrowScale)
        val radius = min(bounds.width, bounds.height) / 2f
        val inset = ArrowWidth.toPx() * arrowScale / 2f
        arrow.translate(
            Offset(
                x = radius + bounds.center.x - inset,
                y = bounds.center.y + ArcStrokeWidth.toPx() / 2f
            )
        )
        arrow.close()
        rotate(degrees = startAngle + sweepAngle) {
            drawPath(path = arrow, color = color, alpha = alpha)
        }
    }
}
