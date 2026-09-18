package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.swipe.SkySwipeMenu
import com.sky.widget.swipe.SwipeDirection
import com.sky.widget.swipe.SwipeValue
import com.sky.widget.swipe.rememberSwipeState
import kotlinx.coroutines.launch

/**
 * 侧滑菜单（SkySwipeMenu）完整 API 示例页。
 *
 * 逐节演示以下能力：
 * 1. 基本用法（content / background，默认 RightToLeft 右侧菜单）；
 * 2. 菜单方向（direction = LeftToRight，左侧菜单）；
 * 3. 打开判定阈值（threshold）；
 * 4. 状态控制与回调（rememberSwipeState：open / close / isOpen / currentValue / onChange）；
 * 5. 列表场景（LazyColumn 多行各自独立 state，背景删除按钮移除数据项）。
 */
@Composable
fun SwipeMenuDemoScreen(onBack: () -> Unit) {
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
                text = "侧滑菜单示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            SwipeBasicSection()
            SwipeDirectionSection()
            SwipeThresholdSection()
            SwipeStateSection()
            SwipeListSection()
        }
    }
}

// ---------------------------------------------------------------------------
// 1. 基本用法（默认右侧菜单）
// ---------------------------------------------------------------------------

/**
 * 基本用法：content 为前景行，background 为右侧操作菜单；向左滑动内容即露出菜单。
 */
@Composable
private fun SwipeBasicSection() {
    SwipeSectionTitle("1. 基本用法（content / background，默认右菜单）")
    SkySwipeMenu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        background = {
            // 背景菜单：两个等宽操作按钮，其总宽度即完全打开时内容让出的距离。
            Row {
                SwipeActionButton("置顶", Color(0xFF2196F3)) {}
                SwipeActionButton("删除", Color(0xFFE53935)) {}
            }
        },
        content = {
            SwipeDemoRow("基本用法", "向左滑动查看右侧菜单")
        },
    )
}

// ---------------------------------------------------------------------------
// 2. 菜单方向（左侧菜单）
// ---------------------------------------------------------------------------

/**
 * 菜单方向：direction = LeftToRight 时菜单在左侧，需向右滑动内容露出。
 */
@Composable
private fun SwipeDirectionSection() {
    SwipeSectionTitle("2. 菜单方向（direction = LeftToRight）")
    SkySwipeMenu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        direction = SwipeDirection.LeftToRight,
        background = {
            Row {
                SwipeActionButton("已读", Color(0xFF4CAF50)) {}
            }
        },
        content = {
            SwipeDemoRow("左侧菜单", "向右滑动查看左侧菜单")
        },
    )
}

// ---------------------------------------------------------------------------
// 3. 打开判定阈值（threshold）
// ---------------------------------------------------------------------------

/**
 * 打开判定阈值：threshold 越小越容易打开（松手时进度超过 宽度 * threshold 即吸附打开）。
 * 此处用 0.1（轻滑即开）与 0.6（需滑过大半）对比手感。
 */
@Composable
private fun SwipeThresholdSection() {
    SwipeSectionTitle("3. 打开判定阈值（threshold = 0.1 / 0.6）")
    SkySwipeMenu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        threshold = 0.1f,
        background = { Row { SwipeActionButton("易开", Color(0xFF9C27B0)) {} } },
        content = { SwipeDemoRow("threshold = 0.1", "轻滑即吸附打开") },
    )
    SkySwipeMenu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        threshold = 0.6f,
        background = { Row { SwipeActionButton("难开", Color(0xFFFF9800)) {} } },
        content = { SwipeDemoRow("threshold = 0.6", "需滑过大半才打开") },
    )
}

// ---------------------------------------------------------------------------
// 4. 状态控制与回调
// ---------------------------------------------------------------------------

/**
 * 状态控制：rememberSwipeState 提供 open()/close() 代码开合、isOpen/currentValue 状态查询；
 * onChange 在开合状态变化时回调（含首次组合）。
 */
@Composable
private fun SwipeStateSection() {
    val scope = rememberCoroutineScope()
    val swipeState = rememberSwipeState(initialValue = SwipeValue.Hidden)
    var changeTip by remember { mutableStateOf("尚未回调") }

    SwipeSectionTitle("4. 状态控制与回调（state / onChange）")
    SkySwipeMenu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        state = swipeState,
        onChange = { open -> changeTip = "onChange：open = $open" },
        background = { Row { SwipeActionButton("操作", Color(0xFF00BCD4)) {} } },
        content = { SwipeDemoRow("状态控制", "用下方按钮代码开合") },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "isOpen = ${swipeState.isOpen}；currentValue = ${swipeState.currentValue}；$changeTip",
            fontSize = 13.sp,
            color = Color(0xFF2196F3),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 代码打开（带动画）。
            OutlinedButton(onClick = { scope.launch { swipeState.open() } }) { Text("open()") }
            // 代码关闭（带动画）。
            OutlinedButton(onClick = { scope.launch { swipeState.close() } }) { Text("close()") }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. 列表场景（多行独立 state + 删除数据项）
// ---------------------------------------------------------------------------

/**
 * 列表场景：LazyColumn 中每行各自 rememberSwipeState，互不影响；
 * 背景“删除”按钮点击后从数据源移除该项，实现常见消息列表侧滑删除。
 */
@Composable
private fun SwipeListSection() {
    val items = remember { mutableStateListOf("消息 1", "消息 2", "消息 3", "消息 4") }

    SwipeSectionTitle("5. 列表场景（多行独立 state + 侧滑删除）")
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = items, key = { it }) { item ->
            // 每行独立 state，保证各行开合互不干扰。
            val rowState = rememberSwipeState()
            val rowScope = rememberCoroutineScope()
            SkySwipeMenu(
                modifier = Modifier.fillMaxWidth(),
                state = rowState,
                background = {
                    Row {
                        SwipeActionButton("删除", Color(0xFFE53935)) {
                            // 关闭菜单并从数据源移除该项。
                            rowScope.launch { rowState.close() }
                            items.remove(item)
                        }
                    }
                },
                content = {
                    SwipeDemoRow(item, "左滑可删除本行")
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 通用小组件
// ---------------------------------------------------------------------------

/** 示例小节标题。 */
@Composable
private fun SwipeSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF333333),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** 前景内容行：白底卡片 + 标题 + 副标题。 */
@Composable
private fun SwipeDemoRow(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, fontSize = 15.sp, color = Color(0xFF222222))
        Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF999999))
    }
}

/** 背景操作按钮：固定宽度、铺满高度、纯色底。 */
@Composable
private fun SwipeActionButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun SwipeMenuDemoPreview() {
    SkyWidgetComposeTheme {
        SwipeMenuDemoScreen(onBack = {})
    }
}
