package com.sky.widget.signatureView

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

/**
 * 签名板 Composable。
 *
 * 默认尺寸为 [fillMaxWidth] + 200dp 高度，可通过 [modifier] 覆盖。
 *
 * @param modifier 外层修饰符，可用于覆盖默认尺寸或添加额外行为。
 * @param state 签名状态，用于 [SkySignatureViewState.clear] / [SkySignatureViewState.save]。
 * @param color 笔触颜色，默认黑色。
 * @param strokeWidth 笔触宽度，默认 4dp。
 * @param backgroundColor 签名板背景色，默认白色。
 */
@Composable
fun SkySignatureView(
    modifier: Modifier = Modifier,
    state: SkySignatureViewState = rememberSkySignatureViewState(),
    color: Color = Color.Black,
    strokeWidth: Dp = 4.dp,
    backgroundColor: Color = Color.White
) {
    val density = LocalDensity.current
    val paint = remember(color, strokeWidth, density) {
        Paint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            this.strokeWidth = with(density) { strokeWidth.toPx() }
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    SkySignatureView(
        modifier = modifier,
        state = state,
        paint = paint,
        backgroundColor = backgroundColor
    )
}

/**
 * 签名板 Composable，允许直接传入自定义 [Paint]。
 *
 * @param modifier 外层修饰符。
 * @param state 签名状态。
 * @param paint 自定义 Android Paint，可配置抗锯齿、笔触帽、混合模式等。
 * @param backgroundColor 签名板背景色，默认白色。
 */
@Composable
fun SkySignatureView(
    modifier: Modifier = Modifier,
    state: SkySignatureViewState = rememberSkySignatureViewState(),
    paint: Paint,
    backgroundColor: Color = Color.White
) {
    val drawPaint = remember(paint) { Paint(paint) }
    SideEffect { state.paint = drawPaint }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .defaultMinSize(minHeight = 120.dp)
            .clipToBounds()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                shape = RoundedCornerShape(4.dp)
            )
            .onSizeChanged { state.size = Size(it.width.toFloat(), it.height.toFloat()) }
            .then(modifier)
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.startStroke(offset)
                    },
                    onDrag = { change, _ ->
                        state.addPoint(change.position)
                    },
                    onDragEnd = {
                        state.endStroke()
                    },
                    onDragCancel = {
                        state.endStroke()
                    }
                )
            }
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.fillWith(backgroundColor.toArgb())

            val path = state.buildPath()
            if (!path.isEmpty) {
                nativeCanvas.drawPath(path, drawPaint)
            }
        }
    }
}

/**
 * 创建并记住一个 [SkySignatureViewState]。
 */
@Composable
fun rememberSkySignatureViewState(): SkySignatureViewState = remember {
    SkySignatureViewState()
}

/**
 * 签名板状态，持有笔迹数据并提供 [clear] / [save] 能力。
 *
 * 通过 [rememberSkySignatureViewState] 创建，避免在 Composable 中直接实例化。
 */
@Stable
class SkySignatureViewState internal constructor() {
    internal var size: Size by mutableStateOf(Size.Zero)
    internal var paint: Paint by mutableStateOf(Paint())

    internal val strokes: SnapshotStateList<List<Offset>> = mutableStateListOf()
    private var currentStroke: MutableList<Offset>? = null

    /**
     * 当前是否没有任何笔迹。
     */
    val isEmpty: Boolean
        get() = strokes.isEmpty()

    internal fun startStroke(offset: Offset) {
        currentStroke = mutableListOf(offset)
        strokes.add(currentStroke!!.toList())
    }

    internal fun addPoint(offset: Offset) {
        val stroke = currentStroke ?: return
        stroke.add(offset)
        strokes[strokes.lastIndex] = stroke.toList()
    }

    internal fun endStroke() {
        currentStroke = null
    }

    /**
     * 清空所有笔迹。
     */
    fun clear() {
        currentStroke = null
        strokes.clear()
    }

    /**
     * 将当前签名保存为 Bitmap。
     *
     * @param backgroundColor 输出 Bitmap 的背景色
     * @return 签名的 ARGB_8888 Bitmap
     */
    fun save(backgroundColor: Color = Color.White): Bitmap {
        val width = size.width.toInt().coerceAtLeast(1)
        val height = size.height.toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.fillWith(backgroundColor.toArgb())
        val path = buildPath()
        if (!path.isEmpty) {
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    internal fun buildPath(): Path {
        val path = Path()
        strokes.forEach { stroke ->
            if (stroke.isEmpty()) return@forEach
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
        }
        return path
    }
}

/**
 * 用指定颜色填充整块画布。
 *
 * [Canvas.drawColor] 的单参重载自 API 29 起过时，新系统改用 [BlendMode] 版本，
 * 低版本回退旧实现，避免直接调用过时方法。
 */
private fun Canvas.fillWith(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        drawColor(color, BlendMode.SRC_OVER)
    } else {
        @Suppress("DEPRECATION")
        drawColor(color)
    }
}
