package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.bottomSheet.rememberBottomSheetState
import com.sky.widget.keyboard.SkyKeyBoardIcon
import com.sky.widget.keyboard.SkyNumberKeyBoard
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import kotlinx.coroutines.launch

/**
 * 数字键盘（SkyNumberKeyBoard）示例页。
 *
 * 演示：基本用法（默认键盘）、随机数键盘（random）、自定义用法
 * （确认文案/按键尺寸/间距）、禁用确认按钮（confirmDisable）、隐藏小数点键（showDot）、
 * 自定义删除图标（deleteIcon，iconfont 图标名）、自定义弹窗圆角（sheetCornerRadius）、
 * 自定义确认键（confirmModifier，渐变背景/边框）、
 * 内嵌键盘（asBottomSheet = false，不以弹窗形式出现）。
 * 点击列表行弹出对应键盘，输入内容回显在行尾部，点确认键收起键盘。
 */
@Composable
fun NumberKeyBoardDemoScreen(onBack: () -> Unit) {
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
                text = "数字键盘示例",
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
            // 示例 1：基本用法（默认参数）
            KeyBoardSection(title = "基本用法")
            NumberKeyBoardItem(title = "默认键盘")

            // 示例 2：随机数键盘（0-9 全部参与乱序）
            KeyBoardSection(title = "随机数键盘")
            NumberKeyBoardItem(title = "随机数键盘", random = true)

            // 示例 3：自定义用法（确认文案 + 按键尺寸 + 间距）
            KeyBoardSection(title = "自定义用法")
            NumberKeyBoardItem(
                title = "自定义用法",
                confirmText = "完成",
                itemHeight = 56.dp,
                horizontalSpacing = 8.dp,
                verticalSpacing = 8.dp
            )

            // 示例 4：禁用确认按钮（透明度降低且不可点击）
            KeyBoardSection(title = "禁用确认按钮")
            NumberKeyBoardItem(title = "禁用确认按钮", confirmDisable = true)

            // 示例 5：隐藏「.」小数点键（0 键占满整行）
            KeyBoardSection(title = "隐藏小数点键")
            NumberKeyBoardItem(title = "隐藏小数点键", showDot = false)

            // 示例 6：自定义删除图标（iconfont 形式，用默认字体 sky_iconfont 中的勾选图标演示）
            KeyBoardSection(title = "自定义删除图标")
            NumberKeyBoardItem(
                title = "自定义删除图标",
                deleteIcon = SkyKeyBoardIcon.IconFont("skygouxuanzhong")
            )

            // 示例 7：自定义弹窗圆角（顶部圆角半径由消费者定义）
            KeyBoardSection(title = "自定义圆角")
            NumberKeyBoardItem(title = "自定义圆角", sheetCornerRadius = 8.dp)

            // 示例 8：自定义确认键（confirmModifier 作用于确认键，覆盖默认背景/追加边框）
            KeyBoardSection(title = "自定义确认键")
            NumberKeyBoardItem(
                title = "渐变背景确认键",
                confirmModifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF9800), Color(0xFFF44336))
                    )
                )
            )
            NumberKeyBoardItem(
                title = "自定义边框确认键",
                confirmModifier = Modifier.border(
                    width = 2.dp,
                    color = Color(0xFF2196F3),
                    shape = RoundedCornerShape(8.dp)
                )
            )

            // 示例 9：内嵌键盘（不以 BottomSheet 弹窗形式出现，直接嵌入页面）
            KeyBoardSection(title = "内嵌键盘")
            InlineKeyBoardDemo()
        }
    }
}

/**
 * 分组标题
 */
@Composable
private fun KeyBoardSection(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color(0xFF999999),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * 单个键盘示例行：点击弹出键盘，输入内容回显在行尾部
 *
 * @param title 行标题
 * @param random 是否随机打乱数字键顺序（0-9 全部参与乱序）
 * @param confirmText 确认键文案
 * @param confirmDisable 是否禁用确认键
 * @param showDot 是否显示「.」小数点键
 * @param deleteIcon 删除键图标（矢量 / iconfont），null 使用内置图标
 * @param sheetCornerRadius 弹窗顶部圆角半径
 * @param confirmModifier 确认键修饰符（如自定义背景/边框）
 * @param itemHeight 单个按键高度
 * @param horizontalSpacing 按键水平间距
 * @param verticalSpacing 按键垂直间距
 */
@Composable
private fun NumberKeyBoardItem(
    title: String,
    random: Boolean = false,
    confirmText: String = "确定",
    confirmDisable: Boolean = false,
    showDot: Boolean = true,
    deleteIcon: SkyKeyBoardIcon? = null,
    sheetCornerRadius: Dp = 28.dp,
    confirmModifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
) {
    val scope = rememberCoroutineScope()
    val keyboardState = rememberBottomSheetState()
    var input by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { scope.launch { keyboardState.show() } }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )
        if (input.isNotEmpty()) {
            Text(
                text = input,
                fontSize = 14.sp,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBBBBBB)
        )
    }

    SkyNumberKeyBoard(
        state = keyboardState,
        random = random,
        confirmText = confirmText,
        confirmDisable = confirmDisable,
        showDot = showDot,
        deleteIcon = deleteIcon,
        sheetCornerRadius = sheetCornerRadius,
        confirmModifier = confirmModifier,
        itemHeight = itemHeight,
        horizontalSpacing = horizontalSpacing,
        verticalSpacing = verticalSpacing,
        onInput = { input += it },
        onDelete = { input = input.dropLast(1) },
        onConfirm = { scope.launch { keyboardState.hide() } }
    )
}

/**
 * 内嵌键盘示例：键盘直接嵌入页面（asBottomSheet = false），输入内容回显在键盘上方
 */
@Composable
private fun InlineKeyBoardDemo() {
    var input by remember { mutableStateOf("") }
    Column(modifier = Modifier.background(Color.White)) {
        Text(
            text = input.ifEmpty { "在下方键盘输入内容" },
            fontSize = 15.sp,
            color = if (input.isEmpty()) Color(0xFF999999) else Color(0xFF2196F3),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        SkyNumberKeyBoard(
            asBottomSheet = false,
            onInput = { input += it },
            onDelete = { input = input.dropLast(1) },
            onConfirm = { input = "" }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun NumberKeyBoardDemoScreenPreview() {
    SkyWidgetComposeTheme {
        NumberKeyBoardDemoScreen(onBack = {})
    }
}
