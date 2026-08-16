package com.sky.widget.sample

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.grid.SkyGridLayout
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iconfont.SkyIconFontsLib

/**
 * 图标字体示例页
 *
 * 演示 sky-iconfont 组件：通过 SkyIconFontsLib 注册多个字体（assets/fonts 下），
 * 按字体名称解析并渲染图标。页面顶部列出所有已注册字体并可切换，
 * 验证多字体并存与按需指定字体的能力。
 * 字体未初始化时直接抛出异常，由调用方保证初始化。
 */
@Composable
fun IconFontDemoScreen(onBack: () -> Unit) {
    // 未初始化时 SkyIconFontsLib 内部会直接抛出 IllegalArgumentException
    val fonts = remember { SkyIconFontsLib.getRegisteredFonts() }
    var selectedFont by remember { mutableStateOf(fonts.firstOrNull()?.fontName) }

    // 默认字体的前缀用于去掉图标名展示
    val defaultPrefix = remember { SkyIconFontsLib.mappingPrefix() }

    DemoScaffold(title = "图标字体", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // 已注册字体概览
                Text(
                    text = "已注册字体（${fonts.size}）：${fonts.joinToString { "${it.fontName}(${it.iconCount})" }}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }
            item {
                // 字体切换标签栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fonts.forEach { font ->
                        val selected = font.fontName == selectedFont
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Color(0xFF2196F3) else Color(0xFFEEEEEE))
                                .clickable { selectedFont = font.fontName }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = font.fontName.ifEmpty { "(默认)" },
                                fontSize = 12.sp,
                                color = if (selected) Color.White else Color(0xFF333333)
                            )
                        }
                    }
                }
            }
            item {
                val fontName = selectedFont
                val fontInfo = fonts.firstOrNull { it.fontName == fontName }
                val prefix = fontInfo?.mappingPrefix ?: defaultPrefix
                val icons = SkyIconFontsLib.iconNames(fontName)
                Text(
                    text = "当前字体：${fontName ?: "(默认)"}\n前缀：$prefix  图标数量：${icons.size}",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
                Spacer(Modifier.height(8.dp))
                SkyGridLayout(
                    items = icons,
                    columns = 4,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalSpacing = 8.dp,
                    verticalSpacing = 8.dp
                ) { name ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SkyIconFont(
                            iconName = name,
                            fontName = fontName,
                            tint = Color(0xFF2196F3),
                            fontSize = 22.sp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = name.removePrefix(prefix),
                            fontSize = 10.sp,
                            color = Color(0xFF999999),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 通用示例脚手架：带返回按钮的标题栏 + 内容区
 */
@Composable
private fun DemoScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
