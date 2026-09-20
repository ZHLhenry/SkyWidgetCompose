package com.sky.widget.samplecp

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.cell.SkyCellLayout
import com.sky.widget.iface.SkyIconType
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme

/**
 * 单元格（SkyCellLayout）示例页。
 *
 * 演示：基本用法 / 分割线 / 指定 icon（图片 + iconfont）/ 摘要信息 / 详细信息 /
 * 自定义右侧组件（Switch）/ 插槽 API 完全自定义。
 */
@Composable
fun CellDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val toast: (String) -> Unit = { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                text = "单元格示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本用法
            DemoCard(title = "基本用法") {
                SkyCellLayout(
                    text = "朋友圈",
                    onClick = { toast("点击了 朋友圈") }
                )
            }

            // 显示分割线（含左右边距内缩示例）
            DemoCard(title = "显示分割线（showDivider / dividerPadding）") {
                Column {
                    SkyCellLayout(
                        text = "朋友圈",
                        showDivider = true,
                        onClick = { toast("点击了 朋友圈") }
                    )
                    // 内缩分割线：左边距 = 图标 24dp + 图标间距 12dp，与主标题对齐
                    SkyCellLayout(
                        text = "相册",
                        icon = SkyIconType.painter(painterResource(R.drawable.ic_sample_purple)),
                        showDivider = true,
                        dividerPadding = PaddingValues(start = 36.dp),
                        onClick = { toast("点击了 相册") }
                    )
                    SkyCellLayout(
                        text = "收藏",
                        onClick = { toast("点击了 收藏") }
                    )
                }
            }

            // 指定 icon（Painter 图片）
            DemoCard(title = "指定 icon（Painter）") {
                SkyCellLayout(
                    text = "朋友圈",
                    icon = SkyIconType.painter(painterResource(R.drawable.ic_sample_purple)),
                    onClick = { toast("点击了 朋友圈") }
                )
            }

            // 指定 icon（iconfont）
            DemoCard(title = "指定 icon（iconfont）") {
                SkyCellLayout(
                    text = "分享",
                    icon = SkyIconType.iconFont("testSkyfenxiang", "testSky_iconfont"),
                    onClick = { toast("点击了 分享") }
                )
            }

            // 显示摘要信息
            DemoCard(title = "显示摘要信息（secondaryText）") {
                SkyCellLayout(
                    text = "朋友圈",
                    secondaryText = "摘要信息",
                    icon = SkyIconType.painter(painterResource(R.drawable.ic_sample_purple)),
                    onClick = { toast("点击了 朋友圈") }
                )
            }

            // 显示详细信息
            DemoCard(title = "显示详细信息（rightText）") {
                SkyCellLayout(
                    text = "朋友圈",
                    rightText = "详细信息",
                    icon = SkyIconType.painter(painterResource(R.drawable.ic_sample_purple)),
                    onClick = { toast("点击了 朋友圈") }
                )
            }

            // 自定义右侧组件
            DemoCard(title = "自定义右侧组件（rightContent = Switch）") {
                var checked by remember { mutableStateOf(true) }
                SkyCellLayout(
                    text = "消息通知",
                    showArrow = false,
                    rightContent = {
                        Switch(checked = checked, onCheckedChange = { checked = it })
                    },
                    onClick = { checked = !checked }
                )
            }

            // 插槽 API 完全自定义
            DemoCard(title = "插槽 API（完全自定义内容）") {
                SkyCellLayout(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "VIP 会员", fontSize = 16.sp, color = Color(0xFF333333))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "年卡",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFB300))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300))
                        )
                    },
                    secondaryText = {
                        Text(
                            text = "有效期至 2027-09-19",
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    },
                    rightText = {
                        Text(text = "立即续费", fontSize = 13.sp, color = Color(0xFFFF5722))
                    },
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.White)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun CellDemoScreenPreview() {
    SkyWidgetComposeTheme {
        CellDemoScreen(onBack = {})
    }
}
