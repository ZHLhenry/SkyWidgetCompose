package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.chainScrollLayout.SkyChainScrollLayout
import com.sky.widget.chainScrollLayout.SkyChainScrollMode
import com.sky.widget.chainScrollLayout.SkyChainScrollPosition
import com.sky.widget.chainScrollLayout.rememberSkyChainScrollState
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────
// 示例菜单
//
// 每个子示例独占一个全屏页面：联动滚动依赖嵌套滚动分发，
// 若多个滚动容器同页嵌套（外层再套 verticalScroll）会互相抢占手势导致滑动卡顿，
// 因此这里与刷新示例保持一致的「菜单页 + 独立子页」结构。
// ─────────────────────────────────────────────────────────────────────

private data class ChainDemoEntry(val title: String, val subtitle: String, val screen: Screen)

private val chainDemoEntries = listOf(
    ChainDemoEntry("顶部联动 · 同步模式", "ChainContentFirst（默认）：联动条与内容滚动实时同步折叠 / 展开", Screen.ChainTopSync),
    ChainDemoEntry("顶部联动 · 内容优先", "ContentFirst：内容先滚，回到顶部后再下拉才展开联动条", Screen.ChainTopContentFirst),
    ChainDemoEntry("顶部联动 · 溢出联动", "ChainAfterContent：内容滚到边界后，溢出位移才驱动联动条", Screen.ChainTopAfterContent),
    ChainDemoEntry("底部联动条", "Bottom：底部工具栏随滚动下移隐藏 / 上移显示", Screen.ChainBottomBar),
    ChainDemoEntry("水平联动条", "Start：横向 LazyRow 滚动驱动左侧联动条折叠 / 展开", Screen.ChainHorizontalBar),
    ChainDemoEntry("程序化控制", "setScrollPosition / animateToScrollPosition 与百分比读取", Screen.ChainProgrammatic),
)

/**
 * 联动滚动示例菜单页。
 */
@Composable
fun ChainScrollDemoMenuScreen(onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    ChainDemoScaffold(title = "联动滚动示例", onBack = onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chainDemoEntries, key = { it.title }) { entry ->
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

// ─────────────────────────────────────────────────────────────────────
// 共用脚手架
// ─────────────────────────────────────────────────────────────────────

/** 子示例统一脚手架：沉浸式蓝色标题栏 + 返回键，内容区占满剩余空间。 */
@Composable
private fun ChainDemoScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
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

// ─────────────────────────────────────────────────────────────────────
// 示例 1：顶部联动条 + ChainContentFirst（默认）
// ─────────────────────────────────────────────────────────────────────

/**
 * 默认模式：联动条与内容区域同步位移。
 * 向上滚动列表时联动条立即跟随收起，向下滚动时立即展开。
 */
@Composable
fun ChainTopSyncDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 80.dp,
        position = SkyChainScrollPosition.Top,
    )
    ChainDemoScaffold(title = "顶部联动 · 同步模式", onBack = onBack) {
        SkyChainScrollLayout(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            mode = SkyChainScrollMode.ChainContentFirst,
            chainContent = { s ->
                ChainDemoAppBar(
                    title = "联动 AppBar",
                    offsetY = s.getScrollPositionValue().roundToInt(),
                    color = Color(0xFF2196F3),
                )
            },
        ) {
            // 内容区域：LazyColumn 通过 contentPadding 为顶部联动条让出空间
            ChainDemoLazyColumn(topPadding = 80.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 2：ContentFirst 模式
// ─────────────────────────────────────────────────────────────────────

/**
 * 内容优先：列表先滚，滚到顶部后再向下拉才会展开联动条。
 * 适合"列表内容更重要、联动条仅在回到顶部时才出现"的场景。
 */
@Composable
fun ChainTopContentFirstDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 80.dp,
        position = SkyChainScrollPosition.Top,
    )
    ChainDemoScaffold(title = "顶部联动 · 内容优先", onBack = onBack) {
        SkyChainScrollLayout(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            mode = SkyChainScrollMode.ContentFirst,
            chainContent = { s ->
                ChainDemoAppBar(
                    title = "内容优先 AppBar",
                    offsetY = s.getScrollPositionValue().roundToInt(),
                    color = Color(0xFF4CAF50),
                )
            },
        ) {
            ChainDemoLazyColumn(topPadding = 80.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 3：ChainAfterContent 模式
// ─────────────────────────────────────────────────────────────────────

/**
 * 内容滚到边界后才把溢出位移交给联动条。
 * 与 ContentFirst 的区别：不追踪内容累积滚动量，仅在内容真正到底 / 到顶时响应。
 */
@Composable
fun ChainTopAfterContentDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 80.dp,
        position = SkyChainScrollPosition.Top,
    )
    ChainDemoScaffold(title = "顶部联动 · 溢出联动", onBack = onBack) {
        SkyChainScrollLayout(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            mode = SkyChainScrollMode.ChainAfterContent,
            chainContent = { s ->
                ChainDemoAppBar(
                    title = "溢出联动 AppBar",
                    offsetY = s.getScrollPositionValue().roundToInt(),
                    color = Color(0xFFFF9800),
                )
            },
        ) {
            ChainDemoLazyColumn(topPadding = 80.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 4：底部联动条（Bottom）
// ─────────────────────────────────────────────────────────────────────

/**
 * 底部工具栏：向上滚动列表时工具栏下移隐藏，向下滚动时上移显示。
 * 注意 Bottom 位置不支持 ContentFirst 模式。
 */
@Composable
fun ChainBottomBarDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 64.dp,
        position = SkyChainScrollPosition.Bottom,
    )
    ChainDemoScaffold(title = "底部联动条", onBack = onBack) {
        SkyChainScrollLayout(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            mode = SkyChainScrollMode.ChainContentFirst,
            chainContent = { s ->
                // Bottom 位置初始偏移为 maxPx（完全展开），滚动时向 0 收敛（隐藏）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .offset { IntOffset(0, s.getScrollPositionValue().roundToInt()) }
                        .background(Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("底部工具栏", color = Color.White, fontSize = 15.sp)
                }
            },
        ) {
            ChainDemoLazyColumn(bottomPadding = 64.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 5：水平联动条（Start）
// ─────────────────────────────────────────────────────────────────────

/**
 * 水平方向：横向滚动 LazyRow 时，左侧联动条同步折叠 / 展开。
 */
@Composable
fun ChainHorizontalBarDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 100.dp,
        position = SkyChainScrollPosition.Start,
    )
    ChainDemoScaffold(title = "水平联动条", onBack = onBack) {
        SkyChainScrollLayout(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            mode = SkyChainScrollMode.ChainContentFirst,
            chainContent = { s ->
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxSize()
                        .offset { IntOffset(s.getScrollPositionValue().roundToInt(), 0) }
                        .background(Color(0xFF9C27B0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("侧边栏", color = Color.White, fontSize = 14.sp)
                }
            },
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(30) { index ->
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                            .background(chainDemoColor(index)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Item $index", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 6：代码控制与百分比读取
// ─────────────────────────────────────────────────────────────────────

/**
 * 代码控制：通过 state 提供的 API 主动设置滚动位置，并实时读取百分比。
 */
@Composable
fun ChainProgrammaticDemoScreen(onBack: () -> Unit) {
    val state = rememberSkyChainScrollState(
        minScrollPosition = 0.dp,
        maxScrollPosition = 80.dp,
        position = SkyChainScrollPosition.Top,
    )
    ChainDemoScaffold(title = "程序化控制", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 控制面板：实时展示当前位置与百分比，提供三个程序化滚动入口
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "当前位置：${state.getScrollPositionValue().roundToInt()} px，" +
                        "百分比：${"%.2f".format(state.getScrollPositionPercentage())}",
                    fontSize = 13.sp,
                    color = Color(0xFF2196F3),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 无动画展开到 0（完全展开）
                    OutlinedButton(onClick = { state.setScrollPosition(0f) }) { Text("展开") }
                    // 无动画收起到 minPx（完全隐藏）
                    OutlinedButton(onClick = { state.setScrollPosition(state.minPx) }) { Text("收起") }
                    // 有动画展开
                    Button(onClick = { state.setScrollPositionWithAnimate(0f) }) { Text("动画展开") }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                SkyChainScrollLayout(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFAFAFA)),
                    mode = SkyChainScrollMode.ChainContentFirst,
                    chainContent = { s ->
                        ChainDemoAppBar(
                            title = "受控 AppBar",
                            offsetY = s.getScrollPositionValue().roundToInt(),
                            color = Color(0xFF00BCD4),
                        )
                    },
                ) {
                    ChainDemoLazyColumn(topPadding = 80.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 通用小组件
// ─────────────────────────────────────────────────────────────────────

/** 示例 AppBar：固定高度 80dp，根据 offsetY 整体平移实现折叠效果。 */
@Composable
private fun ChainDemoAppBar(title: String, offsetY: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .offset { IntOffset(0, offsetY) }
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 示例列表：30 条彩色行。
 *
 * 通过 [topPadding] / [bottomPadding] 为联动条预留空间，避免内容被遮挡；
 * 这两个 padding 直接作用于 LazyColumn 的 contentPadding，与联动条位移解耦。
 */
@Composable
private fun ChainDemoLazyColumn(
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
    ) {
        items(30) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(chainDemoColor(index)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "列表项 $index",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

/** 按索引循环取色，保证相邻行颜色不同。 */
private fun chainDemoColor(index: Int): Color = when (index % 6) {
    0 -> Color(0xFF2196F3)
    1 -> Color(0xFF4CAF50)
    2 -> Color(0xFFFF9800)
    3 -> Color(0xFFE91E63)
    4 -> Color(0xFF9C27B0)
    else -> Color(0xFF00BCD4)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChainScrollDemoMenuPreview() {
    SkyWidgetComposeTheme {
        ChainScrollDemoMenuScreen(onBack = {}, onNavigate = {})
    }
}
