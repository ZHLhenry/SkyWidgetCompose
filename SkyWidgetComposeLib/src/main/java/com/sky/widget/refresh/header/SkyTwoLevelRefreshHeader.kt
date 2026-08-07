package com.sky.widget.refresh.header

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.R
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshState

/**
 * [SkyTwoLevelRefreshHeader] 的可定制资源状态。所有字段为 null 时使用库内默认 `sky_rl_header_*` 资源。
 *
 * @param pullingText 下拉中文案（默认“下拉开始刷新”）
 * @param releaseText 拉过刷新阈值文案（默认“释放立即刷新”）
 * @param secondaryText 拉过二级阈值文案（默认“释放进入二楼”）
 * @param refreshingText 刷新中文案（默认“正在刷新…”）
 * @param finishedText 刷新完成文案（默认“刷新完成”）
 */
@Stable
class SkyTwoLevelRefreshHeaderState(
    val pullingText: String? = null,
    val releaseText: String? = null,
    val secondaryText: String? = null,
    val refreshingText: String? = null,
    val finishedText: String? = null
)

/** 创建并记住 [SkyTwoLevelRefreshHeaderState]。 */
@Composable
fun rememberSkyTwoLevelRefreshHeaderState(
    pullingText: String? = null,
    releaseText: String? = null,
    secondaryText: String? = null,
    refreshingText: String? = null,
    finishedText: String? = null
): SkyTwoLevelRefreshHeaderState = remember(pullingText, releaseText, secondaryText, refreshingText, finishedText) {
    SkyTwoLevelRefreshHeaderState(pullingText, releaseText, secondaryText, refreshingText, finishedText)
}

/**
 * 下拉进入二楼 Header（参考 SmartRefreshLayout TwoLevelHeader）：
 * 左侧箭头（下拉朝下，拉过阈值旋转 180° 朝上，刷新时切换为转圈进度），右侧单行状态文案。
 *
 * 下拉过程文案三级切换：
 * “下拉开始刷新” →（过刷新阈值）“释放立即刷新” →（过二级阈值）“释放进入二楼”。
 * 松手越过二级阈值时由容器触发 `onSecondFloor` 回调（不触发刷新）。
 *
 * 所有文案统一通过 [SkyTwoLevelRefreshHeaderState] 定制（未传字段回落到库内
 * `sky_rl_header_*` 默认资源）。
 *
 * 使用方式（rate 需与外层容器保持一致）：
 *
 * ```kotlin
 * SkyRefreshLayout(
 *     state = state,
 *     secondFloorRate = 2f,
 *     onSecondFloor = { /* 打开二楼 */ },
 *     header = { SkyTwoLevelRefreshHeader(state, secondFloorRate = 2f) },
 *     ...
 * )
 * ```
 *
 * 仅支持纵向（Vertical）排版。
 *
 * @param state 外层 `SkyRefreshLayout` 的同一个 [SkyRefreshState]
 * @param secondFloorRate 二级阈值倍率，需与外层 `SkyRefreshLayout` 的 secondFloorRate 保持一致
 * @param headerState 可定制资源状态，默认全用库内资源
 */
@Composable
fun SkyTwoLevelRefreshHeader(
    state: SkyRefreshState,
    secondFloorRate: Float = 2f,
    headerState: SkyTwoLevelRefreshHeaderState = rememberSkyTwoLevelRefreshHeaderState()
) {
    val flag = state.refreshFlag
    val refreshing = flag == SkyRefreshFlag.REFRESHING
    // 触发阈值即自身声明的高度（容器测量后写入 headerBound，二者恒等）
    val trigger = state.headerBound.coerceAtLeast(1f)
    val overTrigger = state.indicatorOffset >= trigger
    // 是否越过二级阈值（释放进入二楼）
    val overSecondFloor = secondFloorRate > 0f && state.indicatorOffset >= trigger * secondFloorRate

    val statusText = when {
        refreshing -> headerState.refreshingText ?: stringResource(R.string.sky_rl_header_refreshing)
        flag == SkyRefreshFlag.FINISHING -> headerState.finishedText ?: stringResource(R.string.sky_rl_header_finished)
        overSecondFloor -> headerState.secondaryText ?: stringResource(R.string.sky_rl_header_secondary)
        overTrigger -> headerState.releaseText ?: stringResource(R.string.sky_rl_header_release)
        else -> headerState.pullingText ?: stringResource(R.string.sky_rl_header_pulling)
    }

    // 箭头：未达阈值朝下（下拉），达阈值后旋转 180° 朝上（释放/进二楼）
    val arrowRotation by animateFloatAsState(
        targetValue = if (overTrigger) 180f else 0f,
        label = "SkyTwoLevelRefreshHeaderArrow"
    )
    val contentColor = colorResource(R.color.sky_rl_text_title)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { rotationZ = arrowRotation }
                    ) {
                        val strokeWidthPx = 1.6.dp.toPx()
                        val cx = size.width / 2f
                        // 竖线（箭杆）
                        drawLine(
                            color = Color(0xFF666666),
                            start = Offset(cx, size.height * 0.15f),
                            end = Offset(cx, size.height * 0.75f),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                        // 两侧斜线（箭头，默认朝下）
                        drawLine(
                            color = Color(0xFF666666),
                            start = Offset(size.width * 0.18f, size.height * 0.45f),
                            end = Offset(cx, size.height * 0.78f),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF666666),
                            start = Offset(size.width * 0.82f, size.height * 0.45f),
                            end = Offset(cx, size.height * 0.78f),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Text(
                text = statusText,
                fontSize = 16.sp,
                color = contentColor,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}
