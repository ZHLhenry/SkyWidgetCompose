package com.sky.widget.samplecp

import android.widget.Toast
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.samplecp.annotatedtext.AnnotatedTextUiIntent
import com.sky.widget.samplecp.annotatedtext.AnnotatedTextUiState
import com.sky.widget.samplecp.annotatedtext.AnnotatedTextViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.text.SkyAnnotatedAction
import com.sky.widget.text.SkyAnnotatedText

/**
 * 高亮文字示例页（SkyMVI 改造版）。
 *
 * 该页面主要演示 [SkyAnnotatedText] 的高亮与点击能力；
 * 点击高亮文本时派发 [AnnotatedTextUiIntent.AnnotationClick]，由 ViewModel 更新
 * 最近点击文案，同时弹出 Toast 反馈。
 *
 * 演示：正则高亮、点击回调。
 */
@Composable
fun AnnotatedTextDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<AnnotatedTextViewModel>(),
    ) { state, intent ->
        AnnotatedTextDemoContent(state = state, intent = intent, onBack = onBack)
    }
}

@Composable
private fun AnnotatedTextDemoContent(
    state: AnnotatedTextUiState,
    intent: (AnnotatedTextUiIntent) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 点击高亮文本：派发 intent 更新最近点击文案，同时 Toast 反馈
    val onAnnotationClick: (String) -> Unit = { matched ->
        intent(AnnotatedTextUiIntent.AnnotationClick(matched))
        Toast.makeText(context, "点击了：$matched", Toast.LENGTH_SHORT).show()
    }

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
                text = "高亮文字示例",
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
            state.lastClick?.let { click ->
                Text(
                    text = "最近点击：$click",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 示例 1：手机号 / 邮箱 / 链接高亮并点击
            DemoCard(title = "手机号 / 邮箱 / 链接高亮并点击") {
                val text =
                    "如有疑问请联系 13800138000 或邮箱 support@example.com，更多详情见 https://www.sky.com"
                SkyAnnotatedText(
                    text = text,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF333333),
                        lineHeight = 24.sp
                    ),
                    annotatedStyle = SpanStyle(
                        color = Color(0xFF2196F3),
                        textDecoration = TextDecoration.Underline
                    ),
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "(1[3-9]\\d{9})",
                            onClick = onAnnotationClick
                        ),
                        SkyAnnotatedAction(
                            regex = "\\w+@\\w+\\.\\w+",
                            onClick = onAnnotationClick
                        ),
                        SkyAnnotatedAction(
                            regex = "https?://[\\w./-]+",
                            onClick = onAnnotationClick
                        )
                    )
                )
            }

            // 示例 2：活动文案高亮
            DemoCard(title = "活动文案高亮（含点击）") {
                val text =
                    "【重要】活动将于 2026-08-20 截止，使用优惠码 SAVE20 立减，点击查看规则。"
                SkyAnnotatedText(
                    text = text,
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0xFF333333),
                        lineHeight = 24.sp
                    ),
                    annotatedStyle = SpanStyle(
                        color = Color(0xFFE91E63),
                        fontWeight = FontWeight.Bold
                    ),
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "SAVE20",
                            onClick = onAnnotationClick
                        )
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun AnnotatedTextDemoScreenPreview() {
    SkyWidgetComposeTheme {
        AnnotatedTextDemoScreen(onBack = {})
    }
}
