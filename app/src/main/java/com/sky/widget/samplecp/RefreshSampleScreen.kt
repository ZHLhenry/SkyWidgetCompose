package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.refresh.SkyRefreshLayout
import com.sky.widget.refresh.rememberSkyRefreshState
import com.sky.widget.samplecp.refresh.RefreshUiIntent
import com.sky.widget.samplecp.refresh.RefreshViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * 刷新组件完整用法示例页面（SkyMVI 改造版）。
 *
 * 列表数据由 [RefreshViewModel] 持有，UI 通过 [SkyMviScreen] 接收
 * [RefreshUiState] 与 intent 派发入口；下拉刷新 / 上拉加载更多的
 * "完成信号"由 SkyRefresh 库自身的 [SkyRefreshState.finish] 负责，
 * 这里根据 UiState 中的 isRefreshing / isLoadingMore 联动触发。
 */
@Composable
fun RefreshSampleScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<RefreshViewModel>(),
    ) { state, intent ->
        val refreshState = rememberSkyRefreshState()

        // 刷新完成：isRefreshing 由 true -> false 时通知 SkyRefresh 收起 Header
        LaunchedEffect(state.isRefreshing) {
            if (!state.isRefreshing) refreshState.finish()
        }
        // 加载完成：isLoadingMore 由 true -> false 时通知 SkyRefresh 收起 Footer
        LaunchedEffect(state.isLoadingMore) {
            if (!state.isLoadingMore) refreshState.finish(noMoreData = state.items.size >= 45)
        }

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
                state = refreshState,
                onRefresh = { intent(RefreshUiIntent.Refresh) },
                onLoadMore = { intent(RefreshUiIntent.LoadMore) },
                noMoreDataText = "没有更多数据了"
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.items) { text ->
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
}

@Preview(showBackground = true)
@Composable
fun RefreshSampleScreenPreview() {
    SkyWidgetComposeTheme {
        RefreshSampleScreen(onBack = {})
    }
}
