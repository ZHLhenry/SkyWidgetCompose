package com.sky.widget.pageIndicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 内置指示器样式集合。
 *
 * 以下三个均为 [SkyPageIndicatorScope] 的扩展 Composable，作为“每一项”的绘制单元，
 * 需配合 [SkyPageIndicator] 的 itemContent 使用；[SkyPageIndicator] 不传 itemContent 时
 * 默认使用圆点样式 [SkyDotIndicatorStyle]。
 *
 * 选用示例：
 * ```
 * SkyPageIndicator(state = state) { SkyUnderlineIndicatorStyle() }
 * SkyPageIndicator(state = state) { SkyNumberIndicatorStyle() }
 * ```
 */

/**
 * 圆点指示器项（[SkyPageIndicator] 的默认样式）：选中项放大并高亮。
 *
 * @param selectedColor 选中颜色。
 * @param unselectedColor 未选中颜色。
 * @param selectedSize 选中尺寸。
 * @param unselectedSize 未选中尺寸。
 */
@Composable
fun SkyPageIndicatorScope.SkyDotIndicatorStyle(
    selectedColor: Color = Color(0xFF2196F3),
    unselectedColor: Color = Color(0xFFCCCCCC),
    selectedSize: Dp = 10.dp,
    unselectedSize: Dp = 8.dp,
) {
    Box(
        modifier = Modifier
            .size(if (selected) selectedSize else unselectedSize)
            .background(
                color = if (selected) selectedColor else unselectedColor,
                shape = CircleShape
            )
    )
}

/**
 * 下划线指示器项：每一项为一段圆角横条，选中项加粗并高亮，形似 Tab 下划线。
 *
 * @param selectedColor 选中颜色。
 * @param unselectedColor 未选中颜色。
 * @param width 每段下划线的宽度。
 * @param selectedHeight 选中高度（加粗）。
 * @param unselectedHeight 未选中高度。
 */
@Composable
fun SkyPageIndicatorScope.SkyUnderlineIndicatorStyle(
    selectedColor: Color = Color(0xFF2196F3),
    unselectedColor: Color = Color(0xFFCCCCCC),
    width: Dp = 24.dp,
    selectedHeight: Dp = 4.dp,
    unselectedHeight: Dp = 2.dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(if (selected) selectedHeight else unselectedHeight)
            .background(
                color = if (selected) selectedColor else unselectedColor,
                shape = RoundedCornerShape(50)
            )
    )
}

/**
 * 数字指示器项：每一项显示页码数字（index + 1）；
 * 选中项为高亮圆形底 + 白色数字，未选中项仅灰色数字。
 *
 * @param selectedColor 选中圆形底色。
 * @param unselectedColor 未选中数字颜色。
 * @param size 圆形底尺寸。
 * @param textSize 数字字号。
 */
@Composable
fun SkyPageIndicatorScope.SkyNumberIndicatorStyle(
    selectedColor: Color = Color(0xFF2196F3),
    unselectedColor: Color = Color(0xFF999999),
    size: Dp = 22.dp,
    textSize: TextUnit = 12.sp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = if (selected) selectedColor else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${index + 1}",
            color = if (selected) Color.White else unselectedColor,
            fontSize = textSize
        )
    }
}
