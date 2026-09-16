package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.ratingbar.SkyRatingBar
import com.sky.widget.ratingbar.SkyRatingIcon
import com.sky.widget.samplecp.ratingbar.RatingUiIntent
import com.sky.widget.samplecp.ratingbar.RatingUiState
import com.sky.widget.samplecp.ratingbar.RatingViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * 评分条（SkyRatingBar）示例页（SkyMVI 改造版）。
 *
 * 各示例卡片的分值由 [RatingViewModel] 持有并通过 [SkyMviScreen] 下发，
 * 点击 / 滑动评分条后通过 intent 回传 ViewModel 更新。
 *
 * 演示：基础用法、半星用法、只读状态、自定义图标（Vector / iconfont）、自定义样式及各自的半星组合。
 */
@Composable
fun RatingBarDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<RatingViewModel>(),
    ) { state, intent ->
        RatingBarDemoContent(state = state, intent = intent, onBack = onBack)
    }
}

@Composable
private fun RatingBarDemoContent(
    state: RatingUiState,
    intent: (RatingUiIntent) -> Unit,
    onBack: () -> Unit,
) {
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
                text = "评分条示例",
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
            // 示例 1：基础用法（默认内置五角星，整星评分）
            DemoCard(title = "基础用法") {
                RatingItem(rating = state.basicRating) {
                    SkyRatingBar(
                        value = state.basicRating,
                        onChange = { intent(RatingUiIntent.UpdateBasic(it)) }
                    )
                }
            }

            // 示例 2：半星用法（allowHalf 开启 0.5 步进）
            DemoCard(title = "半星用法") {
                RatingItem(rating = state.halfRating) {
                    SkyRatingBar(
                        value = state.halfRating,
                        allowHalf = true,
                        onChange = { intent(RatingUiIntent.UpdateHalf(it)) }
                    )
                }
            }

            // 示例 3：只读状态（禁用点击与滑动，仅展示）
            DemoCard(title = "只读状态") {
                RatingItem(rating = 3f) {
                    SkyRatingBar(
                        value = 3f,
                        readOnly = true,
                        onChange = {}
                    )
                }
            }

            // 示例 4：自定义图标（Vector，实心 / 描边爱心）
            DemoCard(title = "自定义图标（Vector）") {
                RatingItem(rating = state.vectorIconRating) {
                    SkyRatingBar(
                        value = state.vectorIconRating,
                        size = DpSize(24.dp, 24.dp),
                        activeColor = Color(0xFFE91E63),
                        activeIcon = SkyRatingIcon.Vector(Icons.Filled.Favorite),
                        normalIcon = SkyRatingIcon.Vector(Icons.Filled.FavoriteBorder),
                        onChange = { intent(RatingUiIntent.UpdateVectorIcon(it)) }
                    )
                }
            }

            // 示例 5：自定义图标（iconfont 勾选 / 未勾选图标）
            DemoCard(title = "自定义图标（iconfont）") {
                RatingItem(rating = state.customIconRating) {
                    SkyRatingBar(
                        value = state.customIconRating,
                        size = DpSize(24.dp, 24.dp),
                        activeIcon = SkyRatingIcon.Iconfont("skygouxuanzhong"),
                        normalIcon = SkyRatingIcon.Iconfont("skygouweixuanzhong"),
                        onChange = { intent(RatingUiIntent.UpdateCustomIcon(it)) }
                    )
                }
            }

            // 示例 6：自定义图标（iconfont 五角星）+ 半星
            DemoCard(title = "自定义图标（iconfont）半星用法") {
                RatingItem(rating = state.customIconHalfRating) {
                    SkyRatingBar(
                        value = state.customIconHalfRating,
                        size = DpSize(24.dp, 24.dp),
                        allowHalf = true,
                        activeIcon = SkyRatingIcon.Iconfont("skywujiaoxingxuanzhong"),
                        normalIcon = SkyRatingIcon.Iconfont("skywujiaoxingweixuanzhong"),
                        onChange = { intent(RatingUiIntent.UpdateCustomIconHalf(it)) }
                    )
                }
            }

            // 示例 7：自定义样式（颜色 / 尺寸 / 间距）
            DemoCard(title = "自定义样式") {
                RatingItem(rating = state.customStyleRating) {
                    SkyRatingBar(
                        value = state.customStyleRating,
                        size = DpSize(30.dp, 30.dp),
                        horizontalSpacing = 8.dp,
                        activeColor = Color(0xFFFF5252),
                        normalColor = Color(0xFFD7DDE4),
                        onChange = { intent(RatingUiIntent.UpdateCustomStyle(it)) }
                    )
                }
            }

            // 示例 8：自定义样式 + 半星
            DemoCard(title = "自定义样式半星用法") {
                RatingItem(rating = state.customStyleHalfRating) {
                    SkyRatingBar(
                        value = state.customStyleHalfRating,
                        size = DpSize(28.dp, 28.dp),
                        horizontalSpacing = 8.dp,
                        allowHalf = true,
                        activeColor = Color(0xFFFF9800),
                        normalColor = Color(0xFFD7DDE4),
                        onChange = { intent(RatingUiIntent.UpdateCustomStyleHalf(it)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 单个评分示例项：评分条 + 当前评分文案
 */
@Composable
private fun RatingItem(
    rating: Float,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
        Text(
            text = "当前评分：${formatRating(rating)}",
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
    }
}

/** 分值展示格式化：整数不显示小数位 */
private fun formatRating(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun RatingBarDemoScreenPreview() {
    SkyWidgetComposeTheme {
        RatingBarDemoScreen(onBack = {})
    }
}
