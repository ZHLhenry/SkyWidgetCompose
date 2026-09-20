package com.sky.widget.tabLayout

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.badge.SkyBadgeBox
import com.sky.widget.badge.SkyBadgeGravity
import com.sky.widget.badge.SkyBadgeState
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iface.SkyIconType
import com.sky.widget.viewpage.SkyViewPageState
import kotlin.math.roundToInt

/**
 * Tab 排列模式。
 */
enum class SkyTabMode {
    /** 等分模式：所有 Tab 均分父容器宽度（类似底部导航栏）。 */
    Fixed,

    /** 滑动模式：Tab 宽度自适应内容，超出父容器宽度时可横向滑动，选中项自动滚动到可见区域。 */
    Scrollable
}

/**
 * [SkyTabLayout] 的单个 Tab 数据。
 *
 * 默认渲染为「图标 + 文字」样式（图标在上、文字在下）；若需要完全自定义某个 Tab 的布局，
 * 可传入 [customContent]，其优先级高于 [SkyTabLayout] 的 `itemContent` 插槽与默认样式。
 *
 * @param text Tab 文字。
 * @param icon 未选中态图标，见 [com.sky.widget.iface.SkyIconType]；为 null 时只渲染文字。
 * @param selectedIcon 选中态图标；为 null 时回落到 [icon]。
 * @param badgeState 角标状态（复用 [SkyBadgeState] 的数字 / 文本 / 圆点能力）；为 null 时不显示角标。
 * @param customContent 该 Tab 的完全自定义布局，接收是否选中参数。
 */
@Immutable
class SkyTabItem(
    val text: String,
    val icon: SkyIconType? = null,
    val selectedIcon: SkyIconType? = null,
    val badgeState: SkyBadgeState? = null,
    val customContent: (@Composable (selected: Boolean) -> Unit)? = null,
)

/**
 * [SkyTabLayout] 的样式配置集合。
 *
 * 将颜色 / 尺寸 / 角标等可定制项收敛为一个不可变配置。类型 public 是作为 [SkyTabLayout]
 * 参数类型的必然要求；构造器为 internal，消费者只能通过 [rememberSkyTabStyle] 创建：
 * `SkyTabLayout(tabs = tabs, style = rememberSkyTabStyle(iconTint = true))`。
 * 所有字段仅作用于默认「图标 + 文字」样式与内置角标；自定义布局（[SkyTabItem.customContent] /
 * `itemContent` 插槽）不受其影响。
 *
 * @property selectedTextColor 选中态文字颜色；[iconTint] 为 true 时也用于选中态图标着色。
 * @property unselectedTextColor 未选中态文字颜色；[iconTint] 为 true 时也用于未选中态图标着色。
 * @property textSize 文字大小。
 * @property iconSize 默认样式中图标的尺寸。
 * @property iconTextSpacing 默认样式中图标与文字的间距。
 * @property itemPadding 默认样式中单个 Tab 内容的内边距。
 * @property itemSpacing [SkyTabMode.Scrollable] 模式下相邻 Tab 的间距。
 * @property edgePadding [SkyTabMode.Scrollable] 模式下首尾两端的留白。
 * @property iconTint 是否用选中 / 未选中颜色给图标着色；图标自带颜色（如彩色矢量图）时应保持 false。
 * @property badgeBackgroundColor 角标背景色。
 * @property badgeTextColor 角标文字颜色。
 * @property badgeGravity 角标相对图标的方位，默认右上角。
 * @property badgeOffset 角标相对 [badgeGravity] 基准位置的额外偏移。
 */
@Immutable
class SkyTabStyle internal constructor(
    val selectedTextColor: Color = Color(0xFFFF5722),
    val unselectedTextColor: Color = Color(0xFF999999),
    val textSize: TextUnit = 12.sp,
    val iconSize: Dp = 24.dp,
    val iconTextSpacing: Dp = 2.dp,
    val itemPadding: PaddingValues = PaddingValues(vertical = 6.dp),
    val itemSpacing: Dp = 28.dp,
    val edgePadding: Dp = 16.dp,
    val iconTint: Boolean = false,
    val badgeBackgroundColor: Color = Color.Red,
    val badgeTextColor: Color = Color.White,
    val badgeGravity: SkyBadgeGravity = SkyBadgeGravity.TOP_END,
    val badgeOffset: DpOffset = DpOffset(6.dp, (-3).dp),
)

/**
 * 创建并记住一个 [SkyTabStyle]，参数与 [SkyTabStyle] 构造字段一一对应（默认值即库默认样式）。
 */
@Composable
fun rememberSkyTabStyle(
    selectedTextColor: Color = Color(0xFFFF5722),
    unselectedTextColor: Color = Color(0xFF999999),
    textSize: TextUnit = 12.sp,
    iconSize: Dp = 24.dp,
    iconTextSpacing: Dp = 2.dp,
    itemPadding: PaddingValues = PaddingValues(vertical = 6.dp),
    itemSpacing: Dp = 28.dp,
    edgePadding: Dp = 16.dp,
    iconTint: Boolean = false,
    badgeBackgroundColor: Color = Color.Red,
    badgeTextColor: Color = Color.White,
    badgeGravity: SkyBadgeGravity = SkyBadgeGravity.TOP_END,
    badgeOffset: DpOffset = DpOffset(6.dp, (-3).dp),
): SkyTabStyle = remember(
    selectedTextColor, unselectedTextColor, textSize, iconSize, iconTextSpacing,
    itemPadding, itemSpacing, edgePadding, iconTint,
    badgeBackgroundColor, badgeTextColor, badgeGravity, badgeOffset
) {
    SkyTabStyle(
        selectedTextColor = selectedTextColor,
        unselectedTextColor = unselectedTextColor,
        textSize = textSize,
        iconSize = iconSize,
        iconTextSpacing = iconTextSpacing,
        itemPadding = itemPadding,
        itemSpacing = itemSpacing,
        edgePadding = edgePadding,
        iconTint = iconTint,
        badgeBackgroundColor = badgeBackgroundColor,
        badgeTextColor = badgeTextColor,
        badgeGravity = badgeGravity,
        badgeOffset = badgeOffset
    )
}

/**
 * 标签布局组件（纯 Compose 实现）。
 *
 * 默认每个 Tab 为「图标 + 文字」样式，并支持：
 * - [SkyTabMode.Fixed] 等分模式 / [SkyTabMode.Scrollable] 滑动模式；
 * - [Orientation.Horizontal] 横向 / [Orientation.Vertical] 纵向（侧边栏式导航）；
 * - 基于 [SkyBadgeState] 的角标（数字 / 文本 / 圆点）；
 * - 通过 [itemContent] 插槽或 [SkyTabItem.customContent] 自定义 Tab 布局；
 * - 传入 [pagerState] 即可与 [com.sky.widget.viewpage.SkyViewPage] 零胶水联动：
 *   选中索引直接读取分页状态（滑动页面 Tab 自动跟随），点击 Tab 自动翻页；
 *   此时 [selectedIndex] 参数被忽略，[onTabSelected] 作为点击后的附加回调（可空）。
 *
 * @param tabs Tab 数据集合。
 * @param modifier 外层修饰符。
 * @param selectedIndex 当前选中索引（受控）；仅 [pagerState] 为 null 时生效。
 * @param onTabSelected Tab 点击回调（重复点击当前项也会触发，由消费者决定是否响应）；
 *   [pagerState] 为 null 时通常必传，否则点击无任何效果。
 * @param pagerState 可选，[com.sky.widget.viewpage.SkyViewPage] 的状态控制器；传入后进入联动模式。
 * @param animateToPage 联动模式下点击 Tab 是否以动画方式翻页；false 时无动画直接跳转。
 * @param mode 排列模式，默认 [SkyTabMode.Fixed] 等分。
 * @param orientation 排列方向，默认 [Orientation.Horizontal] 横向；
 *   [SkyTabMode.Fixed] 纵向时各 Tab 均分父容器高度，[SkyTabMode.Scrollable] 纵向时上下滑动。
 * @param style 样式配置集合（颜色 / 尺寸 / 角标等），经 [rememberSkyTabStyle] 创建，见 [SkyTabStyle]。
 * @param itemContent 自定义 Tab 布局插槽，接收（索引、Tab 数据、是否选中）；
 *   优先级低于 [SkyTabItem.customContent]，高于默认「图标 + 文字」样式。
 */
@Composable
fun SkyTabLayout(
    tabs: List<SkyTabItem>,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    onTabSelected: ((index: Int) -> Unit)? = null,
    pagerState: SkyViewPageState? = null,
    animateToPage: Boolean = true,
    mode: SkyTabMode = SkyTabMode.Fixed,
    orientation: Orientation = Orientation.Horizontal,
    style: SkyTabStyle = rememberSkyTabStyle(),
    itemContent: (@Composable (index: Int, tab: SkyTabItem, selected: Boolean) -> Unit)? = null,
) {
    if (tabs.isEmpty()) return

    // 联动模式下选中索引直接订阅分页状态（读取 .value 即完成组合期订阅），否则使用受控参数。
    val pagerSelectedIndex = pagerState?.getCurrSelectIndexState()?.value
    val actualSelectedIndex = pagerSelectedIndex ?: selectedIndex

    // 统一的点击分发：联动模式先驱动翻页，再派发附加回调。
    val handleTabSelected: (Int) -> Unit = { index ->
        if (pagerState != null && index != pagerState.getCurrSelectIndex()) {
            if (animateToPage) {
                pagerState.setPageIndexWithAnimate(index)
            } else {
                pagerState.setPageIndex(index)
            }
        }
        onTabSelected?.invoke(index)
    }

    when (mode) {
        SkyTabMode.Fixed -> {
            // 等分模式：横向均分宽度（Row weight），纵向均分高度（Column weight）。
            if (orientation == Orientation.Horizontal) {
                Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handleTabSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            SkyTabItemContent(
                                index = index,
                                tab = tab,
                                selected = index == actualSelectedIndex,
                                style = style,
                                itemContent = itemContent
                            )
                        }
                    }
                }
            } else {
                Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    tabs.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handleTabSelected(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            SkyTabItemContent(
                                index = index,
                                tab = tab,
                                selected = index == actualSelectedIndex,
                                style = style,
                                itemContent = itemContent
                            )
                        }
                    }
                }
            }
        }

        SkyTabMode.Scrollable -> {
            val scrollState = rememberScrollState()
            // 记录各 Tab 在主轴方向相对内容起点的位置（起点, 尺寸），用于选中项自动居中滚动。
            val itemBounds = remember { mutableStateMapOf<Int, Pair<Float, Int>>() }

            LaunchedEffect(actualSelectedIndex, tabs.size) {
                val (start, mainSize) = itemBounds[actualSelectedIndex] ?: return@LaunchedEffect
                val viewport = scrollState.viewportSize
                if (viewport <= 0) return@LaunchedEffect
                val target = (start + mainSize / 2f - viewport / 2f)
                    .roundToInt()
                    .coerceIn(0, scrollState.maxValue)
                scrollState.animateScrollTo(target)
            }

            // 横向取 x/width，纵向取 y/height，统一写入主轴位置表。
            val scrollableItemModifier: (Int) -> Modifier = { index ->
                Modifier
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.parentLayoutCoordinates
                            ?.localPositionOf(coordinates, Offset.Zero)
                        itemBounds[index] = if (orientation == Orientation.Horizontal) {
                            (position?.x ?: 0f) to coordinates.size.width
                        } else {
                            (position?.y ?: 0f) to coordinates.size.height
                        }
                    }
                    .clickable { handleTabSelected(index) }
            }

            if (orientation == Orientation.Horizontal) {
                Row(
                    modifier = modifier
                        .horizontalScroll(scrollState)
                        .padding(horizontal = style.edgePadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(style.itemSpacing)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Box(modifier = scrollableItemModifier(index), contentAlignment = Alignment.Center) {
                            SkyTabItemContent(
                                index = index,
                                tab = tab,
                                selected = index == actualSelectedIndex,
                                style = style,
                                itemContent = itemContent
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = style.edgePadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(style.itemSpacing)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Box(modifier = scrollableItemModifier(index), contentAlignment = Alignment.Center) {
                            SkyTabItemContent(
                                index = index,
                                tab = tab,
                                selected = index == actualSelectedIndex,
                                style = style,
                                itemContent = itemContent
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 解析单个 Tab 的最终内容：优先 [SkyTabItem.customContent]，其次 [itemContent] 插槽，最后默认样式。
 */
@Composable
private fun SkyTabItemContent(
    index: Int,
    tab: SkyTabItem,
    selected: Boolean,
    style: SkyTabStyle,
    itemContent: (@Composable (index: Int, tab: SkyTabItem, selected: Boolean) -> Unit)?,
) {
    val custom = tab.customContent
    when {
        custom != null -> custom(selected)
        itemContent != null -> itemContent(index, tab, selected)
        else -> DefaultSkyTabItem(tab = tab, selected = selected, style = style)
    }
}

/**
 * 默认 Tab 样式：图标在上（可带角标）+ 文字在下，选中态文字加粗。
 */
@Composable
private fun DefaultSkyTabItem(
    tab: SkyTabItem,
    selected: Boolean,
    style: SkyTabStyle,
) {
    val contentColor = if (selected) style.selectedTextColor else style.unselectedTextColor
    Column(
        modifier = Modifier.padding(style.itemPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 选中态图标为 null 时回落到未选中态；按 SkyIconType 类型分派渲染。
        val icon = if (selected) tab.selectedIcon ?: tab.icon else tab.icon
        if (icon != null) {
            val iconContent: @Composable () -> Unit = {
                when (icon) {
                    is SkyIconType.FontIcon -> SkyIconFont(
                        iconName = icon.iconName,
                        modifier = Modifier.size(style.iconSize),
                        tint = if (style.iconTint) contentColor else Color.Unspecified,
                        fontSize = with(LocalDensity.current) { style.iconSize.toSp() },
                        fontName = icon.fontName
                    )

                    is SkyIconType.PainterIcon -> Image(
                        painter = icon.painter,
                        contentDescription = tab.text,
                        modifier = Modifier.size(style.iconSize),
                        colorFilter = if (style.iconTint) ColorFilter.tint(contentColor) else null
                    )
                }
            }
            val badgeState = tab.badgeState
            if (badgeState != null) {
                SkyBadgeBox(
                    state = badgeState,
                    gravity = style.badgeGravity,
                    offset = style.badgeOffset,
                    backgroundColor = style.badgeBackgroundColor,
                    textColor = style.badgeTextColor,
                    showShadow = false
                ) { iconContent() }
            } else {
                iconContent()
            }
            Spacer(modifier = Modifier.height(style.iconTextSpacing))
        }
        Text(
            text = tab.text,
            color = contentColor,
            fontSize = style.textSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
