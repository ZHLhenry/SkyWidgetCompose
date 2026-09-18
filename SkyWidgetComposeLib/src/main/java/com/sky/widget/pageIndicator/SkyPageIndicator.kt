package com.sky.widget.pageIndicator

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页面指示器：当前页与总页数均从 [state] 自动读取，零配置联动。
 *
 * 本函数只关心“布局”（按总页数重复绘制每一项、排列方向、间距、主轴居中），
 * 不关心“每一项长什么样”：
 * - 不传 [itemContent] 时使用默认圆点样式 [SkyDotIndicatorStyle]；
 * - 传 [itemContent] 时完全由消费者绘制，可选用内置的 [SkyUnderlineIndicatorStyle]、
 *   [SkyNumberIndicatorStyle]，[SkyDotIndicatorStyle]或自行绘制任意样式；
 * - 各样式自身的颜色/尺寸等参数在对应 Item 上配置（如 `SkyDotIndicatorStyle(selectedColor = ...)`）。
 *
 * [SkyPageIndexSource] 由分页/轮播状态实现（SkyViewPageState、SkyBannerState 均已实现），
 * 因此同一签名同时适配 SkyViewPage 与 SkyBanner；外部任意页码来源可经
 * [SkyPageIndexSource] 工厂函数一行适配。
 *
 * @param state 页码来源（分页/轮播状态），提供当前索引 State 与总页数。
 * @param modifier 作用于指示器根节点的修饰符。
 * @param orientation 排列方向，默认水平 [Orientation.Horizontal]。
 * @param spacing 相邻两项之间的间距，默认 6.dp。
 * @param itemContent 每一项的自定义绘制，接收 [SkyPageIndicatorScope]；为 null 时使用默认圆点。
 */
@Composable
fun SkyPageIndicator(
    state: SkyPageIndexSource,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    spacing: Dp = 6.dp,
    itemContent: (@Composable SkyPageIndicatorScope.() -> Unit)? = null,
) {
    val count = state.pageCount
    val currentIndex by state.currentIndexState
    val item: @Composable SkyPageIndicatorScope.() -> Unit = itemContent ?: { SkyDotIndicatorStyle() }
    val items: @Composable () -> Unit = {
        repeat(count) { i ->
            SkyPageIndicatorScope(count = count, currentIndex = currentIndex, index = i).item()
        }
    }
    if (orientation == Orientation.Horizontal) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { items() }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { items() }
    }
}

/**
 * 页码来源抽象：提供当前选中页索引的 [State] 与总页数。
 *
 * 分页/轮播状态（SkyViewPageState、SkyBannerState）实现本接口后，
 * 即可直接传给 [SkyPageIndicator] 实现零配置联动指示器。
 */
interface SkyPageIndexSource {
    /** 当前选中页索引的 [State]，随翻页自动更新。 */
    val currentIndexState: State<Int>

    /** 总页数。 */
    val pageCount: Int
}

/**
 * [SkyPageIndexSource] 工厂函数：把任意“当前索引 State + 总页数”适配为页码来源。
 *
 * 用于非 Sky 分页组件（如 HorizontalPager、自定义轮播）接入 [SkyPageIndicator]，例如：
 * ```
 * val source = SkyPageIndexSource(
 *     currentIndexState = remember { derivedStateOf { pagerState.currentPage } },
 *     pageCount = pagerState.pageCount,
 * )
 * SkyPageIndicator(state = source)
 * ```
 *
 * @param currentIndexState 当前选中页索引的 [State]。
 * @param pageCount 总页数。
 */
fun SkyPageIndexSource(
    currentIndexState: State<Int>,
    pageCount: Int,
): SkyPageIndexSource = object : SkyPageIndexSource {
    override val currentIndexState: State<Int> = currentIndexState
    override val pageCount: Int = pageCount
}

/**
 * 指示器每一项的绘制作用域。
 *
 * @property count 总页数。
 * @property currentIndex 当前选中页索引。
 * @property index 当前正在绘制的项的索引。
 */
@Immutable
class SkyPageIndicatorScope internal constructor(
    val count: Int,
    val currentIndex: Int,
    val index: Int,
) {
    /** 当前项是否为选中项。 */
    val selected: Boolean get() = index == currentIndex
}
