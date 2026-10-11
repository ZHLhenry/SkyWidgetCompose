package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * 跑马灯示例入口菜单页。
 *
 * 为清晰演示两个组件各自的全量 API，示例拆分为两个独立子页面（各自独占全屏，互不干扰）：
 * - [MarqueeTextDemoScreen]：[com.sky.widget.marqueeView.SkyTextMarquee] 单行文字横向跑马灯。
 * - [MarqueeViewDemoScreen]：[com.sky.widget.marqueeView.SkyMarqueeView] 多 item 轮播容器
 *   及其状态控制器 [com.sky.widget.marqueeView.SkyMarqueeState] 的全部编程接口。
 */
@Composable
fun MarqueeDemoScreen(onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    MarqueeDemoScaffold(title = "跑马灯示例", onBack = onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(marqueeDemoEntries, key = { it.title }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .clickable { onNavigate(entry.screen) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.title, fontSize = 15.sp, color = Color(0xFF333333))
                        Text(
                            text = entry.subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB)
                    )
                }
                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
            }
        }
    }
}

private data class MarqueeDemoEntry(
    val title: String,
    val subtitle: String,
    val screen: Screen
)

private val marqueeDemoEntries = listOf(
    MarqueeDemoEntry(
        title = "SkyTextMarquee 文字跑马灯",
        subtitle = "单行文字溢出横向滚动：方向 / 重复模式（跳回·往返·无缝）/ 速度 / 首尾停顿 / Loop 间距 / 播放控制 全量演示",
        screen = Screen.MarqueeText
    ),
    MarqueeDemoEntry(
        title = "SkyMarqueeView 轮播容器",
        subtitle = "多 item 轮播：四方向 / 切换间隔 / startFlipping·stopFlipping·reset·next·previous·toggleFlipping·setCurrentIndex / 点击事件 / 嵌套文字跑马灯",
        screen = Screen.MarqueeView
    ),
)

/**
 * 跑马灯示例统一脚手架：沉浸式蓝色标题栏 + 返回键，内容区占满剩余空间。
 * 菜单页与两个子页共用，保证视觉与交互一致。
 */
@Composable
internal fun MarqueeDemoScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MarqueeDemoScreenPreview() {
    SkyWidgetComposeTheme {
        MarqueeDemoScreen(onBack = {}, onNavigate = {})
    }
}
