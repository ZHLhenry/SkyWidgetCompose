package com.sky.widget.marqueeView

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 跑马灯 / 轮播视图滚动方向。
 */
enum class SkyMarqueeDirection {
    /** 从右往左水平滚动 */
    LEFT,

    /** 从左往右水平滚动 */
    RIGHT,

    /** 从下往上垂直滚动 */
    UP,

    /** 从上往下垂直滚动 */
    DOWN
}

/**
 * [SkyMarqueeView] 的状态控制器。
 *
 * @param initialFlipping 初始是否自动轮播。
 * @param initialFlipInterval 初始切换间隔，单位毫秒。
 */
@Stable
class SkyMarqueeState(
    initialFlipping: Boolean = true,
    initialFlipInterval: Long = 3000L
) {
    /** 当前是否正在自动轮播。 */
    var isFlipping by mutableStateOf(initialFlipping)
        private set

    /** 自动轮播切换间隔，单位毫秒。 */
    var flipInterval by mutableLongStateOf(initialFlipInterval)

    /** 当前展示项索引。 */
    var currentIndex by mutableIntStateOf(0)
        private set

    /** 开始自动轮播。 */
    fun startFlipping() {
        isFlipping = true
    }

    /** 停止自动轮播。 */
    fun stopFlipping() {
        isFlipping = false
    }

    /** 重置到第一项。 */
    fun reset() {
        currentIndex = 0
    }

    /** 切换到下一项。 */
    fun next(itemCount: Int) {
        if (itemCount <= 0) return
        currentIndex = (currentIndex + 1).coerceAtMost(itemCount - 1)
    }

    /** 切换到上一项。 */
    fun previous() {
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
    }

    /** 切换自动轮播状态。 */
    fun toggleFlipping() {
        isFlipping = !isFlipping
    }

    /** 切换到指定索引。 */
    fun setCurrentIndex(index: Int, itemCount: Int) {
        if (itemCount <= 0) return
        currentIndex = index.coerceIn(0, itemCount - 1)
    }

    internal fun advance(itemCount: Int) {
        if (itemCount <= 0) return
        currentIndex = (currentIndex + 1) % itemCount
    }
}

/**
 * 创建并记住一个 [SkyMarqueeState]。
 *
 * @param initialFlipping 初始是否自动轮播。
 * @param initialFlipInterval 初始切换间隔，单位毫秒。
 */
@Composable
fun rememberSkyMarqueeState(
    initialFlipping: Boolean = true,
    initialFlipInterval: Long = 3000L
): SkyMarqueeState = remember {
    SkyMarqueeState(initialFlipping, initialFlipInterval)
}

/**
 * Compose 跑马灯 / 轮播视图。
 *
 * 特性：
 * - 支持上下左右四种切入方向。
 * - 支持通过 [SkyMarqueeState] 开始、停止、重置、切换项。
 * - 当 [onItemClick] 为 null 时，点击内容会暂停 / 继续自动轮播；
 *   当 [onItemClick] 不为 null 时，点击会派发点击事件，不自动暂停。
 * - 只有一条数据时不轮播，但仍展示内容。
 *
 * @param items 轮播数据列表。
 * @param modifier 修饰符。
 * @param state 轮播状态控制器。
 * @param direction 切换动画方向。
 * @param onItemClick 点击回调，参数为当前索引与对应数据。
 * @param itemContent 每一项的 Composable 内容。
 */
@Composable
fun <T> SkyMarqueeView(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: SkyMarqueeState = rememberSkyMarqueeState(),
    direction: SkyMarqueeDirection = SkyMarqueeDirection.LEFT,
    onItemClick: ((Int, T) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    LaunchedEffect(state.isFlipping, state.flipInterval, items.size) {
        while (state.isFlipping && items.size > 1) {
            delay(state.flipInterval.milliseconds)
            state.advance(items.size)
        }
    }

    val currentItem = items[state.currentIndex.coerceIn(0, items.lastIndex)]

    val clickModifier = if (onItemClick != null) {
        Modifier.clickable {
            onItemClick(state.currentIndex, currentItem)
        }
    } else {
        Modifier.clickable {
            state.toggleFlipping()
        }
    }

    Box(modifier = modifier.then(clickModifier)) {
        AnimatedContent(
            targetState = state.currentIndex,
            transitionSpec = {
                when (direction) {
                    SkyMarqueeDirection.LEFT ->
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }

                    SkyMarqueeDirection.RIGHT ->
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }

                    SkyMarqueeDirection.UP ->
                        slideInVertically { it } togetherWith slideOutVertically { -it }

                    SkyMarqueeDirection.DOWN ->
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                }
            },
            label = "SkyMarqueeView"
        ) { index ->
            itemContent(items[index.coerceIn(0, items.lastIndex)])
        }
    }
}
