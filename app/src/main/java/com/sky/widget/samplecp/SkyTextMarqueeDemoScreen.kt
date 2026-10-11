package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.marqueeView.SkyMarqueeDirection
import com.sky.widget.marqueeView.SkyTextMarquee
import com.sky.widget.marqueeView.SkyTextMarqueeRepeatMode
import com.sky.widget.marqueeView.SkyTextMarqueeStyle
import com.sky.widget.marqueeView.rememberSkyTextMarqueeStyle
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * [com.sky.widget.marqueeView.SkyTextMarquee] 全量 API 演示子页。
 *
 * 顶部固定「预览区」始终可见，下方滚动面板可实时改参数（内部 `mutableStateOf` 存储，
 * LaunchedEffect 观察 keys 变化即时重启动画），做到"调参即所见"。
 *
 * 覆盖组件全部对外能力：
 * - 构造参数：`text` / `style`(TextStyle) / `marqueeStyle` / `onClick`
 * - [SkyTextMarqueeStyle] 行为字段：direction(LEFT/RIGHT)、repeatMode(Restart/Reverse/Loop)、
 *   velocity、startDelayMillis、endPauseMillis、gap
 * - 播放控制：play / pause / toggleFlipping / reset
 * - 观察态：isPlaying / isRunning / translationX
 */
@Composable
fun MarqueeTextDemoScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val noticeStyle = rememberSkyTextMarqueeStyle(
        initialVelocity = 40.dp,
        initialStartDelayMillis = 600L,
        initialEndPauseMillis = 3000L,
        initialRepeatMode = SkyTextMarqueeRepeatMode.Loop,
        initialGap = 80.dp
    )

    MarqueeDemoScaffold(title = "SkyTextMarquee 文字跑马灯", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定预览：调面板参数时始终保持同屏可见
            SkyTextMarqueePreviewCard(style = noticeStyle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DemoCard(title = "属性控制面板") {
                    SkyTextMarqueeControlPanel(style = noticeStyle)
                }

                DemoCard(title = "未溢出：保持静止") {
                    Text(
                        text = "文字宽度未超出可视区域时不滚动，行为等价于普通文本：",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        SkyTextMarquee(
                            text = "短文字不滚动",
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
                            marqueeStyle = rememberSkyTextMarqueeStyle()
                        )
                    }
                }

                DemoCard(title = "点击回调 onClick") {
                    var lastClick by remember { mutableStateOf("点我试试") }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        SkyTextMarquee(
                            text = "↖ 我足够长会被点击触发回调：SkyTextMarquee 支持 onClick 参数，溢出时自动滚动，点击派发事件。",
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(fontSize = 14.sp, color = Color(0xFF1565C0)),
                            marqueeStyle = rememberSkyTextMarqueeStyle(
                                initialVelocity = 30.dp,
                                initialRepeatMode = SkyTextMarqueeRepeatMode.Loop
                            ),
                            onClick = { lastClick = "已点击一次" }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = "onClick 状态：$lastClick", fontSize = 12.sp, color = Color(0xFF666666))
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * 页面顶部固定的 SkyTextMarquee 预览卡：
 * 不受下方 scrollable 内容影响，调面板时能直接看到滚动效果与实时观察态。
 */
@Composable
private fun SkyTextMarqueePreviewCard(style: SkyTextMarqueeStyle) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SkyTextMarquee 预览",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6D4C41)
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                SkyTextMarqueeObservabilityLine(style = style)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            SkyTextMarquee(
                text = "📢 温馨提示：SkyTextMarquee 只在单行文字宽度超出可视区域时才自动滚动，未超限时保持静止；可通过面板实时调参（速度 / 方向 / 模式 / 停顿 / 间距）。",
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(fontSize = 14.sp, color = Color(0xFF6D4C41)),
                marqueeStyle = style
            )
        }
    }
}

/**
 * [SkyTextMarquee] 属性演示面板：直接读写传入 [SkyTextMarqueeStyle] 上的可变状态字段，
 * 任何修改都会即时反映到顶部固定预览区。
 */
@Composable
private fun SkyTextMarqueeControlPanel(style: SkyTextMarqueeStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 播放控制：play / pause / toggleFlipping / reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { style.play() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("开始", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { style.pause() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("暂停", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { style.toggleFlipping() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("切换", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { style.reset() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("重置", fontSize = 12.sp)
            }
        }

        // 方向 + 模式内联标签，尽量把纵向行数压到最低
        // SkyTextMarquee 只支持水平滚动（LEFT/RIGHT），不展示 UP/DOWN 选项
        ChoiceLabelRow(label = "方向")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(SkyMarqueeDirection.LEFT, SkyMarqueeDirection.RIGHT).forEach { dir ->
                val selected = style.direction == dir
                OutlinedButton(
                    onClick = { style.direction = dir },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = when (dir) {
                            SkyMarqueeDirection.LEFT -> "左"
                            else -> "右"
                        },
                        fontSize = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                    )
                }
            }
        }

        ChoiceLabelRow(label = "重复模式")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkyTextMarqueeRepeatMode.entries.forEach { mode ->
                val selected = style.repeatMode == mode
                OutlinedButton(
                    onClick = { style.repeatMode = mode },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = when (mode) {
                            SkyTextMarqueeRepeatMode.Restart -> "跳回"
                            SkyTextMarqueeRepeatMode.Reverse -> "往返"
                            SkyTextMarqueeRepeatMode.Loop -> "无缝"
                        },
                        fontSize = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                    )
                }
            }
        }

        // 数值参数：单行紧凑 Stepper
        StepperRow(
            label = "速度",
            valueText = "${style.velocity.value.toInt()} dp/s",
            onMinus = { style.velocity = (style.velocity.value - 10f).coerceAtLeast(5f).dp },
            onPlus = { style.velocity = (style.velocity.value + 10f).coerceAtMost(300f).dp }
        )
        StepperRow(
            label = "起始停顿",
            valueText = "${style.startDelayMillis} ms",
            onMinus = { style.startDelayMillis = (style.startDelayMillis - 100L).coerceAtLeast(0L) },
            onPlus = { style.startDelayMillis = (style.startDelayMillis + 100L).coerceAtMost(5000L) }
        )
        StepperRow(
            label = "终点停顿",
            valueText = "${style.endPauseMillis} ms",
            onMinus = { style.endPauseMillis = (style.endPauseMillis - 100L).coerceAtLeast(0L) },
            onPlus = { style.endPauseMillis = (style.endPauseMillis + 500L).coerceAtMost(10000L) }
        )
        StepperRow(
            label = "Loop 间距",
            valueText = "${style.gap.value.toInt()} dp",
            onMinus = { style.gap = (style.gap.value - 20f).coerceAtLeast(0f).dp },
            onPlus = { style.gap = (style.gap.value + 20f).coerceAtMost(400f).dp }
        )
    }
}

/** 与预览卡片一同展示的可观察状态行。 */
@Composable
private fun SkyTextMarqueeObservabilityLine(style: SkyTextMarqueeStyle) {
    Text(
        text = "playing=${style.isPlaying}  running=${style.isRunning}  " +
            "tx=${"%.0f".format(style.translationX)}px",
        fontSize = 11.sp,
        color = Color(0xFF888888),
        maxLines = 1
    )
}

/** 面板内紧凑内联小标题（单行、高度低）。 */
@Composable
private fun ChoiceLabelRow(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        color = Color(0xFF666666),
        modifier = Modifier.padding(top = 2.dp)
    )
}

/** 单行紧凑版 Stepper：左侧 label + 右侧 [值][−][+]。 */
@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF666666),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            fontSize = 13.sp,
            color = Color(0xFF333333),
            maxLines = 1
        )
        OutlinedButton(
            onClick = onMinus,
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("−", fontSize = 16.sp)
        }
        OutlinedButton(
            onClick = onPlus,
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MarqueeTextDemoScreenPreview() {
    SkyWidgetComposeTheme {
        MarqueeTextDemoScreen(onBack = {})
    }
}
