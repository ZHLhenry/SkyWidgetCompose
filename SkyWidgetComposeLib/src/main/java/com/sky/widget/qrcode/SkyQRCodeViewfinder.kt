package com.sky.widget.qrcode

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * 扫码取景框。
 *
 * 在相机预览上层绘制扫描框外的半透明遮罩、四角边框与移动扫描线。
 * 取景框的视觉位置基于 [state.frameSize] / [state.frameMarginTop] 在当前 Canvas 尺寸中计算，
 * 不依赖外部传入的相对边界，避免容器尺寸获取异常导致绘制位置错误。
 *
 * @param state 扫码器状态
 * @param modifier 外部修饰符
 * @param maskColor 扫描框外遮罩颜色
 * @param cornerColor 四角边框颜色
 * @param cornerLength 四角边框长度
 * @param cornerWidth 四角边框宽度
 * @param scanLineColor 扫描线颜色
 * @param scanLineHeight 扫描线高度
 * @param scanDuration 扫描线单次循环时长
 */
@Composable
fun SkyQRCodeViewfinder(
    state: SkyQRCodeState,
    modifier: Modifier = Modifier,
    maskColor: Color = Color.Black.copy(alpha = 0.5f),
    cornerColor: Color = Color(0xFF45DDDD),
    cornerLength: Dp = 20.dp,
    cornerWidth: Dp = 4.dp,
    scanLineColor: Color = Color(0xFF45DDDD),
    scanLineHeight: Dp = 4.dp,
    scanDuration: Int = 4000
) {
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "sky_qrcode_scan")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(scanDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    Canvas(modifier = modifier) {
        val rect = computeFrameRect(size.width, size.height, state.frameSize, state.frameMarginTop, density)

        drawMask(maskColor, rect.left, rect.top, rect.right, rect.bottom)
        drawFrameCorners(
            cornerColor,
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            with(density) { cornerLength.toPx() },
            with(density) { cornerWidth.toPx() }
        )
        drawScanLine(scanLineColor, rect.left, rect.top, rect.right, rect.bottom, scanProgress, with(density) { scanLineHeight.toPx() })
    }
}

/**
 * 计算取景框矩形（像素坐标）。
 *
 * 取景框视觉绘制与扫码识别区域共用本函数，保证"所见即所扫"：
 * 宽度/高度为 [Dp.Unspecified] 的维度取容器宽度的一半；[Dp.Unspecified] 的顶部边距表示垂直居中。
 */
internal fun computeFrameRect(
    widthPx: Float,
    heightPx: Float,
    frameSize: DpSize,
    frameMarginTop: Dp,
    density: Density
): Rect {
    val frameWidth = frameSize.width.let {
        if (it == Dp.Unspecified) widthPx / 2f else with(density) { it.toPx() }
    }.coerceIn(0f, widthPx)
    val frameHeight = frameSize.height.let {
        if (it == Dp.Unspecified) widthPx / 2f else with(density) { it.toPx() }
    }.coerceIn(0f, heightPx)
    val marginTop = frameMarginTop.let {
        if (it == Dp.Unspecified) (heightPx - frameHeight) / 2f else with(density) { it.toPx() }
    }
    val left = ((widthPx - frameWidth) / 2f).coerceIn(0f, widthPx)
    val top = marginTop.coerceIn(0f, (heightPx - frameHeight).coerceAtLeast(0f))
    return Rect(left, top, left + frameWidth, top + frameHeight)
}

private fun DrawScope.drawMask(color: Color, left: Float, top: Float, right: Float, bottom: Float) {
    drawRect(color = color, topLeft = Offset(0f, 0f), size = Size(size.width, top))
    drawRect(color = color, topLeft = Offset(0f, top), size = Size(left, bottom - top))
    drawRect(color = color, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
    drawRect(color = color, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
}

private fun DrawScope.drawFrameCorners(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerLength: Float,
    cornerWidth: Float
) {
    // 左上角
    drawRect(color = color, topLeft = Offset(left, top), size = Size(cornerWidth, cornerLength))
    drawRect(color = color, topLeft = Offset(left, top), size = Size(cornerLength, cornerWidth))
    // 右上角
    drawRect(color = color, topLeft = Offset(right - cornerWidth, top), size = Size(cornerWidth, cornerLength))
    drawRect(color = color, topLeft = Offset(right - cornerLength, top), size = Size(cornerLength, cornerWidth))
    // 左下角
    drawRect(color = color, topLeft = Offset(left, bottom - cornerLength), size = Size(cornerWidth, cornerLength))
    drawRect(color = color, topLeft = Offset(left, bottom - cornerWidth), size = Size(cornerLength, cornerWidth))
    // 右下角
    drawRect(color = color, topLeft = Offset(right - cornerWidth, bottom - cornerLength), size = Size(cornerWidth, cornerLength))
    drawRect(color = color, topLeft = Offset(right - cornerLength, bottom - cornerWidth), size = Size(cornerLength, cornerWidth))
}

private fun DrawScope.drawScanLine(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    scanProgress: Float,
    lineHeight: Float
) {
    val scanY = top + (bottom - top) * scanProgress
    // 拖尾渐变光带：扫描线上方由透明渐变到半透明，营造流动扫描感，避免生硬色块
    val trailHeight = (bottom - top) / 4f
    val trailTop = (scanY - trailHeight).coerceAtLeast(top)
    if (scanY > trailTop) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.3f)),
                startY = trailTop,
                endY = scanY
            ),
            topLeft = Offset(left, trailTop),
            size = Size(right - left, scanY - trailTop)
        )
    }
    // 前缘主线：圆头端点，柔和精致。
    // Round 端帽会向两端各外扩半个线宽，端点内缩半个线宽保证视觉总宽不超出取景框
    val capRadius = lineHeight / 2f
    drawLine(
        color = color,
        start = Offset(left + capRadius, scanY),
        end = Offset(right - capRadius, scanY),
        strokeWidth = lineHeight,
        cap = StrokeCap.Round
    )
    // 中心高光，提升质感
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(left + lineHeight * 2, scanY),
        end = Offset(right - lineHeight * 2, scanY),
        strokeWidth = lineHeight / 2f,
        cap = StrokeCap.Round
    )
}
