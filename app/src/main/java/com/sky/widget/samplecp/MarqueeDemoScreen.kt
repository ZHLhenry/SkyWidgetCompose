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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.marqueeView.SkyMarqueeDirection
import com.sky.widget.marqueeView.SkyMarqueeState
import com.sky.widget.marqueeView.SkyMarqueeView
import com.sky.widget.marqueeView.rememberSkyMarqueeState
import com.sky.widget.samplecp.marquee.MarqueeUiIntent
import com.sky.widget.samplecp.marquee.MarqueeUiState
import com.sky.widget.samplecp.marquee.MarqueeViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * 跑马灯 / 轮播视图示例页（SkyMVI 改造版）。
 *
 * 播放状态、方向选择与轮播间隔由 [MarqueeViewModel] 持有并通过 [SkyMviScreen] 下发，
 * SkyMarqueeState 自身负责播放控制，这里用 LaunchedEffect 将 UiState 同步到两个 state。
 *
 * 演示：文字轮播、复杂卡片轮播、方向切换、间隔调节、手动控制。
 */
@Composable
fun MarqueeDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<MarqueeViewModel>(),
    ) { state, intent ->
        MarqueeDemoContent(state = state, intent = intent, onBack = onBack)
    }
}

@Composable
private fun MarqueeDemoContent(
    state: MarqueeUiState,
    intent: (MarqueeUiIntent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val textState: SkyMarqueeState = rememberSkyMarqueeState(initialFlipInterval = 2000L)
    val cardState: SkyMarqueeState = rememberSkyMarqueeState(initialFlipInterval = 2500L)

    val direction = SkyMarqueeDirection.entries.getOrElse(state.directionIndex) { SkyMarqueeDirection.LEFT }
    var flipIntervalMs by remember { mutableIntStateOf(2000) }

    val textItems = remember {
        listOf(
            "欢迎体验 SkyMarqueeView 🎉",
            "Compose 版本支持上下左右四个方向",
            "点击内容可暂停 / 继续自动轮播",
            "也支持自定义复杂的 item 内容",
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

    // 间隔变化时同步两个 state
    LaunchedEffect(flipIntervalMs) {
        textState.flipInterval = flipIntervalMs.toLong()
        cardState.flipInterval = flipIntervalMs.toLong()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "跑马灯示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 控制面板
            DemoCard(title = "控制面板") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 播放控制
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { textState.startFlipping(); cardState.startFlipping() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("开始", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { textState.stopFlipping(); cardState.stopFlipping() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("停止", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { textState.reset(); cardState.reset() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("重置", fontSize = 13.sp)
                        }
                    }

                    // 方向选择
                    Text("切换方向", fontSize = 14.sp, color = Color(0xFF666666))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkyMarqueeDirection.entries.forEachIndexed { index, dir ->
                            val selected = state.directionIndex == index
                            OutlinedButton(
                                onClick = { intent(MarqueeUiIntent.SetDirection(index)) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
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

                    // 间隔调节
                    Text("切换间隔：${flipIntervalMs}ms", fontSize = 14.sp, color = Color(0xFF666666))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { flipIntervalMs = (flipIntervalMs - 500).coerceAtLeast(500) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("-500ms", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { flipIntervalMs = (flipIntervalMs + 500).coerceAtMost(5000) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("+500ms", fontSize = 13.sp)
                        }
                    }
                }
            }

            // 示例 1：简单文字轮播
            DemoCard(title = "文字轮播（点击暂停 / 继续）") {
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
                        state = textState,
                        direction = direction,
                        modifier = Modifier.fillMaxWidth()
                    ) { text ->
                        Text(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            fontSize = 15.sp,
                            color = Color(0xFF333333),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 示例 2：复杂卡片轮播
            DemoCard(title = "卡片轮播（点击事件）") {
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

            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class MarqueeNotice(
    val backgroundColor: Color,
    val title: String,
    val message: String
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MarqueeDemoScreenPreview() {
    SkyWidgetComposeTheme {
        MarqueeDemoScreen(onBack = {})
    }
}
