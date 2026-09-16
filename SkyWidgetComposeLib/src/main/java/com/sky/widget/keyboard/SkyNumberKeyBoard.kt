package com.sky.widget.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sky.widget.bottomSheet.SkyBottomSheetState
import com.sky.widget.bottomSheet.SkyBottomSheet
import com.sky.widget.bottomSheet.rememberBottomSheetState
import com.sky.widget.iconfont.SkyIconFont

/**
 * @Class: SkyNumberKeyBoard
 * @Author: Henry
 * @Date: 2026/9/16 10:27
 * @Description: 数字键盘底部弹窗（0-9 数字键 + 删除键 + 确认键，确认键纵向占满右侧剩余空间），
 * 基于 [SkyBottomSheet] 实现，通过 [SkyBottomSheetState] 控制展示与隐藏
 *
 * 使用示例：
 * ```
 * val state = rememberBottomSheetState()
 * val scope = rememberCoroutineScope()
 * SkyNumberKeyBoard(
 *     state = state,
 *     onInput = { value -> /* 追加输入 */ },
 *     onDelete = { /* 删除一位 */ },
 *     onConfirm = { scope.launch { state.hide() } }
 * )
 * // 唤起键盘：scope.launch { state.show() }
 * ```
 *
 * @param modifier 键盘内容区修饰符
 * @param confirmModifier 确认键修饰符
 * @param state 底部弹窗状态，通过 [rememberBottomSheetState] 创建，控制键盘展示/隐藏
 * @param sheetCornerRadius 弹窗顶部圆角半径，默认 28.dp；传 0.dp 为直角
 * @param horizontalSpacing 按键水平间距（含两侧边距），默认 6.dp
 * @param verticalSpacing 按键垂直间距，默认 6.dp
 * @param itemHeight 单个按键高度，默认 48.dp
 * @param confirmText 确认键文案，默认「确定」
 * @param confirmDisable 是否禁用确认键（降低透明度且不可点击），默认 false
 * @param random 是否随机打乱数字键顺序（0-9 全部参与乱序，安全键盘场景），默认 false
 * @param showDot 是否显示「.」小数点键，默认 true；隐藏后「0」键占满底部整行
 * @param deleteIcon 删除键图标，支持 [SkyKeyBoardIcon.Vector]（矢量图标）与
 * [SkyKeyBoardIcon.IconFont]（iconfont 图标名）两种形式，默认 null 使用内置 Backspace 图标
 * @param asBottomSheet 是否以 BottomSheet 弹窗形式出现，默认 true；
 * 为 false 时键盘直接内嵌在页面中，此时 [state] 与 [sheetCornerRadius] 参数不生效
 * @param onInput 数字/符号键点击回调，参数为按键值（"0"-"9" 或 "."）
 * @param onDelete 删除键点击回调
 * @param onConfirm 确认键点击回调，[confirmDisable] 为 true 时不触发
 */
@Composable
fun SkyNumberKeyBoard(
    modifier: Modifier = Modifier,
    confirmModifier: Modifier = Modifier,
    state: SkyBottomSheetState = rememberBottomSheetState(),
    sheetCornerRadius: Dp = 28.dp,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
    itemHeight: Dp = 48.dp,
    confirmText: String = "确定",
    confirmDisable: Boolean = false,
    random: Boolean = false,
    showDot: Boolean = true,
    deleteIcon: SkyKeyBoardIcon? = null,
    asBottomSheet: Boolean = true,
    onInput: ((value: String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
) {
    // 键盘内容主体：弹窗模式与内嵌模式共用
    val keyboardContent: @Composable () -> Unit =
        {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 22.dp)
                    .then(modifier)
            ) {
                // 4 列等宽按键：列间距 3 处 + 两侧边距 2 处，共 5 个间距
                val itemWidth = (maxWidth - horizontalSpacing * 5) / 4
                // 数字键：随机时 0-9 全部参与乱序，否则 1-9 顺序排列、0 固定在底行
                val digits = if (random) (0..9).shuffled() else (1..9).toList()
                val keyRows = if (random) digits.dropLast(1).chunked(3) else digits.chunked(3)
                val lastDigit = if (random) digits.last() else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = verticalSpacing,
                            start = horizontalSpacing,
                            end = horizontalSpacing
                        ),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
                ) {
                    // 左侧数字区：1-9 三行 + 底部「0」「.」行（0 占两列）
                    Column(
                        modifier = Modifier.width(itemWidth * 3 + horizontalSpacing * 2),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        keyRows.forEach { rowKeys ->
                            Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                                rowKeys.forEach { key ->
                                    val value = key.toString()
                                    KeyItem(
                                        text = value,
                                        modifier = Modifier.size(itemWidth, itemHeight),
                                        onClick = { onInput?.invoke(value) }
                                    )
                                }
                            }
                        }
                        // 底行：末位数字键 + 「.」小数点键（随机键盘时末位数字为乱序结果，不固定为 0）
                        Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                            KeyItem(
                                text = lastDigit.toString(),
                                modifier = Modifier
                                    // 显示小数点键时占两列，否则占满整行
                                    .width(
                                        if (showDot) itemWidth * 2 + horizontalSpacing
                                        else itemWidth * 3 + horizontalSpacing * 2
                                    )
                                    .height(itemHeight),
                                onClick = { onInput?.invoke(lastDigit.toString()) }
                            )
                            if (showDot) {
                                KeyItem(
                                    text = ".",
                                    modifier = Modifier.size(itemWidth, itemHeight),
                                    onClick = { onInput?.invoke(".") }
                                )
                            }
                        }
                    }
                    // 右侧：删除键 + 确认键（确认键填满剩余高度）
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(itemHeight * 4 + verticalSpacing * 3),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(itemWidth, itemHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable {
                                    onDelete?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (deleteIcon) {
                                // 未传入时使用内置 Backspace 矢量图标
                                null -> Icon(
                                    imageVector = KeyboardBackspaceIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )

                                is SkyKeyBoardIcon.Vector -> Icon(
                                    imageVector = deleteIcon.imageVector,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )

                                is SkyKeyBoardIcon.IconFont -> SkyIconFont(
                                    iconName = deleteIcon.iconName,
                                    fontName = deleteIcon.fontName,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .alpha(if (confirmDisable) 0.38f else 1.0f)
                                .clickable(
                                    enabled = !confirmDisable,
                                    onClick = {
                                        onConfirm?.invoke()
                                    }
                                )
                                .then(confirmModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

    if (asBottomSheet) {
        SkyBottomSheet(
            state = state,
            sheetCornerRadius = sheetCornerRadius,
            content = keyboardContent
        )
    } else {
        keyboardContent()
    }
}

/**
 * 单个数字/符号按键
 *
 * @param text 按键文案
 * @param modifier 按键修饰符（由外部指定尺寸）
 * @param onClick 按键点击回调
 */
@Composable
private fun KeyItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

/**
 * 键盘按键图标描述：同时支持矢量图标与 iconfont 图标两种形式
 */
sealed interface SkyKeyBoardIcon {

    /**
     * 矢量图标
     *
     * @property imageVector 图标矢量数据
     */
    data class Vector(val imageVector: ImageVector) : SkyKeyBoardIcon

    /**
     * iconfont 图标（需先在 Application 中通过 SkyIconFontsLib.initRegister 注册字体）
     *
     * @property iconName 图标名（css 前缀 + font_class，如 "skygouxuanzhong"）
     * @property fontName 可选：指定字体（TTF 文件名，不含 .ttf），不传使用默认字体
     */
    data class IconFont(val iconName: String, val fontName: String? = null) : SkyKeyBoardIcon
}

/**
 * 内置删除键图标（Material Backspace 轮廓路径），避免为单个图标引入 material-icons 依赖
 */
private val KeyboardBackspaceIcon: ImageVector = ImageVector.Builder(
    name = "Backspace",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = true
).path(fill = SolidColor(Color.Black)) {
    moveTo(22f, 3f)
    horizontalLineTo(7f)
    curveTo(6.31f, 3f, 5.77f, 3.35f, 5.41f, 3.88f)
    lineTo(0f, 12f)
    lineTo(5.41f, 20.11f)
    curveTo(5.77f, 20.64f, 6.31f, 21f, 7f, 21f)
    horizontalLineTo(22f)
    curveTo(23.1f, 21f, 24f, 20.1f, 24f, 19f)
    verticalLineTo(5f)
    curveTo(24f, 3.9f, 23.1f, 3f, 22f, 3f)
    close()
    moveTo(22f, 19f)
    horizontalLineTo(7.07f)
    lineTo(2.4f, 12f)
    lineToRelative(4.66f, -7f)
    horizontalLineTo(22f)
    verticalLineToRelative(14f)
    close()
    moveTo(10.41f, 17f)
    lineTo(14f, 13.41f)
    lineTo(17.59f, 17f)
    lineTo(19f, 15.59f)
    lineTo(15.41f, 12f)
    lineTo(19f, 8.41f)
    lineTo(17.59f, 7f)
    lineTo(14f, 10.59f)
    lineTo(10.41f, 7f)
    lineTo(9f, 8.41f)
    lineTo(12.59f, 12f)
    lineTo(9f, 15.59f)
    close()
}.build()
