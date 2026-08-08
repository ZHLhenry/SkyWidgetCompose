package com.sky.widget.sample

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
import androidx.compose.material3.MaterialTheme
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
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.text.SkyAnnotatedAction
import com.sky.widget.text.SkyAnnotatedText

/**
 * 高亮文字示例页。
 *
 * 演示：
 * - 指定文字高亮并点击（如隐私政策）
 * - 手机号样式自动识别并点击
 * - 邮箱样式自动识别并点击
 */
@Composable
fun AnnotatedTextDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
            // 指定文字
            DemoCard(title = "指定文字") {
                SkyAnnotatedText(
                    text = "我已阅读并同意《隐私政策》",
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "《隐私政策》",
                            onClick = {
                                Toast.makeText(context, "点击：$it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                )
            }

            // 手机号样式
            DemoCard(title = "手机号样式") {
                SkyAnnotatedText(
                    text = "您的订单已代收，如有疑问您可以联系配送员【张三，18710220022】确认，感谢您购物，欢迎再次光临。",
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "1[3-9]\\d{9}",
                            onClick = {
                                Toast.makeText(context, "拨打电话：$it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                )
            }

            // 邮箱样式
            DemoCard(title = "邮箱样式") {
                SkyAnnotatedText(
                    text = "邮件已发送至您的邮箱：549226148@qq.com。",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF333333)
                    ),
                    annotatedStyle = SpanStyle(
                        color = Color(0xFFE53980),
                        textDecoration = TextDecoration.Underline
                    ),
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                            onClick = {
                                Toast.makeText(context, "发送邮件：$it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                )
            }

            // 多规则同时匹配
            DemoCard(title = "多规则同时匹配") {
                SkyAnnotatedText(
                    text = "请致电 18710220022 或发送邮件至 service@sky.com，《隐私政策》详情请查看。",
                    annotatedActions = listOf(
                        SkyAnnotatedAction(
                            regex = "1[3-9]\\d{9}",
                            onClick = {
                                Toast.makeText(context, "电话：$it", Toast.LENGTH_SHORT).show()
                            }
                        ),
                        SkyAnnotatedAction(
                            regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                            onClick = {
                                Toast.makeText(context, "邮箱：$it", Toast.LENGTH_SHORT).show()
                            }
                        ),
                        SkyAnnotatedAction(
                            regex = "《隐私政策》",
                            onClick = {
                                Toast.makeText(context, "协议：$it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                )
            }
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
