package com.sky.widget.chainScrollLayout.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.sky.widget.chainScrollLayout.SkyChainScrollMode
import com.sky.widget.chainScrollLayout.SkyChainScrollPosition
import com.sky.widget.chainScrollLayout.SkyChainScrollState

/**
 * 把 [value] 钳制到 [[min], [max]] 闭区间内，等价 `coerceIn`，
 * 独立函数便于统一表达"取中间值"的语义（与参考实现命名保持一致）。
 */
internal fun midOf(min: Float, value: Float, max: Float): Float = when {
    value < min -> min
    value > max -> max
    else -> value
}

/**
 * 在当前累积位置叠加 [delta] 并无动画设置（自动钳制到 `[[SkyChainScrollState.minPx], [SkyChainScrollState.maxPx]]`）。
 *
 * 三个连接器都反复执行「取当前位置 → 叠加位移 → 钳制 → setScrollPosition」，
 * 抽取为扩展函数统一这一关键逻辑，避免各分支重复钳制样板、遗漏边界处理。
 */
private fun SkyChainScrollState.scrollBy(delta: Float) {
    setScrollPosition(midOf(minPx, getScrollPositionValue() + delta, maxPx))
}

/** 在当前累积位置叠加 [delta] 并以动画设置（自动钳制）；用于 fling 惯性收尾。 */
private suspend fun SkyChainScrollState.animateBy(delta: Float) {
    animateToScrollPosition(midOf(minPx, getScrollPositionValue() + delta, maxPx))
}

/**
 * [SkyChainScrollMode.ChainContentFirst] 对应的 [NestedScrollConnection]。
 *
 * 联动条优先：手势能量先驱动联动条，联动条到达边界后剩余位移再交给内容区域。
 *
 * 实现要点：
 * - 使用 [onPostScroll] 的 `consumed`（内容已消费位移）作为联动条的驱动量，
 *   让联动条与内容保持相对位移同步；
 * - [onPostFling] 只回调 `onScrollStop`，不再叠加惯性位移，
 *   避免联动条越过边界后仍继续动画。
 */
internal class SkyChainContentFirstConnection(
    private val state: SkyChainScrollState,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val delta = if (state.orientationIsHorizontal) consumed.x else consumed.y
        state.scrollBy(delta)
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val delta = if (state.orientationIsHorizontal) consumed.x else consumed.y
        state.callOnScrollStop(delta)
        return super.onPostFling(consumed, available)
    }
}

/**
 * [SkyChainScrollMode.ChainAfterContent] 对应的 [NestedScrollConnection]。
 *
 * 内容区域完全无法滚动后，才把溢出位移交给联动条。
 *
 * 实现要点：
 * - 使用 [onPostScroll] 的 `available`（内容未消费的剩余位移）作为联动条的驱动量，
 *   因此只有内容真正到达边界时才会推动联动条；
 * - [onPostFling] 中若 `onScrollStop` 未拦截，则用剩余惯性速度做一次动画收尾。
 */
internal class SkyChainAfterContentConnection(
    private val state: SkyChainScrollState,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val delta = if (state.orientationIsHorizontal) available.x else available.y
        state.scrollBy(delta)
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val delta = if (state.orientationIsHorizontal) available.x else available.y
        // 消费者拦截则不再叠加惯性动画
        if (state.callOnScrollStop(delta)) return super.onPostFling(consumed, available)
        state.animateBy(delta)
        return super.onPostFling(consumed, available)
    }
}

/**
 * [SkyChainScrollMode.ContentFirst] 对应的 [NestedScrollConnection]。
 *
 * 内容区域优先：内容先滚，滚到边界后剩余位移再驱动联动条。
 *
 * 实现要点：
 * - 仅支持 [SkyChainScrollPosition.Top] / [SkyChainScrollPosition.Start]，与其他位置组合会在初始化时抛异常；
 * - `available.y < 0`（向上溢出，内容已到顶）时联动条同步收起；
 * - `consumed.y > 0`（内容正在向下滚动）时，用 [scrollSum] 追踪累积滚动量，
 *   仅当累积量位于 [[SkyChainScrollState.minPx], [SkyChainScrollState.maxPx]] 之间时才让联动条跟随展开，
 *   避免内容已经滚到中部时联动条被误展开。
 */
internal class SkyContentFirstConnection(
    private val state: SkyChainScrollState,
) : NestedScrollConnection {

    init {
        if (state.position == SkyChainScrollPosition.End || state.position == SkyChainScrollPosition.Bottom) {
            throw IllegalStateException(
                "[SkyChainScrollMode.ContentFirst] 不支持 [SkyChainScrollPosition.Bottom] 与 [SkyChainScrollPosition.End]"
            )
        }
    }

    /** 内容区域累积滚动量（px），用于判断当前是否处于"接近顶部"的展开窗口。 */
    private var scrollSum = 0f

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val delta = if (state.orientationIsHorizontal) available.x else available.y
        // 内容先滚完，顶部溢出（delta < 0）时联动条跟着收起
        if (delta < 0f) state.scrollBy(delta)
        val delta2 = if (state.orientationIsHorizontal) consumed.x else consumed.y
        scrollSum += delta2
        // 顶部：累积滚动量位于 [minPx, maxPx] 之间时，联动条跟随内容一起向下展开
        if (delta2 > 0f && scrollSum < state.maxPx && scrollSum > state.minPx) {
            state.scrollBy(delta2)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val delta = if (state.orientationIsHorizontal) available.x else available.y
        if (state.callOnScrollStop(delta)) return super.onPostFling(consumed, available)
        // 仅对向上溢出（收起方向）做惯性收尾；向下展开的惯性由内容自身消化
        if (delta < 0f) state.animateBy(delta)
        return super.onPostFling(consumed, available)
    }
}
