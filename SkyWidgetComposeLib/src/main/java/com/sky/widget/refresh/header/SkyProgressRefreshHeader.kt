package com.sky.widget.refresh.header

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshState

/**
 * [SkyProgressRefreshHeader] 的可定制资源状态。所有字段为 null 时使用库内默认值。
 *
 * @param color 辉光与进度条颜色；null 时取 `MaterialTheme.colorScheme.primary`
 */
@Stable
class SkyProgressRefreshHeaderState(
    val color: Color? = null
)

/** 创建并记住 [SkyProgressRefreshHeaderState]。 */
@Composable
fun rememberSkyProgressRefreshHeaderState(
    color: Color? = null
): SkyProgressRefreshHeaderState = remember(color) { SkyProgressRefreshHeaderState(color) }

/**
 * 辉光 + 线性进度条刷新 Header（FixedFront 效果图配套样式）。
 *
 * - 下拉中：顶部 72dp 区域显示自上而下渐隐的主题色辉光（FastOutSlowIn 渐入），
 *   顶部线性进度条按下拉距离实时填充；
 * - 刷新中：进度条切换为无限循环的不确定进度动画；
 * - 空闲时：完全不绘制任何内容，避免常驻轨道线。
 *
 * 位移由外层 [SkyRefreshLayout] 容器完成，本组件只按进度绘制，
 * 与 [com.sky.widget.refresh.SkyRefreshStyle.FixedFront]（固定在前面）搭配效果最佳。
 *
 * 使用方式（直接传入容器同一个 state）：
 *
 * ```kotlin
 * SkyRefreshLayout(
 *     state = state,
 *     style = SkyRefreshStyle.FixedFront,
 *     header = { SkyProgressRefreshHeader(state) },
 *     ...
 * )
 * ```
 *
 * 仅支持纵向（Vertical）排版。
 *
 * @param state 外层 `SkyRefreshLayout` 的同一个 [SkyRefreshState]
 * @param headerState 可定制资源状态，默认全用库内默认值
 */
@Composable
fun SkyProgressRefreshHeader(
    state: SkyRefreshState,
    headerState: SkyProgressRefreshHeaderState = rememberSkyProgressRefreshHeaderState()
) {
    val color = headerState.color ?: MaterialTheme.colorScheme.primary
    val refreshing = state.refreshFlag == SkyRefreshFlag.REFRESHING
    val pulling = state.refreshFlag == SkyRefreshFlag.PULLING
    // 触发阈值即自身声明的高度（容器测量后写入 headerBound，二者恒等）
    val trigger = state.headerBound.coerceAtLeast(1f)
    val progress = (state.indicatorOffset / trigger).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .drawWithCache {
                onDrawBehind {
                    // 辉光：自上而下渐隐的半透明主题色，随下拉进度以 FastOutSlowIn 曲线淡入
                    val brush = Brush.verticalGradient(
                        0f to color.copy(alpha = 0.45f),
                        1f to color.copy(alpha = 0f)
                    )
                    if (pulling) {
                        drawRect(
                            brush = brush,
                            alpha = FastOutSlowInEasing.transform(progress)
                        )
                    }
                }
            }
            .fillMaxWidth()
            .height(72.dp)
    ) {
        when {
            // 刷新中：无限循环的不确定进度条
            refreshing -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
            // 下拉中：随下拉距离实时填充的确定进度条（空闲时不绘制，避免常驻轨道线）
            progress > 0f -> LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
        }
    }
}
