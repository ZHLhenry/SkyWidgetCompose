package com.sky.widget.refresh.header

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.core.content.edit
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.R
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "SkyTimeRefreshHeader"
private const val KEY_LAST_UPDATE_TIME = "lastUpdateTime"

/**
 * [SkyTimeRefreshHeader] 的可定制资源状态。所有字段为 null 时使用库内默认 `sky_rl_header_*` 资源。
 *
 * @param pullingText 下拉中文案（默认“下拉开始刷新”）
 * @param releaseText 拉过阈值文案（默认“释放立即刷新”）
 * @param refreshingText 刷新中文案（默认“正在刷新…”）
 * @param finishedText 刷新完成文案（默认“刷新完成”）
 * @param timeFormat 完整的 SimpleDateFormat pattern（单引号内为原样输出的前缀文字，
 * 默认 `'最后更新：'M-d HH:mm`）；一个字段同时控制时间行文案与格式
 */
@Stable
class SkyTimeRefreshHeaderState(
    val pullingText: String? = null,
    val releaseText: String? = null,
    val refreshingText: String? = null,
    val finishedText: String? = null,
    val timeFormat: String? = null
)

/** 创建并记住 [SkyTimeRefreshHeaderState]。 */
@Composable
fun rememberSkyTimeRefreshHeaderState(
    pullingText: String? = null,
    releaseText: String? = null,
    refreshingText: String? = null,
    finishedText: String? = null,
    timeFormat: String? = null
): SkyTimeRefreshHeaderState = remember(pullingText, releaseText, refreshingText, finishedText, timeFormat) {
    SkyTimeRefreshHeaderState(pullingText, releaseText, refreshingText, finishedText, timeFormat)
}

/**
 * SmartRefreshLayout ClassicsHeader 风格的经典 Header：
 * 左侧箭头（下拉朝下，拉过阈值旋转 180° 朝上，刷新时切换为转圈进度），
 * 右侧两行文字（状态标题 16sp + 最后更新时间 12sp）。
 *
 * 所有文案与时间格式统一通过 [SkyTimeRefreshHeaderState] 定制（未传字段回落到库内
 * `sky_rl_header_*` 默认资源）：
 *
 * ```kotlin
 * header = {
 *     SkyTimeRefreshHeader(
 *         state,
 *         headerState = rememberSkyTimeRefreshHeaderState(
 *             pullingText = "Pull to refresh",
 *             timeFormat = "'Last update:' yyyy-MM-dd HH:mm:ss"
 *         )
 *     )
 * }
 * ```
 *
 * 最后更新时间通过 SharedPreferences 持久化（对齐 ClassicsHeader 行为），
 * 从未刷新过时默认展示当前时间。
 *
 * 仅支持纵向（Vertical）排版。
 *
 * @param state 外层 `SkyRefreshLayout` 的同一个 [SkyRefreshState]
 * @param headerState 可定制资源状态，默认全用库内资源
 */
@Composable
fun SkyTimeRefreshHeader(
    state: SkyRefreshState,
    headerState: SkyTimeRefreshHeaderState = rememberSkyTimeRefreshHeaderState()
) {
    val flag = state.refreshFlag
    val refreshing = flag == SkyRefreshFlag.REFRESHING
    // 触发阈值即自身声明的高度（容器测量后写入 headerBound，二者恒等）
    val trigger = state.headerBound.coerceAtLeast(1f)
    val overTrigger = state.indicatorOffset >= trigger

    // ── 最后更新时间持久化 ──
    // 通过 SharedPreferences 存储上次刷新完成的时间戳，跨 Session 保留。
    // 刷新结束（FINISHING）时自动更新为当前时间；从未刷新过时默认展示当前时间。
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var lastUpdateMillis by rememberSaveable {
        mutableLongStateOf(prefs.getLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis()))
    }
    // 监听状态机：进入 FINISHING 时记录时间戳并持久化
    LaunchedEffect(flag) {
        if (flag == SkyRefreshFlag.FINISHING) {
            val now = System.currentTimeMillis()
            lastUpdateMillis = now
            prefs.edit { putLong(KEY_LAST_UPDATE_TIME, now) }
        }
    }

    val statusText = when {
        refreshing -> headerState.refreshingText ?: stringResource(R.string.sky_rl_header_refreshing)
        flag == SkyRefreshFlag.FINISHING -> headerState.finishedText ?: stringResource(R.string.sky_rl_header_finished)
        overTrigger -> headerState.releaseText ?: stringResource(R.string.sky_rl_header_release)
        else -> headerState.pullingText ?: stringResource(R.string.sky_rl_header_pulling)
    }
    // 时间格式即完整的 SimpleDateFormat pattern（含前缀文字），一个字段同时控制文案与格式
    val timePattern = headerState.timeFormat ?: stringResource(R.string.sky_rl_header_time_format)
    val timeText = remember(timePattern, lastUpdateMillis) {
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(lastUpdateMillis))
    }

    // 箭头：未达阈值朝下（下拉），达阈值后旋转 180° 朝上（释放）
    val arrowRotation by animateFloatAsState(
        targetValue = if (overTrigger) 180f else 0f,
        label = "SkyTimeRefreshHeaderArrow"
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
                    // 自定义箭头绘制：竖线（箭杆）+ 两侧斜线（箭头），默认朝下
                    // 通过 graphicsLayer.rotationZ 实现 0°→180° 的旋转动画
                    Canvas(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer { rotationZ = arrowRotation }
                    ) {
                        val strokeWidthPx = 1.6.dp.toPx()
                        val cx = size.width / 2f
                        // 竖线（箭杆）：从顶部 15% 到底部 75%
                        drawLine(
                            color = Color(0xFF666666),
                            start = Offset(cx, size.height * 0.15f),
                            end = Offset(cx, size.height * 0.75f),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                        // 左侧斜线：从左上到中心底部
                        drawLine(
                            color = Color(0xFF666666),
                            start = Offset(size.width * 0.18f, size.height * 0.45f),
                            end = Offset(cx, size.height * 0.78f),
                            strokeWidth = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                        // 右侧斜线：从右上到中心底部
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
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    color = contentColor
                )
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = colorResource(R.color.sky_rl_text_tertiary)
                )
            }
        }
    }
}
