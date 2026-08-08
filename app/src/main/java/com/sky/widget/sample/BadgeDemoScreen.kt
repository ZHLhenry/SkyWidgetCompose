package com.sky.widget.sample

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.badge.SkyBadgeBox
import com.sky.widget.badge.SkyBadgeDragState
import com.sky.widget.badge.SkyBadgeGravity
import com.sky.widget.badge.SkyBadgeState
import com.sky.widget.badge.SkyBadgeView
import com.sky.widget.badge.rememberSkyBadgeState
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme

/**
 * 徽章（Badge）示例页。
 *
 * 演示：
 * - 数字 / 文本 / 圆点徽章
 * - 九宫格方位对齐
 * - 拖拽消除与状态回调
 * - 颜色 / 边框 / 阴影样式切换
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val numberState: SkyBadgeState = rememberSkyBadgeState(initialNumber = 5)
    val positionState: SkyBadgeState = rememberSkyBadgeState(initialNumber = 8)
    val styleState: SkyBadgeState = rememberSkyBadgeState(initialNumber = 12)

    var currentGravity by remember { mutableStateOf(SkyBadgeGravity.TOP_END) }
    var dragStatus by remember { mutableStateOf("未拖拽") }

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
                text = "徽章示例",
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
            // 示例 1：数字控制
            DemoCard(title = "数字徽章（点击按钮切换）") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { numberState.number += 1 },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("+1", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { numberState.number = (numberState.number - 1).coerceAtLeast(-1) },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("-1", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { numberState.number = 120 },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("99+", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { numberState.isExact = !numberState.isExact },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(
                                if (numberState.isExact) "精确显示" else "99+ 折叠",
                                fontSize = 13.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { numberState.setBadgeText(if (numberState.text == null) "NEW" else null) },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("切换文本", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { numberState.toggle() },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("显示/隐藏", fontSize = 13.sp) }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SkyBadgeBox(
                            state = numberState,
                            gravity = SkyBadgeGravity.TOP_END,
                            offset = DpOffset((-4).dp, 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("消息", color = Color(0xFF1976D2), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 示例 2：九宫格位置 + 拖拽消除
            DemoCard(title = "九宫格方位（长按徽章可拖拽消除）") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkyBadgeGravity.entries.forEach { gravity ->
                            val label = when (gravity) {
                                SkyBadgeGravity.TOP_START -> "左上"
                                SkyBadgeGravity.TOP_CENTER -> "中上"
                                SkyBadgeGravity.TOP_END -> "右上"
                                SkyBadgeGravity.CENTER_START -> "左中"
                                SkyBadgeGravity.CENTER -> "正中"
                                SkyBadgeGravity.CENTER_END -> "右中"
                                SkyBadgeGravity.BOTTOM_START -> "左下"
                                SkyBadgeGravity.BOTTOM_CENTER -> "中下"
                                SkyBadgeGravity.BOTTOM_END -> "右下"
                            }
                            OutlinedButton(
                                onClick = {
                                    currentGravity = gravity
                                    positionState.reset((1..20).random())
                                    dragStatus = "未拖拽"
                                },
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "拖拽状态：$dragStatus",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SkyBadgeBox(
                            state = positionState,
                            gravity = currentGravity,
                            offset = DpOffset(4.dp, (-4).dp),
                            backgroundColor = Color(0xFFFF9800),
                            draggable = true,
                            maxDragDistance = 90.dp,
                            onDragStateChanged = { state ->
                                dragStatus = when (state) {
                                    SkyBadgeDragState.START -> "开始拖拽"
                                    SkyBadgeDragState.DRAGGING -> "拖拽中"
                                    SkyBadgeDragState.DRAGGING_OUT_OF_RANGE -> "超出范围"
                                    SkyBadgeDragState.CANCELED -> "已取消"
                                    SkyBadgeDragState.SUCCEED -> {
                                        Toast.makeText(context, "已消除", Toast.LENGTH_SHORT).show()
                                        "已消除"
                                    }

                                    else -> "未知"
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("目标", color = Color(0xFFE65100), fontSize = 14.sp)
                            }
                        }
                    }

                    if (!positionState.isVisible) {
                        Button(
                            onClick = {
                                positionState.reset(8)
                                dragStatus = "已恢复"
                            }
                        ) {
                            Text("恢复徽章")
                        }
                    }
                }
            }

            // 示例 3：样式定制
            DemoCard(title = "样式定制（边框 / 颜色）") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var styleIndex by remember { mutableIntStateOf(0) }
                    val styles = listOf(
                        Triple(Color(0xFFFF5252), Color.Unspecified, 0.dp),
                        Triple(Color(0xFF2196F3), Color.White, 1.dp),
                        Triple(Color(0xFF9C27B0), Color.Yellow, 2.dp),
                        Triple(Color(0xFF4CAF50), Color(0xFF1B5E20), 1.dp)
                    )
                    val (bg, border, borderWidth) = styles[styleIndex]

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEachIndexed { index, _ ->
                            OutlinedButton(
                                onClick = { styleIndex = index },
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) { Text("样式 ${index + 1}", fontSize = 13.sp) }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SkyBadgeBox(
                            state = styleState,
                            gravity = SkyBadgeGravity.TOP_END,
                            backgroundColor = bg,
                            borderColor = border,
                            borderWidth = borderWidth,
                            textColor = Color.White,
                            showShadow = true
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("设置", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 示例 4：独立使用 SkyBadgeView
            DemoCard(title = "独立 SkyBadgeView") {
                val standaloneState = rememberSkyBadgeState(initialNumber = 7)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { standaloneState.number = (1..50).random() },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("随机数字", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { standaloneState.setBadgeText(if (standaloneState.text == null) "VIP" else null) },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("切换 VIP", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { standaloneState.number = -1 },
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) { Text("圆点", fontSize = 13.sp) }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SkyBadgeView(
                            state = standaloneState,
                            backgroundColor = Color(0xFF9C27B0),
                            textColor = Color.White,
                            horizontalPadding = 6.dp,
                            verticalPadding = 6.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun BadgeDemoScreenPreview() {
    SkyWidgetComposeTheme {
        BadgeDemoScreen(onBack = {})
    }
}
