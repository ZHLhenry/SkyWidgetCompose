package com.sky.widget.cell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.painter.Painter
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iface.SkyIconType

/**
 * 单元格布局（纯 Compose 实现，Material3）。
 *
 * 通用列表项：左侧图标（可选）+ 主标题 / 摘要 + 右侧详情文字 / 箭头（或自定义组件），
 * 支持底部分割线与点击事件，常用于设置页、个人中心菜单等场景。
 *
 * 如需完全自定义各区域内容，使用插槽重载 `SkyCellLayout(text = { ... }, ...)`。
 *
 * @param text 主标题文字。
 * @param modifier 外层修饰符（在库内默认修饰符之后应用，可覆盖）。
 * @param icon 左侧图标，见 [SkyIconType]（[Painter] 图片或 iconfont 名称）；为 null 时不显示。
 * @param secondaryText 摘要信息（主标题下方的小字）；为 null 时不显示。
 * @param rightText 右侧详细信息文字（箭头左侧）；为 null 时不显示。
 * @param showArrow 是否显示右侧箭头；[rightContent] 非空时箭头不渲染。
 * @param rightContent 自定义右侧内容（如 Switch、勾选图标等）；优先级高于箭头。
 * @param showDivider 是否在单元格底部显示分割线（贴整个 item 的底边，贯穿内容宽度，而非紧贴文本）。
 * @param minHeight 单元格最小高度，内容超出时自动增高。
 * @param contentPadding 内容区内边距。
 * @param backgroundColor 背景色。
 * @param textStyle 主标题文字样式。
 * @param secondaryTextStyle 摘要文字样式。
 * @param rightTextStyle 右侧详情文字样式。
 * @param dividerColor 分割线颜色。
 * @param dividerPadding 分割线内边距（左右边距可实现内缩分割线，如与主标题对齐）。
 * @param onClick 点击回调；为 null 时不可点击（无涟漪效果）。
 */
@Composable
fun SkyCellLayout(
    text: String,
    modifier: Modifier = Modifier,
    icon: SkyIconType? = null,
    secondaryText: String? = null,
    rightText: String? = null,
    showArrow: Boolean = true,
    rightContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    minHeight: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    secondaryTextStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    rightTextStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
    dividerPadding: PaddingValues = PaddingValues(),
    onClick: (() -> Unit)? = null,
) {
    SkyCellLayout(
        text = @Composable { Text(text = text, style = textStyle) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(backgroundColor)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(contentPadding)
            .then(modifier),
        icon = icon?.let { @Composable { SkyCellIconContent(it) } },
        secondaryText = secondaryText?.let {
            @Composable {
                Text(
                    text = it,
                    style = secondaryTextStyle,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        rightText = rightText?.let {
            @Composable { Text(text = it, style = rightTextStyle) }
        },
        rightContent = run {
            val arrowContent: @Composable () -> Unit = {
                SkyCellArrowIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val resolved: (@Composable () -> Unit)? = when {
                rightContent != null -> rightContent
                showArrow -> arrowContent
                else -> null
            }
            resolved
        },
        showDivider = showDivider,
        dividerColor = dividerColor,
        dividerPadding = dividerPadding
    )
}

/**
 * 插槽版 [SkyCellLayout]：各区域内容完全自定义。
 *
 * @param text 主标题内容（必填）。
 * @param modifier 外层修饰符。
 * @param icon 左侧图标内容；为 null 时不显示。
 * @param secondaryText 摘要内容（主标题下方）；为 null 时不显示。
 * @param rightText 右侧详情内容（[rightContent] 左侧）；为 null 时不显示。
 * @param rightContent 右侧内容（箭头 / Switch 等）；为 null 时不显示。
 * @param showDivider 是否在单元格底部显示分割线（贴整个 item 的底边，贯穿内容宽度，而非紧贴文本）。
 * @param dividerColor 分割线颜色。
 * @param dividerPadding 分割线内边距（左右边距可实现内缩分割线，如与主标题对齐）。
 */
@Composable
fun SkyCellLayout(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    secondaryText: (@Composable () -> Unit)? = null,
    rightText: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
    dividerPadding: PaddingValues = PaddingValues(),
) {
    // 根 Box 承载完整 item 高度（含 minHeight），内容垂直居中；分割线对齐其底边，位于整个 item 下方。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                text()
                secondaryText?.invoke()
            }
            rightText?.invoke()
            rightContent?.invoke()
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(dividerPadding),
                thickness = 0.5.dp,
                color = dividerColor
            )
        }
    }
}

/**
 * 默认右侧箭头：Canvas 自绘 chevron（库模块不依赖 material-icons）。
 */
@Composable
private fun SkyCellArrowIcon(tint: Color) {
    Canvas(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(20.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.25f)
            lineTo(size.width * 0.62f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height * 0.75f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * 默认左侧图标渲染：按 [SkyIconType] 类型分派，统一 24dp 尺寸 + 右侧 12dp 间距。
 */
@Composable
private fun SkyCellIconContent(icon: SkyIconType) {
    when (icon) {
        is SkyIconType.PainterIcon -> Image(
            painter = icon.painter,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp)
        )

        is SkyIconType.FontIcon -> SkyIconFont(
            iconName = icon.iconName,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp),
            fontSize = with(LocalDensity.current) { 24.dp.toSp() },
            fontName = icon.fontName
        )
    }
}
