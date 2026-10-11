package com.sky.widget.chainScrollLayout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * [SkyChainScrollLayout] 的状态控制器。
 *
 * 内部使用 [Animatable] 维护联动条的滚动位置（单位 px，含符号），
 * 数值范围被钳制在 [[minPx], [maxPx]]：
 * - [SkyChainScrollPosition.Top] / [Start]：`minPx = minScrollPosition - maxScrollPosition`（负值），`maxPx = 0`
 *   （初始位置 0 = 完全展开，滑动到 minPx = 完全隐藏）
 * - [SkyChainScrollPosition.Bottom] / [End]：`minPx = 0`，`maxPx = maxScrollPosition - minScrollPosition`（正值）
 *   （初始位置 maxPx = 完全展开，滑动到 0 = 完全隐藏）
 *
 * 通常由 [rememberSkyChainScrollState] 创建并跨重组持有实例；消费者可：
 * - 通过 [getScrollPositionValue] / [getScrollPositionPercentage] 读取当前位置驱动 UI（平移、透明度等）；
 * - 通过 [setScrollPosition] / [setScrollPositionWithAnimate] / [animateToScrollPosition] 代码控制滚动。
 */
@Stable
class SkyChainScrollState internal constructor(
    /** 联动条允许的滚动位置最小值（px，含符号），方向由 [position] 决定；可用于代码滚动到该边界。 */
    val minPx: Float,
    /** 联动条允许的滚动位置最大值（px，含符号），方向由 [position] 决定；可用于代码滚动到该边界。 */
    val maxPx: Float,
    /** 联动条所在容器位置，同时决定滑动方向与初始位置。 */
    internal val position: SkyChainScrollPosition,
    /** 内部协程作用域，用于 [setScrollPosition] 等非挂起 API 启动动画 / snap。 */
    internal val coroutineScope: CoroutineScope,
    /** 停止滚动时的回调；返回 true 会拦截后续 fling 联动动画。 */
    private val onScrollStop: ((SkyChainScrollState, Float) -> Boolean)?,
) {
    /** 是否为水平方向（[SkyChainScrollPosition.Start] / [End]）。 */
    val orientationIsHorizontal: Boolean = position.isHorizontal()

    /**
     * 联动条当前位置动画对象；初始位置：
     * - [SkyChainScrollPosition.Top] / [Start]：0（完全展开）
     * - [SkyChainScrollPosition.Bottom] / [End]：[maxPx]（完全展开）
     */
    internal val scrollPosition: Animatable<Float, AnimationVector1D> = Animatable(
        if (position == SkyChainScrollPosition.Bottom || position == SkyChainScrollPosition.End) maxPx else 0f
    )

    /** 获取当前滚动位置（px，含符号）；可直接用于 [androidx.compose.ui.Modifier.offset] 等。 */
    fun getScrollPositionValue(): Float = scrollPosition.value

    /**
     * 获取当前滚动位置占整个滚动范围的比例（`0f` ~ `1f`）。
     *
     * 计算方式为 `abs(当前位置 / (maxPx - minPx))`，即以 0 为基准的归一化绝对偏移：
     * 位置为 0 时返回 `0f`，到达另一侧边界（[minPx] 或 [maxPx]）时返回 `1f`。
     * 当 [minPx] == [maxPx]（无有效滚动范围）时恒为 `0f`，避免除零。
     */
    fun getScrollPositionPercentage(): Float {
        val span = maxPx - minPx
        return if (span == 0f) 0f else abs(scrollPosition.value / span)
    }

    /** 无动画直接把位置设置到 [value]（自动钳制到 [[minPx], [maxPx]]）。 */
    fun setScrollPosition(value: Float) {
        coroutineScope.launch { scrollPosition.snapTo(value.coerceIn(minPx, maxPx)) }
    }

    /** 挂起版本：无动画直接把位置设置到 [value]（自动钳制）。 */
    suspend fun snapToScrollPosition(value: Float) {
        scrollPosition.snapTo(value.coerceIn(minPx, maxPx))
    }

    /** 以动画形式把位置设置到 [value]（自动钳制）。 */
    fun setScrollPositionWithAnimate(value: Float) {
        coroutineScope.launch { scrollPosition.animateTo(value.coerceIn(minPx, maxPx)) }
    }

    /** 挂起版本：以动画形式把位置设置到 [value]（自动钳制）。 */
    suspend fun animateToScrollPosition(value: Float) {
        scrollPosition.animateTo(value.coerceIn(minPx, maxPx))
    }

    /** 调用外部注入的 onScrollStop；返回 true 表示消费者拦截了后续 fling 联动动画。 */
    internal fun callOnScrollStop(delta: Float): Boolean =
        onScrollStop?.invoke(this, delta) == true
}

/**
 * 创建并记住一个 [SkyChainScrollState]，跨重组复用同一实例。
 *
 * 当 [minScrollPosition]、[maxScrollPosition]、[position]、[onScrollStop] 中任一变化时会重建 state，
 * 因此调用方应尽量保持这些参数的稳定引用（避免每次组合都传入新的 lambda 实例）。
 *
 * @param minScrollPosition 最小滚动位置（距离指定方向顶点的距离），通常传 `0.dp` 表示完全展开时的偏移。
 * @param maxScrollPosition 最大滚动位置（距离指定方向顶点的距离），
 *   通常传联动条自身的高度 / 宽度，表示完全隐藏时的偏移；应 >= [minScrollPosition]。
 * @param position 联动条所在容器位置，同时决定滑动方向；默认 [SkyChainScrollPosition.Top]。
 * @param onScrollStop 停止滚动时的回调，参数为当前 state 与本次 fling 的剩余速度；
 *   返回 true 会拦截后续 fling 联动动画（可用于业务层自行接管惯性收尾）；默认 null 不拦截。
 */
@Composable
fun rememberSkyChainScrollState(
    minScrollPosition: Dp,
    maxScrollPosition: Dp,
    position: SkyChainScrollPosition = SkyChainScrollPosition.Top,
    onScrollStop: ((SkyChainScrollState, Float) -> Boolean)? = null,
): SkyChainScrollState {
    val density = LocalDensity.current
    // Dp → px 转换：仅在 Dp 值或 density 变化时重算，避免每帧重复换算
    val minPx = remember(minScrollPosition, density) {
        with(density) { minScrollPosition.roundToPx().toFloat() }
    }
    val maxPx = remember(maxScrollPosition, density) {
        with(density) { maxScrollPosition.roundToPx().toFloat() }
    }
    val scope = rememberCoroutineScope()
    return remember(minPx, maxPx, position, scope, onScrollStop) {
        // 根据 position 决定内部滚动范围的方向：
        // - Top / Start：滚动范围为负值，初始 0（完全展开），最小值为 -(maxScroll - minScroll)
        // - Bottom / End：滚动范围为正值，初始 maxPx（完全展开），最小值为 0
        when (position) {
            SkyChainScrollPosition.Top, SkyChainScrollPosition.Start -> SkyChainScrollState(
                minPx = minPx - maxPx,
                maxPx = 0f,
                position = position,
                coroutineScope = scope,
                onScrollStop = onScrollStop,
            )

            SkyChainScrollPosition.Bottom, SkyChainScrollPosition.End -> SkyChainScrollState(
                minPx = 0f,
                maxPx = maxPx - minPx,
                position = position,
                coroutineScope = scope,
                onScrollStop = onScrollStop,
            )
        }
    }
}
