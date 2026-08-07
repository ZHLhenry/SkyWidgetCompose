package com.sky.widget.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshLayout
import com.sky.widget.refresh.rememberSkyRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────
// 示例菜单
// ─────────────────────────────────────────────────────────────────────

private data class RefreshDemoEntry(val title: String, val subtitle: String, val screen: Screen)

private val refreshDemoEntries = listOf(
    RefreshDemoEntry("基础用法", "onRefresh / onLoadMore / noMoreDataText / finish()", Screen.RefreshBasic),
    RefreshDemoEntry("仅下拉刷新", "enableLoadMore = false", Screen.RefreshOnly),
    RefreshDemoEntry("仅上拉加载", "enableRefresh = false", Screen.LoadMoreOnly),
    RefreshDemoEntry("无终态提示", "noMoreDataText 不传：数据到底后不展示任何 UI", Screen.NoMoreHidden),
    RefreshDemoEntry("阻尼系数", "stickinessLevel：0.2 / 0.5 / 0.8 手感对比", Screen.Stickiness),
    RefreshDemoEntry("横向滑动", "orientation = Orientation.Horizontal", Screen.Horizontal),
    RefreshDemoEntry("自定义指示器", "header / footer 插槽 + SkyRefreshFlag 状态驱动", Screen.CustomIndicator),
    RefreshDemoEntry("程序化触发", "autoRefresh() / autoLoadMore() / 开关动态切换", Screen.AutoTrigger),
)

/**
 * 刷新组件示例菜单页。
 */
@Composable
fun RefreshDemoMenuScreen(onBack: () -> Unit, onNavigate: (Screen) -> Unit) {
    RefreshDemoScaffold(title = "刷新示例", onBack = onBack) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(refreshDemoEntries, key = { it.title }) { entry ->
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
// 共用脚手架与列表
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun RefreshDemoScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
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

/** 纵向文字列表，各示例共用的内容主体。 */
@Composable
private fun DemoItemList(items: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items, key = { it }) { text ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(text = text, fontSize = 15.sp, color = Color(0xFF333333))
            }
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 1：仅下拉刷新（enableLoadMore = false）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun OnlyRefreshDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    RefreshDemoScaffold(title = "仅下拉刷新", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onRefresh = {
                scope.launch {
                    delay(1500)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 2：仅上拉加载（enableRefresh = false）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun LoadMoreOnlyDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableRefresh = false)
    var items by remember { mutableStateOf(List(10) { "列表项 #${it + 1}" }) }

    RefreshDemoScaffold(title = "仅上拉加载", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onLoadMore = {
                scope.launch {
                    delay(1500)
                    val appended = List(5) { "列表项 #${items.size + it + 1}" }
                    items = items + appended
                    state.finish(noMoreData = items.size >= 30)
                }
            },
            noMoreDataText = "已经到底啦 ~"
        ) {
            DemoItemList(items)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例：无终态提示（noMoreDataText 不传）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun NoMoreHiddenDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableRefresh = false)
    var items by remember { mutableStateOf(List(10) { "列表项 #${it + 1}" }) }

    RefreshDemoScaffold(title = "无终态提示", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "未传 noMoreDataText：加载到 30 条后底部不展示任何“无更多数据”UI，末尾也无法拉出提示",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                SkyRefreshLayout(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onLoadMore = {
                        scope.launch {
                            delay(1500)
                            val appended = List(5) { "列表项 #${items.size + it + 1}" }
                            items = items + appended
                            state.finish(noMoreData = items.size >= 30)
                        }
                    }
                    // 刻意不传 noMoreDataText：终态 UI 整体隐藏
                ) {
                    DemoItemList(items)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 3：阻尼系数（stickinessLevel）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun StickinessDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }
    val stickinessOptions = listOf(0.2f, 0.5f, 0.8f)

    RefreshDemoScaffold(title = "阻尼系数", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "阻尼系数：", fontSize = 14.sp, color = Color(0xFF333333))
                stickinessOptions.forEach { option ->
                    val selected = state.stickinessLevel == option
                    Text(
                        text = "$option",
                        fontSize = 14.sp,
                        color = if (selected) Color.White else Color(0xFF2196F3),
                        modifier = Modifier
                            .background(if (selected) Color(0xFF2196F3) else Color(0xFFE3F2FD))
                            .clickable { state.stickinessLevel = option }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = "阻尼越小越\"沉\"：0.2 时手指滑 100px，Header 仅跟随 20px（仅作用于下拉刷新，上拉加载为 1:1 跟手）",
                fontSize = 12.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                SkyRefreshLayout(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onRefresh = {
                        scope.launch {
                            delay(1500)
                            items = items.shuffled()
                            state.finish()
                        }
                    }
                ) {
                    DemoItemList(items)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 4：横向滑动（orientation = Horizontal）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun HorizontalRefreshDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState()
    var items by remember { mutableStateOf(List(8) { "卡片 #${it + 1}" }) }

    RefreshDemoScaffold(title = "横向滑动", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            orientation = Orientation.Horizontal,
            onRefresh = {
                scope.launch {
                    delay(1500)
                    items = List(8) { "卡片 #${it + 1}" }
                    state.finish()
                }
            },
            onLoadMore = {
                scope.launch {
                    delay(1500)
                    val appended = List(4) { "卡片 #${items.size + it + 1}" }
                    items = items + appended
                    state.finish(noMoreData = items.size >= 20)
                }
            },
            noMoreDataText = "到底啦"
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it }) { text ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(140.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = text, fontSize = 15.sp, color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 5：自定义 Header / Footer
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun DemoCustomHeader(flag: SkyRefreshFlag) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (flag) {
                SkyRefreshFlag.IDLE -> "继续下拉"
                SkyRefreshFlag.PULLING -> "松开立即刷新"
                SkyRefreshFlag.REFRESHING -> "正在刷新…"
                SkyRefreshFlag.FINISHING -> "刷新完成"
            },
            fontSize = 14.sp,
            color = Color(0xFF1976D2)
        )
    }
}

@Composable
private fun DemoCustomFooter(flag: SkyRefreshFlag, noMoreData: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFFFF3E0)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                noMoreData -> "— 已经到底啦 —"
                flag == SkyRefreshFlag.REFRESHING -> "正在加载…"
                flag == SkyRefreshFlag.PULLING -> "继续上拉加载"
                else -> "上拉加载更多"
            },
            fontSize = 13.sp,
            color = Color(0xFFEF6C00)
        )
    }
}

@Composable
fun CustomIndicatorDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState()
    var items by remember { mutableStateOf(List(10) { "列表项 #${it + 1}" }) }

    RefreshDemoScaffold(title = "自定义指示器", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            header = { DemoCustomHeader(flag = state.refreshFlag) },
            footer = { DemoCustomFooter(flag = state.loadMoreFlag, noMoreData = state.noMoreData) },
            onRefresh = {
                scope.launch {
                    delay(1500)
                    items = List(10) { "列表项 #${it + 1}" }
                    state.finish()
                }
            },
            onLoadMore = {
                scope.launch {
                    delay(1500)
                    val appended = List(5) { "列表项 #${items.size + it + 1}" }
                    items = items + appended
                    state.finish(noMoreData = items.size >= 30)
                }
            }
        ) {
            DemoItemList(items)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 6：程序化触发（autoRefresh / autoLoadMore / 开关）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun AutoTriggerDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState()
    var items by remember { mutableStateOf(List(10) { "列表项 #${it + 1}" }) }

    RefreshDemoScaffold(title = "程序化触发", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 程序化触发按钮：内部会派发与手势一致的 onRefresh / onLoadMore 回调
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { scope.launch { state.autoRefresh() } }) {
                    Text(text = "autoRefresh()")
                }
                Button(onClick = { scope.launch { state.autoLoadMore() } }) {
                    Text(text = "autoLoadMore()")
                }
            }
            // 全局开关动态切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "下拉刷新", fontSize = 14.sp, color = Color(0xFF333333))
                Switch(
                    checked = state.enableRefresh,
                    onCheckedChange = { state.enableRefresh = it },
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "上拉加载",
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 24.dp)
                )
                Switch(
                    checked = state.enableLoadMore,
                    onCheckedChange = { state.enableLoadMore = it },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SkyRefreshLayout(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onRefresh = {
                        scope.launch {
                            delay(1500)
                            items = List(10) { "列表项 #${it + 1}" }
                            state.finish()
                        }
                    },
                    onLoadMore = {
                        scope.launch {
                            delay(1500)
                            val appended = List(5) { "列表项 #${items.size + it + 1}" }
                            items = items + appended
                            state.finish(noMoreData = items.size >= 30)
                        }
                    }
                ) {
                    DemoItemList(items)
                }
            }
        }
    }
}
