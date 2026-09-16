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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.verifycode.SkyVerifyCodeEdit
import com.sky.widget.verifycode.SkyVerifyType

/**
 * 验证码输入框（SkyVerifyCodeEdit）示例页。
 *
 * 演示：基本用法（下划线样式）、正方形用法、自定义大小、自定义数量、
 * 自定义线宽与自定义颜色，输满后 Toast 展示完整验证码。
 */
@Composable
fun VerifyCodeDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val showCompleteToast: (String) -> Unit = { code ->
        Toast.makeText(context, "输入完成：$code", Toast.LENGTH_SHORT).show()
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
                text = "验证码示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 示例 1：基本用法（默认 6 位下划线样式）
            DemoCard(title = "基本用法") {
                SkyVerifyCodeEdit(onComplete = showCompleteToast)
            }

            // 示例 2：正方形用法（方框样式）
            DemoCard(title = "正方形用法") {
                SkyVerifyCodeEdit(
                    type = SkyVerifyType.Square,
                    onComplete = showCompleteToast
                )
            }

            // 示例 3：自定义大小（单个格子尺寸）
            DemoCard(title = "自定义大小") {
                SkyVerifyCodeEdit(
                    size = DpSize(40.dp, 52.dp),
                    type = SkyVerifyType.Square,
                    onComplete = showCompleteToast
                )
            }

            // 示例 4：自定义数量（4 位验证码 + 加大格子）
            DemoCard(title = "自定义数量") {
                SkyVerifyCodeEdit(
                    count = 4,
                    size = DpSize(56.dp, 56.dp),
                    type = SkyVerifyType.Square,
                    onComplete = showCompleteToast
                )
            }

            // 示例 5：自定义线宽（下划线高度 + 光标宽度）
            DemoCard(title = "自定义线宽") {
                SkyVerifyCodeEdit(
                    lineHeight = 4.dp,
                    cursorLineWidth = 3.dp,
                    onComplete = showCompleteToast
                )
            }

            // 示例 6：自定义颜色（线条 / 数字 / 光标）
            DemoCard(title = "自定义颜色") {
                SkyVerifyCodeEdit(
                    activeLineColor = Color(0xFF2196F3),
                    normalLineColor = Color(0xFFBBDEFB),
                    textColor = Color(0xFF1565C0),
                    cursorLineColor = Color(0xFF2196F3),
                    onComplete = showCompleteToast
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun VerifyCodeDemoScreenPreview() {
    SkyWidgetComposeTheme {
        VerifyCodeDemoScreen(onBack = {})
    }
}
