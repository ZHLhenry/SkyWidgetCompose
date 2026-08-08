package com.sky.widget.badge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Badge 对齐方位。
 */
enum class SkyBadgeGravity {
    TOP_START, TOP_CENTER, TOP_END,
    CENTER_START, CENTER, CENTER_END,
    BOTTOM_START, BOTTOM_CENTER, BOTTOM_END
}

/**
 * 拖拽状态常量。
 */
object SkyBadgeDragState {
    const val START = 1
    const val DRAGGING = 2
    const val DRAGGING_OUT_OF_RANGE = 3
    const val CANCELED = 4
    const val SUCCEED = 5
}

/**
 * [SkyBadgeBox] / [SkyBadgeView] 的状态控制器。
 *
 * @param initialNumber 初始数字；0 时隐藏，负数时显示圆点。
 * @param initialText 初始文本；优先级高于数字。
 * @param initialVisible 初始是否可见。
 * @param maxNumber 超过该数字后显示为 maxNumber+，例如 99+。
 * @param circleShapeThreshold 文本位数小于等于该值时按圆形绘制。
 * @param roundedRadiusPercent 圆角矩形状态下的圆角百分比（0~99）。
 * @param showBadgeThreshold 显示徽章的最小计数阈值；数字小于等于该值时隐藏。
 */
@Stable
class SkyBadgeState(
    initialNumber: Int = 0,
    initialText: String? = null,
    initialVisible: Boolean = true,
    var maxNumber: Int = 99,
    var circleShapeThreshold: Int = 2,
    var roundedRadiusPercent: Int = 50,
    var showBadgeThreshold: Int = 0
) {
    /** 当前是否可见。 */
    var isVisible by mutableStateOf(initialVisible)

    /** 徽章数字；0 时隐藏，负数时显示圆点。 */
    var number by mutableIntStateOf(initialNumber)

    /** 徽章文本；优先级高于 [number]。 */
    var text by mutableStateOf(initialText)

    /** 超过 [maxNumber] 时是否精确显示，false 则显示 "maxNumber+"。 */
    var isExact by mutableStateOf(false)

    internal var dragOffsetX by mutableFloatStateOf(0f)
    internal var dragOffsetY by mutableFloatStateOf(0f)
    internal var isDragging by mutableStateOf(false)
    internal var isOutOfRange by mutableStateOf(false)
    internal var badgeWidth by mutableFloatStateOf(0f)
    internal var badgeHeight by mutableFloatStateOf(0f)

    /** 当前应显示的文本；null 表示不显示。 */
    val displayText: String?
        get() = when {
            !text.isNullOrEmpty() -> text
            number < 0 -> ""
            number <= showBadgeThreshold -> null
            number > maxNumber && !isExact -> "$maxNumber+"
            else -> number.toString()
        }

    /** 当前是否为圆形徽章。 */
    val isCircleShape: Boolean
        get() {
            val display = displayText ?: return false
            val isDot = display.isEmpty()
            val isNumber = text.isNullOrEmpty() && number > 0
            return isDot || (isNumber && display.length <= circleShapeThreshold)
        }

    /** 隐藏徽章。 */
    fun hide() {
        isVisible = false
    }

    /** 显示徽章。 */
    fun show() {
        isVisible = true
    }

    /** 切换可见性。 */
    fun toggle() {
        isVisible = !isVisible
    }

    /** 设置数字。 */
    fun setBadgeNumber(n: Int) {
        number = n
    }

    /** 设置文本。 */
    fun setBadgeText(t: String?) {
        text = t
    }

    /** 重置为指定数字并显示；同时清空拖拽偏移与拖拽状态。 */
    fun reset(n: Int = number) {
        number = n
        isVisible = true
        dragOffsetX = 0f
        dragOffsetY = 0f
        isDragging = false
        isOutOfRange = false
    }

    override fun toString(): String {
        return "SkyBadgeState(number=$number, text=$text, isVisible=$isVisible, " +
                "maxNumber=$maxNumber, circleShapeThreshold=$circleShapeThreshold, " +
                "showBadgeThreshold=$showBadgeThreshold, isExact=$isExact)"
    }
}

/**
 * 创建并记住一个 [SkyBadgeState]。
 *
 * @param initialNumber 初始数字；0 时隐藏，负数时显示圆点。
 * @param initialText 初始文本；优先级高于数字。
 * @param initialVisible 初始是否可见。
 * @param maxNumber 超过该数字后显示为 maxNumber+，例如 99+。
 * @param circleShapeThreshold 文本位数小于等于该值时按圆形绘制。
 * @param roundedRadiusPercent 圆角矩形状态下的圆角百分比（0~99）。
 * @param showBadgeThreshold 显示徽章的最小计数阈值；数字小于等于该值时隐藏。
 */
@Composable
fun rememberSkyBadgeState(
    initialNumber: Int = 0,
    initialText: String? = null,
    initialVisible: Boolean = true,
    maxNumber: Int = 99,
    circleShapeThreshold: Int = 2,
    roundedRadiusPercent: Int = 50,
    showBadgeThreshold: Int = 0
): SkyBadgeState = remember {
    SkyBadgeState(
        initialNumber = initialNumber,
        initialText = initialText,
        initialVisible = initialVisible,
        maxNumber = maxNumber,
        circleShapeThreshold = circleShapeThreshold,
        roundedRadiusPercent = roundedRadiusPercent,
        showBadgeThreshold = showBadgeThreshold
    )
}

/**
 * 容器式徽章组件，将徽章叠加在 [content] 之上。
 *
 * 组件尺寸**包裹 [content]**，badge 基于 content 的实际尺寸定位，可溢出 content 边界。
 * 当 [draggable] 为 `true` 时，拖拽手势监听挂载在 badge 上，拖拽绘制层（连接线、爆炸动画）
 * 与 content 共用同一坐标系，避免绘制位置随外部容器尺寸发生偏移。
 *
 * @param modifier 外层修饰符。
 * @param state 徽章状态。
 * @param gravity 徽章相对 content 的方位。
 * @param offset 额外偏移。
 * @param backgroundColor 徽章背景色。
 * @param textColor 徽章文字颜色。
 * @param textSize 徽章文字大小。
 * @param horizontalPadding 圆角矩形状态下的水平内边距；也用于圆点/圆形的水平内边距。
 * @param verticalPadding 圆角矩形状态下的垂直内边距；也用于圆点/圆形的垂直内边距。
 * @param borderColor 边框颜色。
 * @param borderWidth 边框宽度。
 * @param showShadow 是否显示阴影。
 * @param draggable 是否支持长按 badge 拖拽消除。
 * @param maxDragDistance 拖拽判定消失的最大距离。
 * @param onDragStateChanged 拖拽状态回调，状态值见 [SkyBadgeDragState]。
 * @param badge 自定义徽章内容；默认使用 [DefaultSkyBadgeContent]。
 * @param content 被叠加的主体内容。
 */
@Composable
fun SkyBadgeBox(
    modifier: Modifier = Modifier,
    state: SkyBadgeState = rememberSkyBadgeState(),
    gravity: SkyBadgeGravity = SkyBadgeGravity.TOP_END,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    backgroundColor: Color = Color.Red,
    textColor: Color = Color.White,
    textSize: TextUnit = 11.sp,
    horizontalPadding: Dp = 4.dp,
    verticalPadding: Dp = 0.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    showShadow: Boolean = true,
    draggable: Boolean = false,
    maxDragDistance: Dp = 90.dp,
    onDragStateChanged: ((Int) -> Unit)? = null,
    badge: @Composable BoxScope.() -> Unit = {
        DefaultSkyBadgeContent(
            state = state,
            backgroundColor = backgroundColor,
            textColor = textColor,
            textSize = textSize,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            borderColor = borderColor,
            borderWidth = borderWidth,
            showShadow = showShadow
        )
    },
    content: @Composable () -> Unit
) {
    val displayText = state.displayText
    if (displayText == null || !state.isVisible) {
        Box(modifier = modifier) { content() }
        return
    }

    val density = LocalDensity.current
    val maxDragPx = with(density) { maxDragDistance.toPx() }
    val offsetPx = with(density) {
        IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Layout(
            content = {
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) { content() }
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .then(
                            if (draggable) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            state.isDragging = true
                                            state.dragOffsetX = 0f
                                            state.dragOffsetY = 0f
                                            onDragStateChanged?.invoke(SkyBadgeDragState.START)
                                        },
                                        onDragEnd = {
                                            state.isDragging = false
                                            if (state.isOutOfRange) {
                                                state.hide()
                                                onDragStateChanged?.invoke(SkyBadgeDragState.SUCCEED)
                                            } else {
                                                state.dragOffsetX = 0f
                                                state.dragOffsetY = 0f
                                                state.isOutOfRange = false
                                                onDragStateChanged?.invoke(SkyBadgeDragState.CANCELED)
                                            }
                                        },
                                        onDragCancel = {
                                            state.isDragging = false
                                            state.dragOffsetX = 0f
                                            state.dragOffsetY = 0f
                                            state.isOutOfRange = false
                                            onDragStateChanged?.invoke(SkyBadgeDragState.CANCELED)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            state.dragOffsetX += dragAmount.x
                                            state.dragOffsetY += dragAmount.y

                                            val distance = sqrt(
                                                state.dragOffsetX * state.dragOffsetX +
                                                    state.dragOffsetY * state.dragOffsetY
                                            )
                                            state.isOutOfRange = distance >= maxDragPx

                                            onDragStateChanged?.invoke(
                                                if (state.isOutOfRange) SkyBadgeDragState.DRAGGING_OUT_OF_RANGE
                                                else SkyBadgeDragState.DRAGGING
                                            )
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) { badge() }
                if (draggable) {
                    BadgeDragCanvas(
                        state = state,
                        maxDragPx = maxDragPx,
                        badgeGravity = gravity,
                        offsetPx = offsetPx,
                        backgroundColor = backgroundColor
                    )
                }
            }
        ) { measurables, constraints ->
            val expectedCount = if (draggable) 3 else 2
            require(measurables.size == expectedCount) {
                "SkyBadgeBox must have exactly $expectedCount children"
            }

            val contentPlaceable = measurables[0].measure(constraints)
            val badgeMeasurable = measurables[1]

            val badgeConstraints = Constraints(
                maxWidth = contentPlaceable.width,
                maxHeight = contentPlaceable.height
            )
            val badgePlaceable = badgeMeasurable.measure(badgeConstraints)

            val width = contentPlaceable.width
            val height = contentPlaceable.height

            state.badgeWidth = badgePlaceable.width.toFloat()
            state.badgeHeight = badgePlaceable.height.toFloat()

            val badgeX = calculateBadgeX(gravity, width, badgePlaceable.width, offsetPx.x)
            val badgeY = calculateBadgeY(gravity, height, badgePlaceable.height, offsetPx.y)

            val canvasPlaceable = if (draggable) {
                measurables[2].measure(Constraints.fixed(width, height))
            } else null

            layout(width, height) {
                contentPlaceable.placeRelative(0, 0)

                val dragX = state.dragOffsetX.roundToInt()
                val dragY = state.dragOffsetY.roundToInt()
                badgePlaceable.placeRelative(badgeX + dragX, badgeY + dragY)

                canvasPlaceable?.placeRelative(0, 0)
            }
        }
    }
}

/**
 * 独立的徽章视图，不依赖外部 content，可直接作为普通 Composable 使用。
 *
 * @param modifier 外层修饰符。
 * @param state 徽章状态。
 * @param backgroundColor 徽章背景色。
 * @param textColor 徽章文字颜色。
 * @param textSize 徽章文字大小。
 * @param horizontalPadding 水平内边距。
 * @param verticalPadding 垂直内边距。
 * @param borderColor 边框颜色。
 * @param borderWidth 边框宽度。
 * @param showShadow 是否显示阴影。
 */
@Composable
fun SkyBadgeView(
    modifier: Modifier = Modifier,
    state: SkyBadgeState = rememberSkyBadgeState(),
    backgroundColor: Color = Color.Red,
    textColor: Color = Color.White,
    textSize: TextUnit = 11.sp,
    horizontalPadding: Dp = 4.dp,
    verticalPadding: Dp = 0.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    showShadow: Boolean = true
) {
    DefaultSkyBadgeContent(
        state = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        textColor = textColor,
        textSize = textSize,
        horizontalPadding = horizontalPadding,
        verticalPadding = verticalPadding,
        borderColor = borderColor,
        borderWidth = borderWidth,
        showShadow = showShadow
    )
}

/**
 * 默认徽章内容实现。
 * 参考 SmartToolFactory Compose-Badge 的自定义 Layout 测量方式，
 * 根据文字基线精确计算尺寸，避免被截断或变成椭圆。
 */
@Composable
private fun DefaultSkyBadgeContent(
    state: SkyBadgeState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Red,
    textColor: Color = Color.White,
    textSize: TextUnit = 11.sp,
    horizontalPadding: Dp = 4.dp,
    verticalPadding: Dp = 0.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    showShadow: Boolean = true
) {
    val displayText = state.displayText ?: return
    val density = LocalDensity.current

    val isDot = displayText.isEmpty()
    val shape = if (state.isCircleShape) CircleShape else RoundedCornerShape(state.roundedRadiusPercent)

    val badgeModifier = modifier
        .let {
            if (showShadow) it.shadow(
                elevation = 2.dp,
                shape = shape
            ) else it
        }
        .border(
            width = borderWidth,
            color = if (borderColor != Color.Unspecified) borderColor else Color.Transparent,
            shape = shape
        )
        .background(
            color = backgroundColor,
            shape = shape
        )

    if (isDot) {
        val dotSizeDp = with(density) {
            (textSize.value.dp + verticalPadding * 2 + 4.dp).toPx().toDp()
        }
        Box(
            modifier = badgeModifier.size(dotSizeDp),
            contentAlignment = Alignment.Center
        ) {}
        return
    }

    val badgeData = remember { BadgeData() }

    val textComposable: @Composable () -> Unit = {
        Text(
            text = displayText,
            color = textColor,
            fontSize = textSize,
            lineHeight = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            onTextLayout = { result ->
                // 使用 firstBaseline 排除 font padding 的影响，获得更精确的文字高度
                badgeData.textHeight = result.firstBaseline.toInt()
                badgeData.textSize = result.size
            }
        )
    }

    Layout(
        modifier = badgeModifier,
        content = textComposable
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        val textHeight = badgeData.textHeight

        val verticalSpaceAroundText = with(density) {
            textHeight * 0.12f + 6 + verticalPadding.toPx()
        }
        val horizontalSpaceAroundText = with(density) {
            textHeight * 0.12f + 6 + horizontalPadding.toPx()
        }

        if (state.isCircleShape) {
            var badgeHeight = (textHeight + 2 * verticalSpaceAroundText).toInt()
            badgeHeight = placeable.width.coerceAtLeast(badgeHeight)

            layout(badgeHeight, badgeHeight) {
                placeable.placeRelative(
                    (badgeHeight - placeable.width) / 2,
                    (badgeHeight - placeable.height) / 2
                )
            }
        } else {
            val badgeHeight = (textHeight + 2 * verticalSpaceAroundText).toInt()
            val width = (placeable.width + 2 * horizontalSpaceAroundText).toInt()

            layout(width, badgeHeight) {
                placeable.placeRelative(
                    x = (width - placeable.width) / 2,
                    y = (-placeable.height + badgeHeight) / 2
                )
            }
        }
    }
}

/**
 * 用于保存 [DefaultSkyBadgeContent] 测量过程中产生的临时数据。
 */
private data class BadgeData(
    var textHeight: Int = 0,
    var textSize: IntSize? = null
)

/**
 * 拖拽绘制层：仅负责绘制连接线与爆炸动画。
 */
@Composable
private fun BadgeDragCanvas(
    state: SkyBadgeState,
    maxDragPx: Float,
    badgeGravity: SkyBadgeGravity,
    offsetPx: IntOffset,
    backgroundColor: Color
) {
    var size by remember { mutableStateOf(Size.Zero) }
    val explosionProgress = remember { Animatable(0f) }
    var showExplosion by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
    ) {
        val badgeX = if (size == Size.Zero) 0f else
            calculateBadgeX(badgeGravity, size.width.toInt(), state.badgeWidth.toInt(), offsetPx.x).toFloat()
        val badgeY = if (size == Size.Zero) 0f else
            calculateBadgeY(badgeGravity, size.height.toInt(), state.badgeHeight.toInt(), offsetPx.y).toFloat()

        val baseCenterX = badgeX + state.badgeWidth / 2f
        val baseCenterY = badgeY + state.badgeHeight / 2f
        val currentCenterX = baseCenterX + state.dragOffsetX
        val currentCenterY = baseCenterY + state.dragOffsetY

        if (state.isDragging && !state.isOutOfRange) {
            val distance = sqrt(
                state.dragOffsetX * state.dragOffsetX +
                    state.dragOffsetY * state.dragOffsetY
            )
            val startRadius = (state.badgeHeight / 4f * (1 - distance / maxDragPx))
                .coerceAtLeast(4f)
            val endRadius = state.badgeHeight / 2f

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBadgeDraggingConnector(
                    startCenter = Offset(baseCenterX, baseCenterY),
                    endCenter = Offset(currentCenterX, currentCenterY),
                    startRadius = startRadius,
                    endRadius = endRadius,
                    color = backgroundColor
                )
            }
        }

        if (showExplosion) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawExplosion(
                    center = Offset(currentCenterX, currentCenterY),
                    color = backgroundColor,
                    progress = explosionProgress.value
                )
            }
        }
    }

    LaunchedEffect(state.isOutOfRange) {
        if (state.isOutOfRange) {
            showExplosion = true
            explosionProgress.snapTo(0f)
            explosionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
            showExplosion = false
        }
    }
}

private fun calculateBadgeX(
    gravity: SkyBadgeGravity,
    parentWidth: Int,
    badgeWidth: Int,
    offsetX: Int
): Int = when (gravity) {
    SkyBadgeGravity.TOP_START, SkyBadgeGravity.CENTER_START, SkyBadgeGravity.BOTTOM_START ->
        -badgeWidth / 2 + offsetX
    SkyBadgeGravity.TOP_CENTER, SkyBadgeGravity.CENTER, SkyBadgeGravity.BOTTOM_CENTER ->
        (parentWidth - badgeWidth) / 2 + offsetX
    SkyBadgeGravity.TOP_END, SkyBadgeGravity.CENTER_END, SkyBadgeGravity.BOTTOM_END ->
        parentWidth - badgeWidth / 2 + offsetX
}

private fun calculateBadgeY(
    gravity: SkyBadgeGravity,
    parentHeight: Int,
    badgeHeight: Int,
    offsetY: Int
): Int = when (gravity) {
    SkyBadgeGravity.TOP_START, SkyBadgeGravity.TOP_CENTER, SkyBadgeGravity.TOP_END ->
        -badgeHeight / 2 + offsetY
    SkyBadgeGravity.CENTER_START, SkyBadgeGravity.CENTER, SkyBadgeGravity.CENTER_END ->
        (parentHeight - badgeHeight) / 2 + offsetY
    SkyBadgeGravity.BOTTOM_START, SkyBadgeGravity.BOTTOM_CENTER, SkyBadgeGravity.BOTTOM_END ->
        parentHeight - badgeHeight / 2 + offsetY
}

/**
 * 绘制拖拽时的贝塞尔连接线。
 */
private fun DrawScope.drawBadgeDraggingConnector(
    startCenter: Offset,
    endCenter: Offset,
    startRadius: Float,
    endRadius: Float,
    color: Color
) {
    val angle = atan2(endCenter.y - startCenter.y, endCenter.x - startCenter.x)
    val startOffset = Offset(
        cos(angle) * startRadius,
        sin(angle) * startRadius
    )
    val endOffset = Offset(
        cos(angle) * endRadius,
        sin(angle) * endRadius
    )

    val path = Path().apply {
        moveTo(startCenter.x + startOffset.x, startCenter.y + startOffset.y)
        quadraticTo(
            (startCenter.x + endCenter.x) / 2f,
            (startCenter.y + endCenter.y) / 2f,
            endCenter.x + endOffset.x,
            endCenter.y + endOffset.y
        )
        lineTo(endCenter.x - endOffset.x, endCenter.y - endOffset.y)
        quadraticTo(
            (startCenter.x + endCenter.x) / 2f,
            (startCenter.y + endCenter.y) / 2f,
            startCenter.x - startOffset.x,
            startCenter.y - startOffset.y
        )
        close()
    }

    drawPath(path = path, color = color)

    drawCircle(
        color = color,
        radius = startRadius,
        center = startCenter
    )
    drawCircle(
        color = color,
        radius = endRadius,
        center = endCenter
    )
}

/**
 * 绘制爆炸粒子动画。
 */
private fun DrawScope.drawExplosion(
    center: Offset,
    color: Color,
    progress: Float
) {
    val particleCount = 12
    val maxRadius = 60f
    val baseRadius = 4f

    for (i in 0 until particleCount) {
        val angle = 2 * PI * i / particleCount
        val distance = progress * maxRadius
        val particleCenter = Offset(
            center.x + cos(angle).toFloat() * distance,
            center.y + sin(angle).toFloat() * distance
        )
        val radius = baseRadius * (1 - progress)
        if (radius > 0) {
            drawCircle(
                color = color.copy(alpha = 1 - progress),
                radius = radius,
                center = particleCenter
            )
        }
    }
}
