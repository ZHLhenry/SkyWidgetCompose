package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.bottomSheet.SkyBottomSheet
import com.sky.widget.bottomSheet.SkyBottomSheetState
import com.sky.widget.bottomSheet.SkyBottomSheetValue
import com.sky.widget.bottomSheet.rememberBottomSheetState
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import kotlinx.coroutines.launch

/**
 * 底部弹窗（SkyBottomSheet）示例页。
 *
 * 演示完整 API：基本用法（sheetBackgroundColor / sheetCornerRadius / modifier）、
 * 初始锚点（rememberBottomSheetState 的 initialValue）、
 * 状态控制（show / animateTo / hide / isVisible）。
 * 点击列表行弹出对应配置的弹窗。
 */
@Composable
fun BottomSheetDemoScreen(onBack: () -> Unit) {
    // 初始锚点演示：点击行记录期望锚点，随后以该锚点创建 state 立即展示
    var pendingInitialValue by remember { mutableStateOf<SkyBottomSheetValue?>(null) }

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
                text = "底部弹窗示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            // 示例 1：基本用法（默认圆角 + 白色背景）
            SheetSection("基本用法")
            SheetDemoRow(title = "默认参数（白色背景 + 28.dp 圆角）")

            // 示例 2：初始锚点（initialValue != Hidden 时创建即展示）
            SheetSection("初始锚点（initialValue）")
            SheetRow(title = "半屏展开（HalfExpanded）") {
                pendingInitialValue = SkyBottomSheetValue.HalfExpanded
            }
            SheetRow(title = "全展开（Expanded）") {
                pendingInitialValue = SkyBottomSheetValue.Expanded
            }

            // 示例 3：自定义背景色（sheetBackgroundColor）
            SheetSection("自定义背景色（sheetBackgroundColor）")
            SheetDemoRow(title = "浅蓝背景", sheetBackgroundColor = Color(0xFFBBDEFB))

            // 示例 4：自定义圆角（sheetCornerRadius）
            SheetSection("自定义圆角（sheetCornerRadius）")
            SheetDemoRow(title = "直角弹窗（0.dp）", sheetCornerRadius = 0.dp)

            // 示例 5：自定义修饰符（modifier，作用于弹窗容器）
            SheetSection("自定义修饰符（modifier）")
            SheetDemoRow(
                title = "两侧留白 24.dp",
                sheetModifier = Modifier.padding(horizontal = 24.dp)
            )

            // 示例 6：状态控制（show / animateTo / hide / isVisible）
            SheetSection("状态控制（show / animateTo / hide）")
            SheetDemoRow(title = "内容内按钮切换锚点", tallContent = true, withControls = true)

            // 初始锚点弹窗：以点击行记录的锚点创建 state，创建即按该锚点展示
            pendingInitialValue?.let { value ->
                val state = rememberBottomSheetState(value)
                // 隐藏后重置，保证下次点击能以新锚点重新创建 state
                LaunchedEffect(state.isVisible) {
                    if (!state.isVisible) pendingInitialValue = null
                }
                SkyBottomSheet(state = state, sheetBackgroundColor = Color.White) {
                    SheetContent(state = state, title = "initialValue = $value", tall = true)
                }
            }
        }
    }
}

/**
 * 分组标题
 */
@Composable
private fun SheetSection(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color(0xFF999999),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * 纯展示行：点击触发 [onClick]（用于不直接持有 state 的演示项）
 */
@Composable
private fun SheetRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBBBBBB)
        )
    }
}

/**
 * 弹窗示例行：点击以对应配置弹出 SkyBottomSheet
 *
 * @param title 行标题（同时作为弹窗内容标题）
 * @param sheetBackgroundColor 弹窗背景色
 * @param sheetCornerRadius 弹窗顶部圆角半径
 * @param sheetModifier 弹窗容器修饰符
 * @param tallContent 是否使用高内容（用于演示半屏/全展开锚点）
 * @param withControls 是否在内容中展示状态控制按钮
 */
@Composable
private fun SheetDemoRow(
    title: String,
    sheetBackgroundColor: Color = Color.White,
    sheetCornerRadius: Dp = 28.dp,
    sheetModifier: Modifier = Modifier,
    tallContent: Boolean = false,
    withControls: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val state = rememberBottomSheetState()
    SheetRow(title = title) { scope.launch { state.show() } }
    SkyBottomSheet(
        modifier = sheetModifier,
        state = state,
        sheetBackgroundColor = sheetBackgroundColor,
        sheetCornerRadius = sheetCornerRadius,
    ) {
        SheetContent(state = state, title = title, tall = tallContent, withControls = withControls)
    }
}

/**
 * 弹窗内容：标题 + 说明 + （高内容行）+（状态控制按钮）+ 隐藏按钮
 */
@Composable
private fun SheetContent(
    state: SkyBottomSheetState,
    title: String,
    tall: Boolean = false,
    withControls: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "content 插槽内容；弹窗为模态，支持下滑手势与返回键关闭",
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
        if (tall) {
            // 高内容：使半屏锚点生效，便于观察 HalfExpanded / Expanded 差异
            repeat(10) { index ->
                Text(text = "内容行 #${index + 1}", fontSize = 14.sp, color = Color(0xFF999999))
            }
        }
        if (withControls) {
            Text(
                text = "state.isVisible = ${state.isVisible}",
                fontSize = 13.sp,
                color = Color(0xFF2196F3)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { scope.launch { state.animateTo(SkyBottomSheetValue.HalfExpanded) } }) {
                    Text("半屏")
                }
                Button(onClick = { scope.launch { state.animateTo(SkyBottomSheetValue.Expanded) } }) {
                    Text("全展开")
                }
                Button(onClick = { scope.launch { state.hide() } }) {
                    Text("隐藏")
                }
            }
        } else {
            Button(onClick = { scope.launch { state.hide() } }) {
                Text("隐藏")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun BottomSheetDemoScreenPreview() {
    SkyWidgetComposeTheme {
        BottomSheetDemoScreen(onBack = {})
    }
}
