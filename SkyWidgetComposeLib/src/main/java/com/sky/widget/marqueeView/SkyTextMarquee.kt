package com.sky.widget.marqueeView

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** 暂停 / 闸门轮询间隔(毫秒):isPlaying=false 或外层闸门未放开时,以此粒度检查状态变化。 */
private const val MARQUEE_POLL_INTERVAL_MS = 60L

/**
 * 单行文字跑马灯重复模式。
 */
enum class SkyTextMarqueeRepeatMode {
    /** 到达终点停顿后瞬时跳回起点继续滚动,会有可见的"跳回"效果。 */
    Restart,

    /** 到达终点停顿后原路返回,往返式滚动。 */
    Reverse,

    /**
     * 无缝循环:到达"文本完整显示"位置停顿后,**继续同方向**滚出屏幕;
     * 通过在主文本一侧再绘制一份相同文本(间距 [SkyTextMarqueeStyle.gap])实现视觉首尾相接,
     * 无需可见的跳回。
     */
    Loop
}

/**
 * [SkyTextMarquee] 的状态与滚动行为参数集合。
 *
 * 与 [SkyMarqueeState] 保持同构:公开可变状态 + 播放控制方法,便于示例统一驱动;
 * 只有当文字实际宽度超过容器宽度时才会真正启动滚动动画;未超限时行为等价于普通 [BasicText]。
 *
 * 使用方式:通过 [rememberSkyTextMarqueeStyle] 创建,直接给字段赋值即可实时生效
 * (内部为 `mutableStateOf`,Compose 会自动观测并重启动画);
 * 播放控制通过 [play] / [pause] / [toggleFlipping] / [reset]。
 *
 * @param initialDirection 初始水平滚动方向;当前仅支持 LEFT/RIGHT,UP/DOWN 会被忽略并保持静止。
 * @param initialVelocity 初始滚动速度(dp/秒);<=0 会回落到 1dp/秒下限。
 * @param initialStartDelayMillis 初始每轮滚动前的停顿(毫秒)。
 * @param initialEndPauseMillis 初始到达"完整显示"后的停顿(毫秒)。
 * @param initialRepeatMode 初始重复模式。
 * @param initialGap 初始 Loop 模式下两份文本之间的水平间距。
 */
@Stable
class SkyTextMarqueeStyle(
    initialDirection: SkyMarqueeDirection = SkyMarqueeDirection.LEFT,
    initialVelocity: Dp = 40.dp,
    initialStartDelayMillis: Long = 500L,
    initialEndPauseMillis: Long = 800L,
    initialRepeatMode: SkyTextMarqueeRepeatMode = SkyTextMarqueeRepeatMode.Restart,
    initialGap: Dp = 60.dp
) {
    /** 水平滚动方向;仅 LEFT/RIGHT 有效。 */
    var direction by mutableStateOf(initialDirection)

    /** 滚动速度(dp/秒)。 */
    var velocity by mutableStateOf(initialVelocity)

    /** 每轮滚动前的停顿(毫秒)。 */
    var startDelayMillis by mutableLongStateOf(initialStartDelayMillis)

    /** 每轮到达"完整显示"后的停顿(毫秒)。 */
    var endPauseMillis by mutableLongStateOf(initialEndPauseMillis)

    /** 重复模式,见 [SkyTextMarqueeRepeatMode]。 */
    var repeatMode by mutableStateOf(initialRepeatMode)

    /** Loop 模式下两份文本的水平间距;仅 Loop 生效。 */
    var gap by mutableStateOf(initialGap)

    /** 用户可设的播放开关;false 时当前动画段结束后暂停,不再进入下一段。 */
    var isPlaying by mutableStateOf(true)
        private set

    /** 当前是否真正在滚动动画中(供外部观察)。 */
    var isRunning by mutableStateOf(false)
        internal set

    /** 当前水平平移像素值(负值表示向左偏移,供外部观察滚动进度)。 */
    var translationX by mutableFloatStateOf(0f)
        internal set

    /** 内部复位信号:递增会重启 [LaunchedEffect],让动画从起点重跑。 */
    internal var resetSignal by mutableLongStateOf(0L)
        private set

    /** 开始 / 恢复滚动。 */
    fun play() {
        isPlaying = true
    }

    /** 暂停滚动(等当前动画段完成后停在原位)。 */
    fun pause() {
        isPlaying = false
    }

    /** 切换播放 / 暂停。 */
    fun toggleFlipping() {
        isPlaying = !isPlaying
    }

    /** 复位到起点并重启一轮动画(不改变播放开关)。 */
    fun reset() {
        resetSignal += 1
    }

    /** 语义化的"开始"别名,与 [SkyMarqueeState.startFlipping] 对齐。 */
    fun startFlipping() = play()

    /** 语义化的"停止"别名,与 [SkyMarqueeState.stopFlipping] 对齐;会停在当前段完成位。 */
    fun stopFlipping() = pause()
}

/**
 * 创建并记住一个 [SkyTextMarqueeStyle]。
 *
 * @param initialDirection 初始水平滚动方向。
 * @param initialVelocity 初始滚动速度(dp/秒)。
 * @param initialStartDelayMillis 初始起始停顿(毫秒)。
 * @param initialEndPauseMillis 初始终点停顿(毫秒)。
 * @param initialRepeatMode 初始重复模式。
 * @param initialGap 初始 Loop 间距。
 */
@Composable
fun rememberSkyTextMarqueeStyle(
    initialDirection: SkyMarqueeDirection = SkyMarqueeDirection.LEFT,
    initialVelocity: Dp = 40.dp,
    initialStartDelayMillis: Long = 500L,
    initialEndPauseMillis: Long = 800L,
    initialRepeatMode: SkyTextMarqueeRepeatMode = SkyTextMarqueeRepeatMode.Restart,
    initialGap: Dp = 60.dp
): SkyTextMarqueeStyle = remember {
    SkyTextMarqueeStyle(
        initialDirection = initialDirection,
        initialVelocity = initialVelocity,
        initialStartDelayMillis = initialStartDelayMillis,
        initialEndPauseMillis = initialEndPauseMillis,
        initialRepeatMode = initialRepeatMode,
        initialGap = initialGap
    )
}

/**
 * 单行文字跑马灯。
 *
 * 使用场景:顶部公告栏 / 通知栏等一行文字,内容过长(超出可视宽度)时自动水平滚动;
 * 未超出时不做任何动画,等价于普通 [BasicText]。
 *
 * 与 [SkyMarqueeView] 的分工:
 * - [SkyMarqueeView]:负责「多个 item 之间的切换轮播」(上下左右四方向);
 * - [SkyTextMarquee]:负责「单个 item 内的一行文字本身的水平滚动」;
 * - 二者可组合:在 [SkyMarqueeView] 的 `itemContent` 插槽内嵌套 [SkyTextMarquee],即可得到
 *   「item 之间轮播 + item 内长文字继续滚动」的复合效果。
 *
 * 三种滚动模式([SkyTextMarqueeStyle.repeatMode]):
 * - [SkyTextMarqueeRepeatMode.Restart]:滚完跳回起点,存在视觉跳变;
 * - [SkyTextMarqueeRepeatMode.Reverse]:往返式来回滚;
 * - [SkyTextMarqueeRepeatMode.Loop]:**无缝循环**——完整显示时停顿,然后继续同方向滚出屏幕,
 *   通过在主文本一侧再绘制一份相同文本形成首尾相接,不会出现跳回。适合顶部公告栏。
 *
 * @param text 要展示的单行文本。
 * @param modifier 修饰符;建议限定宽度或使用 `fillMaxWidth()` 以便检测溢出。
 * @param style 文本样式(字号 / 颜色 / 字重等),沿用 [BasicText] 语义。
 * @param marqueeStyle 滚动状态与行为参数,见 [SkyTextMarqueeStyle]。
 * @param onClick 点击回调;null 表示不消费点击。
 */
@Composable
fun SkyTextMarquee(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 15.sp,
        color = Color(0xFF333333)
    ),
    marqueeStyle: SkyTextMarqueeStyle = rememberSkyTextMarqueeStyle(),
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 依据当前样式测量单行文字的像素宽度;maxLines=1 与渲染端保持一致。
    val textLayoutResult = remember(text, style, density) {
        textMeasurer.measure(
            text = text,
            style = style,
            maxLines = 1
        )
    }
    val textWidthPx = textLayoutResult.size.width.toFloat()

    // 独立持有平移量,避免容器 recomposition 打断动画。
    val translationAnimatable = remember { Animatable(0f) }

    // 外层 SkyMarqueeView 下发的循环闸门(不在轮播内时为 null):
    // 存在时需在滚完首个完整周期前占住它,阻止外层因 flipInterval 提前切走本页长文字。
    val cycleGate = LocalSkyMarqueeCycleGate.current

    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    // 直接读取 style 里的可观察字段(而不是缓存 val),
    // 保证用户改动 style 时,BoxWithConstraints 的绘制分支与 LaunchedEffect 的 keys 同步刷新。
    val currentRepeatMode = marqueeStyle.repeatMode
    val loopMode = currentRepeatMode == SkyTextMarqueeRepeatMode.Loop
    val gapPx = with(density) { marqueeStyle.gap.toPx() }

    // 外层 BoxWithConstraints 负责裁剪与点击;内部文本只平移自身,
    // 由 clipToBounds 做可视裁剪,允许文本宽度 > 容器宽度。
    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .then(clickModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val overflow = textWidthPx > containerWidthPx
        val scrollRangePx = max(textWidthPx - containerWidthPx, 0f)
        // Loop 模式一整圈的位移 = 一份文本宽 + gap;到位后瞬间归零视觉无痕。
        val loopCyclePx = textWidthPx + gapPx

        LaunchedEffect(
            overflow,
            scrollRangePx,
            loopCyclePx,
            marqueeStyle.direction,
            marqueeStyle.velocity,
            marqueeStyle.startDelayMillis,
            marqueeStyle.endPauseMillis,
            marqueeStyle.repeatMode,
            marqueeStyle.resetSignal
        ) {
            val verticalDirection = marqueeStyle.direction == SkyMarqueeDirection.UP ||
                marqueeStyle.direction == SkyMarqueeDirection.DOWN
            if (!overflow || verticalDirection) {
                marqueeStyle.isRunning = false
                marqueeStyle.translationX = 0f
                translationAnimatable.snapTo(0f)
                return@LaunchedEffect
            }

            val velocityPxPerSec = with(density) { marqueeStyle.velocity.toPx() }
                .coerceAtLeast(1f)
            val pxPerMillis = velocityPxPerSec / 1000f

            val leftward = marqueeStyle.direction == SkyMarqueeDirection.LEFT
            // 起点:主文本左沿对齐容器左沿(LEFT)或右沿对齐容器右沿(RIGHT)时的平移量。
            val startValue = if (leftward) 0f else -scrollRangePx
            // 终态:主文本"完整显示"的平移量。
            val endValue = if (leftward) -scrollRangePx else 0f
            val phase1Distance = abs(endValue - startValue)
            val phase1Duration = (phase1Distance / pxPerMillis).toInt().coerceAtLeast(16)

            // 处于外层轮播内:占住闸门,直到本 item 的文字被完整显示一次(reveal 完成)再放开。
            var holding = false
            var revealDone = false
            if (cycleGate != null) {
                cycleGate.acquireHold()
                holding = true
            }

            // 文字首次完整显示(到达 endValue 并完成 endPause)后立即放开闸门,
            // 让外层 SkyMarqueeView 尽快切换;而不是等 Loop 的无缝副本再滚一遍
            // (否则一条长文字会被看到"滚了 2 遍才切")。
            fun onRevealCompleted() {
                if (holding && !revealDone) {
                    revealDone = true
                    holding = false
                    cycleGate?.releaseHold()
                }
            }

            marqueeStyle.isRunning = true
            translationAnimatable.snapTo(startValue)
            marqueeStyle.translationX = startValue
            // 一次「起点 ↔ 完整显示」平移段:起始停顿 → 匀速滚到 [target] → 终点停顿;
            // [reveal] 为 true 表示该段把文字完整显示了一次,需放行外层 SkyMarqueeView 的切换闸门。
            suspend fun scrollLeg(target: Float, duration: Int, reveal: Boolean) {
                delay(marqueeStyle.startDelayMillis.milliseconds)
                translationAnimatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(duration, easing = LinearEasing)
                )
                marqueeStyle.translationX = translationAnimatable.value
                delay(marqueeStyle.endPauseMillis.milliseconds)
                if (reveal) onRevealCompleted()
            }

            try {
                var forward = true
                while (isActive) {
                    // 暂停门控:isPlaying=false 时挂起等待,不推进新段。
                    while (isActive && !marqueeStyle.isPlaying) {
                        delay(MARQUEE_POLL_INTERVAL_MS.milliseconds)
                    }
                    if (!isActive) break

                    when (marqueeStyle.repeatMode) {
                        SkyTextMarqueeRepeatMode.Restart -> {
                            // 滚到完整显示后瞬时跳回起点,存在可见跳变。
                            scrollLeg(endValue, phase1Duration, reveal = true)
                            translationAnimatable.snapTo(startValue)
                            marqueeStyle.translationX = startValue
                        }

                        SkyTextMarqueeRepeatMode.Reverse -> {
                            // 去程(forward=true → endValue)即为完整显示一次;返程不重复放行闸门。
                            scrollLeg(
                                target = if (forward) endValue else startValue,
                                duration = phase1Duration,
                                reveal = forward
                            )
                            forward = !forward
                        }

                        SkyTextMarqueeRepeatMode.Loop -> {
                            // 阶段 1:主文本从"起点"滚到"完整显示"并停顿,立即放行闸门
                            // (不必等阶段 2 的无缝副本再滚一遍,修复"滚了 2 遍才切")。
                            scrollLeg(endValue, phase1Duration, reveal = true)

                            // 阶段 2:继续沿同方向滚出屏幕,副文本从另一侧接管进入视野;
                            // 位移 = loopCyclePx - scrollRangePx = containerWidthPx + gapPx,
                            // 到达后副文本恰好落在主文本"起点位",snapTo 无痕复位。
                            val loopTarget = if (leftward) {
                                -loopCyclePx
                            } else {
                                startValue + loopCyclePx
                            }
                            val loopDistance = abs(loopTarget - translationAnimatable.value)
                            val loopDuration = (loopDistance / pxPerMillis).toInt().coerceAtLeast(16)
                            translationAnimatable.animateTo(
                                targetValue = loopTarget,
                                animationSpec = tween(loopDuration, easing = LinearEasing)
                            )
                            marqueeStyle.translationX = translationAnimatable.value

                            // 无缝复位:此时副文本处于视觉上与主文本起点相同的位置,
                            // 归零不产生可见跳变。
                            translationAnimatable.snapTo(startValue)
                            marqueeStyle.translationX = startValue
                        }
                    }
                }
            } finally {
                // 尚未完整显示一次就被销毁(切走 / 参数变更):兜底释放,避免闸门永久占住卡死外层。
                if (holding) {
                    cycleGate?.releaseHold()
                }
                marqueeStyle.isRunning = false
            }
        }

        if (loopMode && overflow) {
            // Loop 模式:两份文本首尾相接,主副本共用同一个 graphicsLayer 平移。
            // LEFT 方向副本文本在主文本右侧(正偏移),RIGHT 方向副本文本在主文本左侧(负偏移),
            // 保证循环终点时副本恰好落在主文本的"起点位",实现无可见跳变的无缝衔接。
            val leftwardPlacement = marqueeStyle.direction == SkyMarqueeDirection.LEFT
            val secondaryOffsetPx = if (leftwardPlacement) {
                textWidthPx + gapPx
            } else {
                -(textWidthPx + gapPx)
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = translationAnimatable.value
                    }
            ) {
                MarqueeLineText(text = text, style = style)
                MarqueeLineText(
                    text = text,
                    style = style,
                    modifier = Modifier.offset { IntOffset(secondaryOffsetPx.roundToInt(), 0) }
                )
            }
        } else {
            // 平移作用于整行文本;graphicsLayer 与 wrapContentWidth 顺序不影响绘制结果。
            MarqueeLineText(
                text = text,
                style = style,
                modifier = Modifier.graphicsLayer {
                    translationX = translationAnimatable.value
                }
            )
        }
    }
}

/**
 * [SkyTextMarquee] 内部单行文本渲染:固定 `maxLines=1` + [TextOverflow.Clip],
 * 并以 `wrapContentWidth(unbounded=true)` 允许文本按自然宽度测量(可超出父容器),
 * 交由外层 [BoxWithConstraints] 的 clipToBounds 做可视裁剪。
 *
 * 抽出统一封装,避免「主文本 / Loop 副本文本 / 非 Loop 文本」三处重复同一组关键布局配置;
 * 一旦某处遗漏 `unbounded`,就会复现"溢出文字不绘制、平移只见空白"的历史 bug。
 *
 * @param text 要渲染的单行文本。
 * @param style 文本样式(字号 / 颜色 / 字重等)。
 * @param modifier 叠加在 `wrapContentWidth` 之前的修饰符(如平移 graphicsLayer / 偏移 offset)。
 */
@Composable
private fun MarqueeLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.wrapContentWidth(Alignment.Start, unbounded = true)
    )
}
