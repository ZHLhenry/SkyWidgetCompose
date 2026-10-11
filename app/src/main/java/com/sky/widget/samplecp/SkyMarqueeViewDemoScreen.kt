package com.sky.widget.samplecp

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.marqueeView.SkyMarqueeDirection
import com.sky.widget.marqueeView.SkyMarqueeState
import com.sky.widget.marqueeView.SkyMarqueeView
import com.sky.widget.marqueeView.SkyTextMarquee
import com.sky.widget.marqueeView.SkyTextMarqueeRepeatMode
import com.sky.widget.marqueeView.rememberSkyMarqueeState
import com.sky.widget.marqueeView.rememberSkyTextMarqueeStyle
import com.sky.widget.samplecp.marquee.MarqueeUiIntent
import com.sky.widget.samplecp.marquee.MarqueeUiState
import com.sky.widget.samplecp.marquee.MarqueeViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * [com.sky.widget.marqueeView.SkyMarqueeView] 轮播容器全量 API 演示子页。
 *
 * 采用 SkyMVI 结构：方向选择由 [MarqueeViewModel] 持有并下发；播放 / 切换等编程接口
 * 直接读写 [SkyMarqueeState]。顶部固定一个受面板控制的轮播预览，做到"点按钮即所见"。
 *
 * 覆盖组件全部对外能力：
 * - 构造参数：`items` / `state` / `direction`(四方向) / `onItemClick` / `itemContent`
 * - [SkyMarqueeState] 接口：startFlipping / stopFlipping / reset / next / previous /
 *   toggleFlipping / setCurrentIndex，及观察态 currentIndex / isFlipping、可调 flipInterval
 * - 交互：`onItemClick` 点击事件派发；单条数据不轮播仍展示
 * - 组合：itemContent 内嵌套 [com.sky.widget.marqueeView.SkyTextMarquee] 的溢出续接滚动
 */
@Composable
fun MarqueeViewDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(viewModel = hiltViewModel<MarqueeViewModel>()) { state, intent ->
        MarqueeViewDemoContent(state = state, intent = intent, onBack = onBack)
    }
}

@Composable
private fun MarqueeViewDemoContent(
    state: MarqueeUiState,
    intent: (MarqueeUiIntent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 顶部受控轮播 + 文字轮播 + 卡片轮播 + 单条示例，各持独立状态
    val controlState = rememberSkyMarqueeState(initialFlipInterval = 2000L)
    val comboState = rememberSkyMarqueeState(initialFlipInterval = 2000L)
    val cardState = rememberSkyMarqueeState(initialFlipInterval = 2500L)
    val singleState = rememberSkyMarqueeState()

    val direction = SkyMarqueeDirection.entries.getOrElse(state.directionIndex) { SkyMarqueeDirection.LEFT }
    var flipIntervalMs by remember { mutableIntStateOf(2000) }

    // 间隔变化时同步所有自动轮播的 state
    LaunchedEffect(flipIntervalMs) {
        val interval = flipIntervalMs.toLong()
        controlState.flipInterval = interval
        comboState.flipInterval = interval
        cardState.flipInterval = interval
    }

    val simpleItems = remember {
        listOf("第 1 页", "第 2 页", "第 3 页", "第 4 页")
    }
    val textItems = remember {
        listOf(
            "欢迎体验 SkyMarqueeView 🎉",
            "欢迎体验 SkyMarqueeView 🎉🎉🎉🎉",
            "我是超长的item内容我是超长的item内容我是超长的item内容"
        )
    }
    val cardItems = remember {
        listOf(
            MarqueeNotice(Color(0xFFE3F2FD), "系统通知", "新版本功能已上线，点击查看详情"),
            MarqueeNotice(Color(0xFFFFF3E0), "活动推荐", "限时优惠活动正在进行中"),
            MarqueeNotice(Color(0xFFE8F5E9), "安全提醒", "请定期修改密码保障账号安全")
        )
    }

    MarqueeDemoScaffold(title = "SkyMarqueeView 轮播容器", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定预览：受下方「属性控制面板」直接驱动，操作即所见
            MarqueeViewPreviewCard(
                state = controlState,
                direction = direction,
                items = simpleItems
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DemoCard(title = "属性控制面板") {
                    SkyMarqueeViewControlPanel(
                        state = controlState,
                        itemCount = simpleItems.size,
                        directionIndex = state.directionIndex,
                        onDirectionSelect = { intent(MarqueeUiIntent.SetDirection(it)) },
                        flipIntervalMs = flipIntervalMs,
                        onFlipIntervalChange = { flipIntervalMs = it }
                    )
                }

                DemoCard(title = "文字轮播 + 溢出续接 + 内层 onClick（嵌套 SkyTextMarquee）") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        SkyMarqueeView(
                            items = textItems,
                            state = comboState,
                            direction = direction,
                            modifier = Modifier.fillMaxWidth(),
                            onItemClick = { _, _ -> }
                        ) { text ->
                            SkyTextMarquee(
                                text = text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                style = TextStyle(fontSize = 15.sp, color = Color(0xFF333333)),
                                marqueeStyle = rememberSkyTextMarqueeStyle(
                                    initialVelocity = 30.dp,
                                    initialStartDelayMillis = 500L,
                                    initialEndPauseMillis = 2000L,
                                    initialRepeatMode = SkyTextMarqueeRepeatMode.Loop
                                ),
                                onClick = {
                                    val shown = if (text.length > 10) text.take(10) + "…" else text
                                    Toast
                                        .makeText(context, "内层点击：「$shown」", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "点击文字由内层 SkyTextMarquee.onClick 响应（优先于外层 onItemClick，后者已置空）；长文字降速并在完整显示处停留更久（velocity=30dp/s、endPause=2000ms）后，外层才切换。",
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }

                DemoCard(title = "卡片轮播（onItemClick 点击事件）") {
                    SkyMarqueeView(
                        items = cardItems,
                        state = cardState,
                        direction = direction,
                        modifier = Modifier.fillMaxWidth(),
                        onItemClick = { index, notice ->
                            Toast
                                .makeText(
                                    context,
                                    "点击了「${notice.title}」第 ${index + 1} 项",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    ) { notice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(notice.backgroundColor)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "📢",
                                    fontSize = 24.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = notice.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF333333)
                                )
                                Text(
                                    text = notice.message,
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                DemoCard(title = "单条数据：不轮播仍展示") {
                    SkyMarqueeView(
                        items = listOf("只有一条数据，不会自动轮播，但内容仍正常展示"),
                        state = singleState,
                        direction = direction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDE7F6))
                            .padding(horizontal = 12.dp)
                    ) { text ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(text = text, fontSize = 14.sp, color = Color(0xFF4527A0), maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** 顶部固定的轮播预览：仅用于展示属性控制面板的切换效果；传入空 onItemClick 以禁用点击暂停（不再演示该功能）。 */
@Composable
private fun MarqueeViewPreviewCard(
    state: SkyMarqueeState,
    direction: SkyMarqueeDirection,
    items: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "受控轮播预览（由下方属性控制面板驱动）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1565C0)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            SkyMarqueeView(
                items = items,
                state = state,
                direction = direction,
                modifier = Modifier.fillMaxWidth(),
                onItemClick = { _, _ -> /* 预览不演示点击暂停，忽略点击 */ }
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = page, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1565C0))
                }
            }
        }
    }
}

/**
 * [SkyMarqueeView] / [SkyMarqueeState] 属性控制面板：将方向、切换间隔与全部播放 / 逐项切换
 * 接口归纳到同一面板，统一驱动顶部受控轮播预览。
 */
@Composable
private fun SkyMarqueeViewControlPanel(
    state: SkyMarqueeState,
    itemCount: Int,
    directionIndex: Int,
    onDirectionSelect: (Int) -> Unit,
    flipIntervalMs: Int,
    onFlipIntervalChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 方向
        Text("切换方向", fontSize = 12.sp, color = Color(0xFF999999))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkyMarqueeDirection.entries.forEachIndexed { index, dir ->
                val selected = directionIndex == index
                OutlinedButton(
                    onClick = { onDirectionSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = when (dir) {
                            SkyMarqueeDirection.LEFT -> "左"
                            SkyMarqueeDirection.RIGHT -> "右"
                            SkyMarqueeDirection.UP -> "上"
                            SkyMarqueeDirection.DOWN -> "下"
                        },
                        fontSize = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF666666)
                    )
                }
            }
        }

        // 切换间隔
        Text("切换间隔", fontSize = 12.sp, color = Color(0xFF999999))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${flipIntervalMs}ms",
                fontSize = 13.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { onFlipIntervalChange((flipIntervalMs - 500).coerceAtLeast(500)) },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("−", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { onFlipIntervalChange((flipIntervalMs + 500).coerceAtMost(5000)) },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 16.sp)
            }
        }

        // 播放控制
        Text("播放控制", fontSize = 12.sp, color = Color(0xFF999999))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { state.startFlipping() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("开始", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { state.stopFlipping() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("停止", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { state.reset() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("重置", fontSize = 12.sp)
            }
        }

        // 逐项切换
        Text("逐项切换", fontSize = 12.sp, color = Color(0xFF999999))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { state.previous() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("上一项", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { state.next(itemCount) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("下一项", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { state.toggleFlipping() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("切换播放", fontSize = 12.sp)
            }
        }

        // setCurrentIndex 跳指定项
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(itemCount) { index ->
                OutlinedButton(
                    onClick = { state.setCurrentIndex(index, itemCount) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("${index + 1}", fontSize = 12.sp)
                }
            }
        }

        // 观察态
        Text(
            text = "currentIndex=${state.currentIndex}  isFlipping=${state.isFlipping}  flipInterval=${state.flipInterval}ms",
            fontSize = 11.sp,
            color = Color(0xFF888888),
            maxLines = 1
        )
    }
}

private data class MarqueeNotice(
    val backgroundColor: Color,
    val title: String,
    val message: String
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MarqueeViewDemoScreenPreview() {
    SkyWidgetComposeTheme {
        MarqueeViewDemoScreen(onBack = {})
    }
}
