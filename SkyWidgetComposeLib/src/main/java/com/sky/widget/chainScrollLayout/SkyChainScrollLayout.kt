package com.sky.widget.chainScrollLayout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sky.widget.chainScrollLayout.internal.SkyChainAfterContentConnection
import com.sky.widget.chainScrollLayout.internal.SkyChainContentFirstConnection
import com.sky.widget.chainScrollLayout.internal.SkyContentFirstConnection

/**
 * 联动条在容器中所处的位置，同时决定手势滚动的方向。
 *
 * "联动条"指 [SkyChainScrollLayout] 中通过 `chainContent` 提供的、可跟随手势折叠 / 展开的部分，
 * 例如可折叠 AppBar、悬浮工具栏等；"内容区域"指内部可滚动的列表主体。
 */
enum class SkyChainScrollPosition(val orientation: Orientation) {
    /** 联动条位于容器顶部；沿垂直方向滚动（初始完全展开时偏移为 0）。 */
    Top(Orientation.Vertical),

    /** 联动条位于容器底部；沿垂直方向滚动（初始完全展开时偏移为 maxPx）。 */
    Bottom(Orientation.Vertical),

    /** 联动条位于容器起始边（LTR 布局为左侧）；沿水平方向滚动。 */
    Start(Orientation.Horizontal),

    /** 联动条位于容器结束边（LTR 布局为右侧）；沿水平方向滚动。 */
    End(Orientation.Horizontal),
    ;

    /** 当前方向是否为水平（[Start] / [End]）。 */
    fun isHorizontal(): Boolean = orientation == Orientation.Horizontal
}

/**
 * 联动模式：决定滚动能量在联动条与内容区域之间的分配策略。
 */
enum class SkyChainScrollMode {
    /**
     * 内容区域优先：内容先滚，滚到边界后剩余位移再驱动联动条。
     *
     * 典型场景：AppBar 折叠（先滚列表，列表到顶后向下拉再展开 AppBar）。
     * 仅支持 [SkyChainScrollPosition.Top] / [SkyChainScrollPosition.Start]；
     * 与 [SkyChainScrollPosition.Bottom] / [SkyChainScrollPosition.End] 组合会在初始化时抛异常。
     */
    ContentFirst,

    /**
     * 联动条优先：手势能量先驱动联动条，联动条到达边界后剩余位移再交给内容区域。
     *
     * 典型场景：滚动列表时联动条同步跟随位移，形成"整体滚动"的视觉效果（默认）。
     */
    ChainContentFirst,

    /**
     * 内容区域完全无法滚动后，才把溢出位移交给联动条。
     *
     * 与 [ContentFirst] 的区别：不追踪内容区域的累积滚动，仅处理内容边界外的溢出量，
     * 因此联动条只在内容真正到底 / 到顶时才会响应。
     */
    ChainAfterContent,
}

/**
 * 链式（联动）滚动布局。
 *
 * 让"联动条"（如可折叠 AppBar、底部工具栏）与"内容区域"（可滚动列表）共享同一次手势：
 * 通过 [NestedScrollConnection] 在滚动事件流中拦截位移，按 [mode] 分配给联动条或内容区域，
 * 实现类似 Material `TopAppBarScrollBehavior` 的折叠 / 展开效果，但不依赖具体列表实现，
 * 对 [androidx.compose.foundation.lazy.LazyColumn]、[androidx.compose.foundation.verticalScroll]、
 * [androidx.compose.foundation.horizontalScroll] 等任何支持嵌套滚动的组件均透明可用。
 *
 * 使用示例：
 * ```
 * val state = rememberSkyChainScrollState(
 *     minScrollPosition = 0.dp,
 *     maxScrollPosition = 120.dp,
 *     position = SkyChainScrollPosition.Top,
 * )
 * SkyChainScrollLayout(
 *     state = state,
 *     mode = SkyChainScrollMode.ChainContentFirst,
 *     chainContent = { s ->
 *         // 联动条：根据 s.getScrollPositionValue() 平移 / 缩放 / 变色
 *         AppBar(Modifier.offset { IntOffset(0, s.getScrollPositionValue().roundToInt()) })
 *     },
 * ) { s ->
 *     // 内容区域：需要为联动条预留 padding，避免遮挡
 *     LazyColumn(contentPadding = PaddingValues(top = 120.dp)) { ... }
 * }
 * ```
 *
 * @param state 联动滚动状态，见 [rememberSkyChainScrollState]；持有当前滚动位置与范围。
 * @param modifier 作用于外层容器的修饰符。
 * @param mode 联动模式，默认 [SkyChainScrollMode.ChainContentFirst]。
 * @param chainContent 联动条内容（AppBar / 工具栏等），接收 [SkyChainScrollState] 用于读取滚动位置驱动 UI；
 *   会被绘制在 [content] 上层。
 * @param content 内容区域（LazyColumn 等）；接收 [BoxScope] 与 [SkyChainScrollState]，
 *   需要自行设置对应方向的 padding 为联动条让出空间。
 */
@Composable
fun SkyChainScrollLayout(
    state: SkyChainScrollState,
    modifier: Modifier = Modifier,
    mode: SkyChainScrollMode = SkyChainScrollMode.ChainContentFirst,
    chainContent: @Composable (SkyChainScrollState) -> Unit,
    content: @Composable BoxScope.(SkyChainScrollState) -> Unit,
) {
    // 根据联动模式创建对应的嵌套滚动连接器；state 或 mode 变化时重建
    val connection = remember(state, mode) {
        when (mode) {
            SkyChainScrollMode.ContentFirst -> SkyContentFirstConnection(state)
            SkyChainScrollMode.ChainContentFirst -> SkyChainContentFirstConnection(state)
            SkyChainScrollMode.ChainAfterContent -> SkyChainAfterContentConnection(state)
        }
    }

    Box(
        // 拦截内容区域的滚动事件，按 mode 分配给联动条
        modifier = modifier
            .nestedScroll(connection)
            // 裁剪越界部分，避免联动条平移到容器外时依然可见
            .clipToBounds(),
        // 根据 position 把联动条对齐到对应边缘，实现"贴边折叠"的默认布局
        contentAlignment = when (state.position) {
            SkyChainScrollPosition.Top -> Alignment.TopCenter
            SkyChainScrollPosition.Bottom -> Alignment.BottomCenter
            SkyChainScrollPosition.Start -> Alignment.CenterStart
            SkyChainScrollPosition.End -> Alignment.CenterEnd
        },
    ) {
        // 先绘制内容区域（下层），再绘制联动条（上层），保证联动条覆盖内容
        content(state)
        chainContent(state)
    }
}
