package com.sky.widget.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.refresh.SkyRefreshLayout
import com.sky.widget.refresh.rememberSkyRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 刷新组件完整用法示例页面。
 *
 * 用简单文字列表演示：
 * - 下拉刷新 [SkyRefreshLayout.onRefresh]
 * - 上拉加载更多 [SkyRefreshLayout.onLoadMore]
 * - 无更多数据终态提示 [noMoreDataText]
 */
@Composable
fun RefreshSampleScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState()

    // 列表数据：模拟从网络加载的文字项
    var items by remember { mutableStateOf(List(15) { "列表项 #${it + 1}" }) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏（含返回按钮），背景延伸到状态栏区域（沉浸式）
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
                text = "刷新示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        SkyRefreshLayout(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            state = state,
            onRefresh = {
                android.util.Log.d("RefreshSample", "onRefresh triggered, refreshFlag=${state.refreshFlag}")
                scope.launch {
                    delay(3000)
                    items = List(15) { "列表项 #${it + 1}" }
                    state.finish()
                }
            },
            onLoadMore = {
                scope.launch {
                    delay(3000)
                    if (items.size >= 45) {
                        state.finish(noMoreData = true)
                    } else {
                        val appended = List(10) { "列表项 #${items.size + it + 1}" }
                        items = items + appended
                        // 追加后立即判断是否到达数据末尾，达到 45 条直接终态停靠，无需再拉一次
                        state.finish(noMoreData = items.size >= 45)
                    }
                }
            },
            noMoreDataText = "没有更多数据了"
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items) { text ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 15.sp,
                            color = Color(0xFF333333)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RefreshSampleScreenPreview() {
    SkyWidgetComposeTheme {
        RefreshSampleScreen(onBack = {})
    }
}
