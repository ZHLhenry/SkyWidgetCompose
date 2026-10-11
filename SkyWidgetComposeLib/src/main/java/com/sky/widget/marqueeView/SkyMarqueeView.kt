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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** 等待内层跑马灯闸门放开时的轮询间隔(毫秒),与 [SkyTextMarquee] 保持一致。 */
private const val MARQUEE_POLL_INTERVAL_MS = 60L

/**
 * 内层跑马灯与外层 [SkyMarqueeView] 之间的「首循环」协调闸门。
 *
 * 当 [SkyMarqueeView] 的 item 内嵌套了 [SkyTextMarquee] 且单行文字溢出需要滚动时，
 * [SkyTextMarquee] 会在开始滚动前 [acquireHold]、把文字完整显示一次（滚到末端并停顿）后 [releaseHold]；
 * [SkyMarqueeView] 的轮播计时到达后会先等待闸门放开再切换到下一条，
 * 避免长文字还没滚完就被切走。独立使用 [SkyTextMarquee]（无外层轮播）时闸门为 null，不介入。
 */
@Stable
internal class SkyMarqueeCycleGate {
    private var holdCount by mutableIntStateOf(0)

    /** 当前是否有内层跑马灯尚未完成一次完整滚动周期。 */
    val isHeld: Boolean get() = holdCount > 0

    fun acquireHold() {
        holdCount += 1
    }

    fun releaseHold() {
        holdCount = (holdCount - 1).coerceAtLeast(0)
    }
}

/** 向 item 内容下发当前 [SkyMarqueeView] 的循环闸门；不在轮播内时为 null。 */
internal val LocalSkyMarqueeCycleGate =
    staticCompositionLocalOf<SkyMarqueeCycleGate?> { null }

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
 * - 若 item 内容内嵌了溢出的 [SkyTextMarquee]，会等其把长文字完整显示一次（滚到末端并停顿）
 *   后再切换下一条（实际切换间隔 = max(flipInterval, 内层跑马灯首次完整显示时长)）。
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

    val gate = remember { SkyMarqueeCycleGate() }

    LaunchedEffect(state.isFlipping, state.flipInterval, items.size) {
        while (state.isFlipping && items.size > 1) {
            delay(state.flipInterval.milliseconds)
            if (!state.isFlipping) break
            // 若当前 item 内嵌了溢出的 SkyTextMarquee，等它把文字完整显示一次后再切换。
            while (state.isFlipping && gate.isHeld) {
                delay(MARQUEE_POLL_INTERVAL_MS.milliseconds)
            }
            if (!state.isFlipping) break
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
        CompositionLocalProvider(LocalSkyMarqueeCycleGate provides gate) {
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
}
