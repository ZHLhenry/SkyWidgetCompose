package com.sky.widget.refresh.header

import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshLayout
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * [SkyBallRefreshHeader] 的可定制资源状态。
 *
 * @param refreshingColor 刷新中的小球颜色
 * @param idleColor 未刷新时的小球颜色
 */
@Stable
class SkyBallRefreshHeaderState(
    val refreshingColor: Color = Color(0xFF33AAFF),
    val idleColor: Color = Color(0xFFEEEEEE)
)

/** 创建并记住 [SkyBallRefreshHeaderState]。 */
@Composable
fun rememberSkyBallRefreshHeaderState(
    refreshingColor: Color = Color(0xFF33AAFF),
    idleColor: Color = Color(0xFFEEEEEE)
): SkyBallRefreshHeaderState = remember(refreshingColor, idleColor) {
    SkyBallRefreshHeaderState(refreshingColor, idleColor)
}

/**
 * 球状加载刷新 Header：三个小球依次缩放的加载动画。
 *
 * 动画原理：三个小球以 120ms 间隔依次启动缩放循环（周期 750ms），
 * 使用 [AccelerateDecelerateInterpolator] 实现先加速后减速的物理质感。
 * 缩放曲线：前半程从 1.0 缩小到 0.3，后半程从 0.3 恢复到 1.0，形成"呼吸"效果。
 *
 * 未刷新时显示灰色静止小球，[SkyRefreshFlag.REFRESHING] 时变为蓝色并播放缩放动画。
 * 通过 [SkyRefreshLayout] 的 `header` 插槽使用：
 *
 * ```kotlin
 * SkyRefreshLayout(
 *     state = state,
 *     header = { SkyBallRefreshHeader(flag = state.refreshFlag) },
 *     ...
 * )
 * ```
 *
 * @param flag 头部刷新状态，传 `state.refreshFlag`
 * @param orientation 与外层 [SkyRefreshLayout] 的 orientation 保持一致，
 * 决定主轴尺寸（纵向高 60dp / 横向宽 60dp，即触发刷新的阈值）
 * @param headerState 可定制资源状态，默认全用库内默认值
 */
@Composable
fun SkyBallRefreshHeader(
    flag: SkyRefreshFlag,
    orientation: Orientation = Orientation.Vertical,
    headerState: SkyBallRefreshHeaderState = rememberSkyBallRefreshHeaderState()
) {
    Box(
        modifier = Modifier.let {
            if (orientation == Orientation.Vertical) it.fillMaxWidth().height(60.dp)
            else it.fillMaxHeight().width(60.dp)
        },
        contentAlignment = Alignment.Center
    ) {
        val refreshing = flag == SkyRefreshFlag.REFRESHING
        val density = LocalDensity.current
        val radius = with(density) { 8.dp.toPx() }
        val spacing = with(density) { 8.dp.toPx() }
        val interpolator = remember { AccelerateDecelerateInterpolator() }
        val ballColor = if (refreshing) headerState.refreshingColor else headerState.idleColor

        // 动画时钟：以 16ms（约 60fps）为步进驱动小球缩放
        var elapsed by remember { mutableLongStateOf(0L) }
        LaunchedEffect(refreshing) {
            if (refreshing) {
                val start = System.currentTimeMillis()
                while (true) {
                    elapsed = System.currentTimeMillis() - start
                    delay(16.milliseconds)
                }
            }
        }

        // 三个小球以 120ms 间隔依次启动，形成波浪式缩放效果
        Canvas(modifier = Modifier.size(width = 64.dp, height = 16.dp)) {
            for (i in 0 until 3) {
                // 每个小球延迟 120ms 启动，错开相位
                val time = elapsed - 120L * (i + 1)
                var percent = if (time > 0) (time % 750) / 750f else 0f
                percent = interpolator.getInterpolation(percent)
                // 缩放曲线：前半程 1.0→0.3 缩小，后半程 0.3→1.0 恢复
                val scale = if (refreshing) {
                    if (percent < 0.5f) 1 - percent * 2 * 0.7f else percent * 2 * 0.7f - 0.4f
                } else {
                    1f
                }
                // 水平居中排列三个小球
                val cx = center.x + (i - 1) * (spacing + radius * 2)
                drawCircle(ballColor, radius = radius * scale, center = Offset(cx, center.y))
            }
        }
    }
}
