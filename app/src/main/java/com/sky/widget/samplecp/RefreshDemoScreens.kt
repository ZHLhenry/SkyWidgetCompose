package com.sky.widget.samplecp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.refresh.SkyRefreshFlag
import com.sky.widget.refresh.SkyRefreshLayout
import com.sky.widget.refresh.SkyRefreshState
import com.sky.widget.refresh.SkyRefreshStyle
import com.sky.widget.refresh.header.SkyBallRefreshHeader
import com.sky.widget.refresh.header.SkyCircleRefreshHeader
import com.sky.widget.refresh.header.SkyLottieRefreshHeader
import com.sky.widget.refresh.header.SkyProgressRefreshHeader
import com.sky.widget.grid.SkyGridLayout
import com.sky.widget.refresh.header.SkyTimeRefreshHeader
import com.sky.widget.refresh.header.SkyTwoLevelRefreshHeader
import com.sky.widget.refresh.rememberSkyRefreshState
import com.sky.widget.samplecp.refresh.RefreshUiIntent
import com.sky.widget.samplecp.refresh.RefreshViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
    RefreshDemoEntry("球状加载 Header", "库内置 SkyBallRefreshHeader（三球缩放动画）", Screen.BallHeader),
    RefreshDemoEntry("Lottie Header", "库内置 SkyLottieRefreshHeader（Lottie 动画）", Screen.LottieHeader),
    RefreshDemoEntry("FixedContent 样式", "内容固定 + SkyCircleRefreshHeader 圆圈滑入覆盖", Screen.FixedContent),
    RefreshDemoEntry("FixedFront 样式", "辉光+进度条固定在前面，下拉进度填充", Screen.FixedFront),
    RefreshDemoEntry("时间文案 Header", "SkyTimeRefreshHeader：箭头+状态文案+最后更新时间，文案/格式可同名资源覆盖", Screen.TimeHeader),
    RefreshDemoEntry("下拉进入二楼", "secondFloorRate + onSecondFloor：拉过二级阈值松手打开二楼", Screen.SecondFloor),
    RefreshDemoEntry("自定义二楼 Header", "不用库内组件：业务自定义 Header + 二楼进度渐变动画", Screen.CustomSecondFloor),
    RefreshDemoEntry("网格内容", "SkyGridLayout 作为刷新容器的内容主体，支持下拉刷新与上拉加载", Screen.RefreshGrid),
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
private fun DemoItemList(
    items: List<String>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
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
                    delay(1500.milliseconds)
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
                    delay(1500.milliseconds)
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
// 示例 3：无终态提示（noMoreDataText 不传）
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
                            delay(1500.milliseconds)
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
// 示例 4：阻尼系数（stickinessLevel）
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
                            delay(1500.milliseconds)
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
// 示例 5：横向滑动（orientation = Horizontal）
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
                    delay(1500.milliseconds)
                    items = List(8) { "卡片 #${it + 1}" }
                    state.finish()
                }
            },
            onLoadMore = {
                scope.launch {
                    delay(1500.milliseconds)
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
// 示例 6：自定义 Header / Footer
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
                    delay(1500.milliseconds)
                    items = List(10) { "列表项 #${it + 1}" }
                    state.finish()
                }
            },
            onLoadMore = {
                scope.launch {
                    delay(1500.milliseconds)
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
// 示例 7：程序化触发（autoRefresh / autoLoadMore / 开关）
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
                            delay(1500.milliseconds)
                            items = List(10) { "列表项 #${it + 1}" }
                            state.finish()
                        }
                    },
                    onLoadMore = {
                        scope.launch {
                            delay(1500.milliseconds)
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

// ─────────────────────────────────────────────────────────────────────
// 示例 8：球状加载 Header（库内置 SkyBallRefreshHeader，三球缩放动画效果）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun BallHeaderDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    // 必须等新数据重组应用后再回滚：在 onRefresh 里 shuffle 后立刻 scrollToItem，
    // 会被重组时 LazyColumn 按 key 锚定首项的逻辑覆盖掉
    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "球状加载 Header", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            header = { SkyBallRefreshHeader(flag = state.refreshFlag) },
            onRefresh = {
                scope.launch {
                    delay(1500.milliseconds)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items, listState = listState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 9：Lottie Header（库内置 SkyLottieRefreshHeader，Lottie 动画效果）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun LottieHeaderDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "Lottie Header", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            header = { SkyLottieRefreshHeader(flag = state.refreshFlag) },
            onRefresh = {
                scope.launch {
                    delay(1500.milliseconds)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items, listState = listState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 10：FixedContent 样式：
// 下拉时内容固定不动，官方圆圈风格 Header（SkyCircleRefreshHeader）从顶部滑入并覆盖在内容之上
// ─────────────────────────────────────────────────────────────────────

@Composable
fun FixedContentDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "FixedContent 样式", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            style = SkyRefreshStyle.FixedContent,
            header = { SkyCircleRefreshHeader(state) },
            onRefresh = {
                scope.launch {
                    delay(1500.milliseconds)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items, listState = listState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 11：FixedFront 样式：
// 辉光+线性进度条 Header（SkyProgressRefreshHeader）固定在内容前方原位，
// 下拉时位置不动，辉光渐入、进度条随下拉距离填充，刷新时变为无限滚动进度条
// ─────────────────────────────────────────────────────────────────────

@Composable
fun FixedFrontDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "FixedFront 样式", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            style = SkyRefreshStyle.FixedFront,
            header = { SkyProgressRefreshHeader(state) },
            onRefresh = {
                scope.launch {
                    delay(4000.milliseconds)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items, listState = listState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 12：时间文案 Header（库内置 SkyTimeRefreshHeader）：
// 箭头 + 状态文案 + 最后更新时间；文案与时间格式支持消费者同名资源覆盖
// ─────────────────────────────────────────────────────────────────────

@Composable
fun TimeHeaderDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "时间文案 Header", onBack = onBack) {
        SkyRefreshLayout(
            modifier = Modifier.fillMaxSize(),
            state = state,
            header = { SkyTimeRefreshHeader(state) },
            onRefresh = {
                scope.launch {
                    delay(1500.milliseconds)
                    items = items.shuffled()
                    state.finish()
                }
            }
        ) {
            DemoItemList(items, listState = listState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 13：下拉进入二楼：
// 下拉过刷新阈值显示“释放立即刷新”，继续拉过二级阈值（headerBound × secondFloorRate）
// 显示“释放进入二楼”，松手打开二楼覆盖层而不触发刷新
// ─────────────────────────────────────────────────────────────────────

/**
 * 二楼覆盖层（各二楼示例共用）：进出场沿用下拉方向滑入/滑出，
 * 根节点 clickable 消费触摸，防止穿透到底层列表误触发下拉。
 */
@Composable
private fun DemoSecondFloorOverlay(open: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1B2431))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "欢迎来到二楼", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                text = "这里是业务自定义的二楼内容（活动页 / 广告位 / 小游戏…）",
                fontSize = 13.sp,
                color = Color(0xFF9AA3B2),
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onClose,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(text = "返回一楼")
            }
        }
    }
}

@Composable
fun SecondFloorDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }
    var secondFloorOpen by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "下拉进入二楼", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            SkyRefreshLayout(
                modifier = Modifier.fillMaxSize(),
                state = state,
                secondFloorRate = 2f,
                onSecondFloor = { secondFloorOpen = true },
                header = { SkyTwoLevelRefreshHeader(state, secondFloorRate = 2f) },
                onRefresh = {
                    scope.launch {
                        delay(1500.milliseconds)
                        items = items.shuffled()
                        state.finish()
                    }
                }
            ) {
                DemoItemList(items, listState = listState)
            }

            DemoSecondFloorOverlay(open = secondFloorOpen, onClose = { secondFloorOpen = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 14：自定义 Header 实现下拉进二楼（不使用库内 SkyTwoLevelRefreshHeader）。
//
// 要点：二楼触发逻辑在容器层（secondFloorRate + onSecondFloor），与 Header 解耦；
// 自定义 Header 只需：1) 自声明主轴高度（即刷新阈值）；2) 内部不做位移；
// 3) 用 state.indicatorOffset 与「自身高度 × rate」比较，自行切换“释放进入二楼”提示。
// headerBound 为库 internal（模块外不可读），但阈值恒等于自身声明的高度，自行换算即可。
// ─────────────────────────────────────────────────────────────────────

private val CustomFloorHeaderHeight = 64.dp
private const val CustomSecondFloorRate = 2f

/** 业务自定义的二楼 Header：背景浓度、字号、字重随“二楼进度”渐变。 */
@Composable
private fun DemoCustomFloorHeader(state: SkyRefreshState) {
    val headerHeightPx = with(LocalDensity.current) { CustomFloorHeaderHeight.toPx() }
    val offset = state.indicatorOffset.coerceAtLeast(0f)
    val flag = state.refreshFlag
    val overTrigger = offset >= headerHeightPx
    val overSecondFloor = offset >= headerHeightPx * CustomSecondFloorRate
    // 0~1 的二楼进度，用于驱动渐变动画
    val floorProgress = (offset / (headerHeightPx * CustomSecondFloorRate)).coerceIn(0f, 1f)

    val text = when {
        flag == SkyRefreshFlag.REFRESHING -> "正在刷新…"
        overSecondFloor -> "松手进入二楼！"
        overTrigger -> "松手刷新，继续下拉进二楼"
        else -> "下拉开始刷新"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CustomFloorHeaderHeight)
            .background(Color(0xFF673AB7).copy(alpha = 0.08f + 0.5f * floorProgress)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = (14 + 4 * floorProgress).sp,
            color = if (overSecondFloor) Color.White else Color(0xFF673AB7),
            fontWeight = if (overSecondFloor) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.graphicsLayer {
                val scale = 0.85f + 0.15f * floorProgress
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
fun CustomSecondFloorDemoScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState(enableLoadMore = false)
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }
    var secondFloorOpen by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        listState.scrollToItem(0)
    }

    RefreshDemoScaffold(title = "自定义二楼 Header", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            SkyRefreshLayout(
                modifier = Modifier.fillMaxSize(),
                state = state,
                secondFloorRate = CustomSecondFloorRate,
                onSecondFloor = { secondFloorOpen = true },
                header = { DemoCustomFloorHeader(state) },
                onRefresh = {
                    scope.launch {
                        delay(1500.milliseconds)
                        items = items.shuffled()
                        state.finish()
                    }
                }
            ) {
                DemoItemList(items, listState = listState)
            }

            DemoSecondFloorOverlay(open = secondFloorOpen, onClose = { secondFloorOpen = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 示例 15：网格内容（SkyGridLayout 作为刷新容器的内容主体）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun GridRefreshDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<RefreshViewModel>(),
    ) { state, intent ->
        val refreshState = rememberSkyRefreshState()

        LaunchedEffect(state.isRefreshing) {
            if (!state.isRefreshing) refreshState.finish()
        }
        LaunchedEffect(state.isLoadingMore) {
            if (!state.isLoadingMore) refreshState.finish(noMoreData = state.items.size >= 45)
        }

        RefreshDemoScaffold(title = "网格内容", onBack = onBack) {
            SkyRefreshLayout(
                modifier = Modifier.fillMaxSize(),
                state = refreshState,
                onRefresh = { intent(RefreshUiIntent.Refresh) },
                onLoadMore = { intent(RefreshUiIntent.LoadMore) },
                noMoreDataText = "没有更多商品啦"
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item(key = "intro") {
                        Text(
                            text = "下面使用 SkyGridLayout 展示商品网格，支持下拉刷新与上拉加载。",
                            fontSize = 13.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item(key = "grid") {
                        SkyGridLayout(
                            items = state.items.indices.toList(),
                            columns = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalSpacing = 8.dp,
                            verticalSpacing = 8.dp
                        ) { index ->
                            GridProductCard(index = index + 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridProductCard(index: Int) {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
        Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
        Color(0xFF29B6F6), Color(0xFF26C6DA), Color(0xFF26A69A),
        Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157)
    )
    val color = colors.getOrElse((index - 1) % colors.size) { Color(0xFF2196F3) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "商品 $index",
            fontSize = 13.sp,
            color = Color(0xFF333333),
            maxLines = 1
        )
        Text(
            text = "¥${index * 10}",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
