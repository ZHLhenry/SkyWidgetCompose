package com.sky.widget.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 侧滑菜单：内容层可水平拖拽滑出，露出底层的背景菜单（如删除 / 置顶等操作按钮）。
 *
 * 实现原理：
 * - 用 [Animatable] 维护“打开进度”（0 = 完全关闭，maxOffset = 完全打开，maxOffset 即背景菜单宽度）；
 * - 手势经 foundation 的 [draggable] 捕获，增量按 [direction] 换算成进度并钳制在 [0, maxOffset]；
 * - 松手时把惯性速度折算成一段预测位移，再与 [threshold] 比例比较，决定吸附到打开或关闭；
 * - 内容层与背景层均通过 [Modifier.offset] 平移，避免拖拽过程触发重新布局。
 *
 * @param modifier 作用于侧滑容器根节点的修饰符。
 * @param state 侧滑状态控制器，见 [rememberSwipeState]，用于代码开合、查询打开状态。
 * @param threshold 打开判定阈值比例，范围 (0, 1)；松手时进度超过 maxOffset * threshold 即吸附为打开，默认 0.3。
 * @param direction 背景菜单出现的方向：[SwipeDirection.RightToLeft] 表示菜单在右侧（内容向左滑出），默认。
 * @param onChange 打开状态变化回调，参数为当前是否打开；首次组合也会回调一次。
 * @param background 背景菜单内容（操作按钮等），其宽度决定完全打开时内容层让出的距离；高度自动跟随内容层。
 * @param content 前景内容（列表行等），宽度铺满、高度由内容自身决定（即容器高度），并随进度整体平移。
 */
@Composable
fun SkySwipeMenu(
    modifier: Modifier = Modifier,
    state: SwipeState = rememberSwipeState(),
    threshold: Float = 0.3f,
    direction: SwipeDirection = SwipeDirection.RightToLeft,
    onChange: ((open: Boolean) -> Unit)? = null,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    // 容器宽度（px）：用于计算背景层在关闭态的隐藏位置。
    var boxWidthPx by remember { mutableIntStateOf(0) }
    // 背景菜单宽度（px）：即完全打开时内容层需要让出的最大位移。
    var backgroundWidthPx by remember { mutableIntStateOf(0) }
    // 内容层实测高度（px）：容器高度由内容决定，背景层高度跟随它，
    // 避免在 verticalScroll / LazyColumn 等高度无界约束下 fillMaxHeight 取到无穷大。
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 背景测量完成后同步最大可滑距离到 state，供 open()/close() 与拖拽钳制使用。
    LaunchedEffect(backgroundWidthPx) {
        state.maxOffsetPx = backgroundWidthPx.toFloat()
        // 首次测量到有效宽度时按 initialValue 直接定位（无动画），保证初值 / 状态恢复正确。
        if (!state.initialApplied && backgroundWidthPx > 0) {
            state.initialApplied = true
            state.setOffset(if (state.initialValue == SwipeValue.Open) backgroundWidthPx.toFloat() else 0f)
        }
    }

    // 打开状态变化时回调消费者（含首次组合）。
    LaunchedEffect(state.isOpen) { onChange?.invoke(state.isOpen) }

    // 水平拖拽：把增量按方向换算成“打开进度”并钳制在 [0, maxOffset]。
    val draggableState = rememberDraggableState { delta ->
        // 菜单在右侧时向左滑（delta 为负）为打开；菜单在左侧时相反。
        val signed = if (direction == SwipeDirection.RightToLeft) -delta else delta
        scope.launch { state.setOffset(state.offsetAnim.value + signed) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { boxWidthPx = it.width }
            // 裁剪越界部分，保证关闭态背景完全不可见。
            .clipToBounds()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                // 松手时结合惯性速度预测落点：速度折算成一段额外位移后与阈值比较，手感更跟手。
                onDragStopped = { velocity ->
                    val signedVelocity =
                        if (direction == SwipeDirection.RightToLeft) -velocity else velocity
                    val max = state.maxOffsetPx
                    val predicted =
                        (state.offsetAnim.value + signedVelocity * 0.05f).coerceIn(0f, max)
                    scope.launch { state.settle(predicted > max * threshold) }
                },
            )
    ) {
        // 打开进度（px）：0 = 完全关闭，maxOffset = 完全打开。
        val progress = state.offsetAnim.value
        // 内容层位移：菜单在右侧时向左让出，菜单在左侧时向右让出。
        val contentOffsetX = if (direction == SwipeDirection.RightToLeft) -progress else progress
        // 背景层基准位置：关闭态隐藏在容器外侧（右或左），打开态随进度滑入。
        val backgroundBaseX =
            if (direction == SwipeDirection.RightToLeft) boxWidthPx.toFloat() else -backgroundWidthPx.toFloat()

        // 内容层：宽度铺满、高度由内容自身决定（即容器高度），并随进度整体平移。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { contentHeightPx = it.height }
                .offset { IntOffset(x = contentOffsetX.roundToInt(), y = 0) }
        ) { content() }

        // 背景菜单层：高度跟随内容层实测高度、宽度由菜单内容决定；
        // 位置 = 基准 + 内容位移，实现“露出”效果。
        Box(
            modifier = Modifier
                .height(with(density) { contentHeightPx.toDp() })
                .onSizeChanged { backgroundWidthPx = it.width }
                .offset { IntOffset(x = (backgroundBaseX + contentOffsetX).roundToInt(), y = 0) }
        ) { background() }
    }
}

/**
 * 背景菜单出现的方向。
 */
enum class SwipeDirection {
    /** 菜单在左侧：内容向右滑出露出菜单。 */
    LeftToRight,

    /** 菜单在右侧：内容向左滑出露出菜单（默认）。 */
    RightToLeft,
}

/**
 * 侧滑的开合状态。
 */
enum class SwipeValue {
    /** 关闭（背景菜单隐藏）。 */
    Hidden,

    /** 打开（背景菜单完全露出）。 */
    Open,
}

/**
 * 记住一个 [SwipeState]，配置变化 / 进程恢复时保持打开状态。
 *
 * @param initialValue 初始开合状态，默认 [SwipeValue.Hidden]。
 */
@Composable
fun rememberSwipeState(
    initialValue: SwipeValue = SwipeValue.Hidden,
): SwipeState = rememberSaveable(saver = SwipeState.SAVER) {
    SwipeState(initialValue = initialValue)
}

/**
 * 侧滑状态控制器：持有打开进度动画与开合标志，供代码开合与状态查询。
 */
@Stable
class SwipeState internal constructor(
    val initialValue: SwipeValue,
) {
    /** 当前打开进度动画值（px）：0 = 关闭，[maxOffsetPx] = 完全打开。 */
    internal val offsetAnim = Animatable(0f)

    /** 最大可滑距离（px），由 SkySwipeMenu 测量背景宽度后写入。 */
    internal var maxOffsetPx by mutableFloatStateOf(0f)

    /** 是否已按 [initialValue] 完成首次定位，避免重复 snap。 */
    internal var initialApplied = false

    private val _open = mutableStateOf(initialValue == SwipeValue.Open)

    /** 当前是否处于打开状态（响应式）。 */
    val isOpen: Boolean get() = _open.value

    /** 当前开合状态枚举（响应式）。 */
    val currentValue: SwipeValue get() = if (_open.value) SwipeValue.Open else SwipeValue.Hidden

    /** 无动画把进度设置到 [value]（自动钳制到 [0, maxOffsetPx]）。 */
    internal suspend fun setOffset(value: Float) {
        offsetAnim.snapTo(value.coerceIn(0f, maxOffsetPx))
    }

    /** 以动画吸附到打开 / 关闭，并同步开合标志。 */
    internal suspend fun settle(open: Boolean) {
        offsetAnim.animateTo(if (open) maxOffsetPx else 0f)
        _open.value = open
    }

    /** 打开菜单（带动画）。 */
    suspend fun open() {
        settle(true)
    }

    /** 关闭菜单（带动画）。 */
    suspend fun close() {
        settle(false)
    }

    companion object {
        /** 状态保存器：仅保存开合标志，恢复时作为初始值。 */
        val SAVER: Saver<SwipeState, *> = Saver(
            save = { it.isOpen },
            restore = { SwipeState(if (it) SwipeValue.Open else SwipeValue.Hidden) },
        )
    }
}
