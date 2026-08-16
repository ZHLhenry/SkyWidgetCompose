package com.sky.widget.sample

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.grid.SkyGridLayout
import com.sky.widget.sample.grid.GridUiIntent
import com.sky.widget.sample.grid.GridViewModel
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme

/**
 * 网格布局示例页（SkyMVI 改造版）。
 *
 * 列数 / 间距 / 内边距与点击反馈由 [GridViewModel] 持有，通过 [SkyMviScreen] 下发；
 * 控制面板与网格渲染根据 UiState 重建。
 *
 * 演示：
 * - 基础 2 列网格
 * - 3 列商品网格（自定义 itemContent）
 * - 动态切换列数 / 间距 / 内边距
 */
@Composable
fun GridDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<GridViewModel>(),
    ) { state, intent ->
        GridDemoContent(state = state, intent = intent, onBack = onBack)
    }
}

@Composable
private fun GridDemoContent(
    state: com.sky.widget.sample.grid.GridUiState,
    intent: (GridUiIntent) -> Unit,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

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
                text = "网格布局示例",
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
            ControlPanel(
                columns = state.columns,
                onColumnsChange = { intent(GridUiIntent.SetColumns(it)) },
                horizontalSpacing = state.horizontalSpacing,
                onHorizontalSpacingChange = { intent(GridUiIntent.SetHorizontalSpacing(it)) },
                verticalSpacing = state.verticalSpacing,
                onVerticalSpacingChange = { intent(GridUiIntent.SetVerticalSpacing(it)) },
                padding = state.contentPadding,
                onPaddingChange = { intent(GridUiIntent.SetContentPadding(it)) }
            )

            state.lastClick?.let { click ->
                Text(
                    text = click,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 示例 1：基础色块网格
            DemoCard(title = "基础色块网格（columns = ${state.columns}）") {
                SkyGridLayout(
                    items = List(6) { it + 1 },
                    columns = state.columns,
                    horizontalSpacing = state.horizontalSpacing.dp,
                    verticalSpacing = state.verticalSpacing.dp,
                    contentPadding = PaddingValues(state.contentPadding.dp)
                ) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorForIndex(index))
                            .clickable {
                                val msg = "点击了色块 $index"
                                intent(GridUiIntent.ClickItem(msg))
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 示例 2：商品卡片网格
            DemoCard(title = "商品卡片网格") {
                val products = listOf(
                    Product("手机", "¥3999", Color(0xFF4CAF50)),
                    Product("耳机", "¥299", Color(0xFF2196F3)),
                    Product("手表", "¥1299", Color(0xFFFF9800)),
                    Product("平板", "¥2499", Color(0xFF9C27B0)),
                    Product("键盘", "¥199", Color(0xFFE91E63)),
                    Product("鼠标", "¥99", Color(0xFF00BCD4))
                )

                SkyGridLayout(
                    items = products,
                    columns = 3,
                    horizontalSpacing = 12.dp,
                    verticalSpacing = 12.dp,
                    contentPadding = PaddingValues(0.dp)
                ) { product ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                val msg = "点击了商品 ${product.name}"
                                intent(GridUiIntent.ClickItem(msg))
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(product.color)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = product.price,
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    horizontalSpacing: Int,
    onHorizontalSpacingChange: (Int) -> Unit,
    verticalSpacing: Int,
    onVerticalSpacingChange: (Int) -> Unit,
    padding: Int,
    onPaddingChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "属性设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Spacer(Modifier.height(12.dp))

            IntStepper(
                label = "列数",
                value = columns,
                min = 1,
                max = 5,
                onChange = onColumnsChange
            )
            Spacer(Modifier.height(8.dp))
            IntStepper(
                label = "水平间距",
                value = horizontalSpacing,
                min = 0,
                max = 32,
                onChange = onHorizontalSpacingChange
            )
            Spacer(Modifier.height(8.dp))
            IntStepper(
                label = "垂直间距",
                value = verticalSpacing,
                min = 0,
                max = 32,
                onChange = onVerticalSpacingChange
            )
            Spacer(Modifier.height(8.dp))
            IntStepper(
                label = "内边距",
                value = padding,
                min = 0,
                max = 32,
                onChange = onPaddingChange
            )
        }
    }
}

@Composable
private fun IntStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF666666))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { if (value > min) onChange(value - 1) },
                modifier = Modifier.size(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-")
            }
            Text(
                text = "${value}dp".takeIf { label != "列数" } ?: "$value",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            OutlinedButton(
                onClick = { if (value < max) onChange(value + 1) },
                modifier = Modifier.size(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
internal fun DemoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun colorForIndex(index: Int): Color = when (index % 6) {
    1 -> Color(0xFF2196F3)
    2 -> Color(0xFF4CAF50)
    3 -> Color(0xFFFF9800)
    4 -> Color(0xFFE91E63)
    5 -> Color(0xFF9C27B0)
    else -> Color(0xFF00BCD4)
}

private data class Product(val name: String, val price: String, val color: Color)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun GridDemoScreenPreview() {
    SkyWidgetComposeTheme {
        GridDemoScreen(onBack = {})
    }
}
