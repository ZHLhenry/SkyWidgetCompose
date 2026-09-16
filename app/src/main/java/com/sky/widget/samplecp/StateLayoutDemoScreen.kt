package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.samplecp.statelayout.StateLayoutUiIntent
import com.sky.widget.samplecp.statelayout.StateLayoutViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.stateLayout.SkyEmptyWidget
import com.sky.widget.stateLayout.SkyErrorWidget
import com.sky.widget.stateLayout.SkyPageState
import com.sky.widget.stateLayout.SkyPageStateLayout

/**
 * 页面状态布局示例页（SkyMVI 改造版）。
 *
 * 当前 [SkyPageState] 与列表数据由 [StateLayoutViewModel] 持有，
 * 通过 [SkyMviScreen] 下发；手动切换 / 重新加载意图派发给 ViewModel 处理。
 *
 * 演示：
 * - 基础用法：根据 [SkyPageState] 自动切换 Loading / Success / Empty / Error
 * - 顶部按钮手动切换 4 种状态
 * - 点击「重新加载」模拟网络请求并随机进入 Success / Empty / Error
 * - 自定义空 / 错误占位图标
 */
@Composable
fun StateLayoutDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<StateLayoutViewModel>(),
    ) { uiState, intent ->
        val pageState = uiState.pageState

        fun loadData() = intent(StateLayoutUiIntent.Retry)

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
                    text = "页面状态示例",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 状态切换工具栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "手动切换状态",
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StateButton(
                        text = "Loading",
                        modifier = Modifier.weight(1f),
                        onClick = { intent(StateLayoutUiIntent.ToLoading) }
                    )
                    StateButton(
                        text = "Success",
                        modifier = Modifier.weight(1f),
                        onClick = { intent(StateLayoutUiIntent.ToSuccess) }
                    )
                    StateButton(
                        text = "Empty",
                        modifier = Modifier.weight(1f),
                        onClick = { intent(StateLayoutUiIntent.ToEmpty) }
                    )
                    StateButton(
                        text = "Error",
                        modifier = Modifier.weight(1f),
                        onClick = { intent(StateLayoutUiIntent.ToError) }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

            // 页面状态布局主体
            SkyPageStateLayout(
                pageState = pageState,
                modifier = Modifier.weight(1f),
                empty = { message ->
                    SkyEmptyWidget(
                        image = {
                            Icon(
                                imageVector = Icons.Outlined.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        },
                        message = message.ifEmpty { "暂无数据" },
                        buttonText = "重新加载",
                        onRetry = { loadData() }
                    )
                },
                error = { message ->
                    SkyErrorWidget(
                        image = {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        message = message,
                        buttonText = "重新加载",
                        onRetry = { loadData() }
                    )
                }
            ) {
                val items = (1..15).map { "列表项 #$it" }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items, key = { it }) { text ->
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

@Composable
private fun StateButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun StateLayoutDemoScreenPreview() {
    SkyWidgetComposeTheme {
        StateLayoutDemoScreen(onBack = {})
    }
}
