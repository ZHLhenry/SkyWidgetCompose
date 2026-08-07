package com.sky.widget.refresh

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import com.sky.widget.refresh.footer.SkyClassicsRefreshFooter
import com.sky.widget.refresh.header.SkyClassicsRefreshHeader

/**
 * 核心刷新容器（基于原生 Layout 与硬件加速的复合渲染引擎）。
 *
 * 作为一个具备泛用性与极简状态机的高级嵌套滑动容器，其核心架构特性如下：
 * 1. **多维排版兼容**：通过 [Orientation] 动态重定向约束与平移向量，完美支持纵向（Vertical）与横向（Horizontal）的滚动场景。
 * 2. **无侵入式解耦**：不依赖或侵入任何具体的列表实现，通过标准的 [NestedScrollConnection] 拦截手势，
 *    对任意支持嵌套滚动的子组件（如 `LazyColumn`, `LazyRow`, `HorizontalPager` 等）实现透明包裹。
 * 3. **纯净硬件加速**：摒弃传统改变约束导致的高频 UI 重排（Relayout），全程依靠 [graphicsLayer] 的图层平移
 *    处理交互位移，将 CPU 开销降至极低，实现零卡顿的顺滑手势跟随。
 * 4. **预布局防闪动**：在底层的 [Layout] 测量阶段优先对首尾装饰层完成布局测量，彻底消除首帧渲染时的组件闪烁问题。
 * 5. **三层结构设计**：Header / Content / Footer 三层独立测量与放置，通过 [layoutId] 精准识别，
 *    支持灵活的样式切换（[SkyRefreshStyle]）而不影响内部布局逻辑。
 *
 * @param modifier 外部修饰符
 * @param state 核心状态机引擎，控制并追踪当前的阻尼平移量与生命周期状态
 * @param orientation 排版方向。决定了手势拦截的作用轴以及 Header/Footer 的挂载方位
 * @param style 位移样式 [SkyRefreshStyle]：默认 [SkyRefreshStyle.Translate] 内容跟随；
 * [SkyRefreshStyle.FixedContent] 时下拉刷新侧内容固定、Header 滑入覆盖；
 * [SkyRefreshStyle.FixedFront] 时 Header 固定悬浮在内容顶层原位，下拉仅驱动 Header 内部动画
 * @param onRefresh 下拉刷新（或向右拖拽）触发的异步回调
 * @param onLoadMore 上拉加载（或向左拖拽）触发的异步回调
 * @param secondFloorRate 二楼触发倍率：> 0 时启用“下拉进入二楼”，
 * 松手偏移超过 `headerBound × secondFloorRate` 则触发 [onSecondFloor] 而非刷新；默认 0 关闭
 * @param onSecondFloor 进入二楼回调（参考 SmartRefreshLayout TwoLevelHeader），
 * 由业务层自行展示二楼内容（覆盖层/跳转等）；为 null 时二楼不生效
 * @param noMoreDataText 业务层定制的“无更多数据”文案；**不传则终态不展示任何提示 UI**
 * @param header 自定义头部渲染器，默认提供了一个标准的旋转指示器 [SkyClassicsRefreshHeader]
 * @param footer 自定义尾部渲染器，默认提供了一个具备数据穷尽停靠特效的 [SkyClassicsRefreshFooter]
 * @param content 支持嵌套滚动事件分发的内容主体
 */
@Composable
fun SkyRefreshLayout(
    modifier: Modifier = Modifier,
    state: SkyRefreshState,
    orientation: Orientation = Orientation.Vertical,
    style: SkyRefreshStyle = SkyRefreshStyle.Translate,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    secondFloorRate: Float = 0f,
    onSecondFloor: (() -> Unit)? = null,
    noMoreDataText: String? = null,
    header: @Composable () -> Unit = { SkyClassicsRefreshHeader(flag = state.refreshFlag, orientation = orientation) },
    footer: @Composable () -> Unit = {
        SkyClassicsRefreshFooter(
            flag = state.loadMoreFlag,
            noMoreData = state.noMoreData,
            orientation = orientation,
            noMoreText = noMoreDataText
        )
    },
    content: @Composable () -> Unit
) {
    val isVertical = orientation == Orientation.Vertical
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val containerSize = if (isVertical) constraints.maxHeight else constraints.maxWidth

        // 绑定无状态耦合的嵌套滑动连接器
        val connection = remember(state, orientation, containerSize, scope, onRefresh, onLoadMore, secondFloorRate, onSecondFloor) {
            // 将业务回调注入状态机，供 autoRefresh / autoLoadMore 程序化触发时派发
            state.refreshAction = { onRefresh?.invoke() }
            state.loadMoreAction = { onLoadMore?.invoke() }
            SkyRefreshNestedScrollConnection(
                state = state,
                orientation = orientation,
                containerSize = containerSize,
                scope = scope,
                onRefresh = { onRefresh?.invoke() },
                onLoadMore = { onLoadMore?.invoke() },
                secondFloorRate = secondFloorRate,
                onSecondFloor = onSecondFloor
            )
        }

        Layout(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection)
                .clipToBounds(),
            content = {
                // 通过 layoutId 为子组件打标，便于在测量与放置阶段进行精准识别
                Box(modifier = Modifier.layoutId("header")) { header() }
                Box(modifier = Modifier.layoutId("content")) { content() }
                Box(modifier = Modifier.layoutId("footer")) { footer() }
            }
        ) { measurables, constraints ->
            val safeConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            val headerPlaceable = measurables.first { it.layoutId == "header" }.measure(safeConstraints)
            val footerPlaceable = measurables.first { it.layoutId == "footer" }.measure(safeConstraints)

            state.headerBound = if (isVertical) headerPlaceable.height.toFloat() else headerPlaceable.width.toFloat()
            state.footerBound = if (isVertical) footerPlaceable.height.toFloat() else footerPlaceable.width.toFloat()

            // ──────────────────────────────────────────────────────────────────
            // 【核心渲染引擎：基于 graphicsLayer 的零重绘位移架构】
            //
            // 架构优势：
            // 1. 静态约束测量：Content 组件在布局阶段始终保持全屏测量，不因拖拽发生约束变更，
            //    从根源上杜绝了因频繁触发 re-measurement 带来的 CPU 性能损耗与 UI 闪跳。
            // 2. 硬件加速平移：Header、Content、Footer 三层结构严格绑定指示器状态，
            //    仅在 Draw 阶段通过修改 translation 属性进行图层级别移动。
            // 3. 边界动态扩容：当且仅当发生解耦驻留时，容器通过 computed property (`addedHeight`)
            //    动态延展物理渲染范围，确保数据增量无缝衔接，实现媲美原生底层的连贯滚动体验。
            // ──────────────────────────────────────────────────────────────────

            // 动态注入扩容尺寸，给新追加的数据提供物理渲染空间
            val contentConstraints = if (isVertical) {
                constraints.copy(
                    minHeight = constraints.maxHeight + state.addedHeight,
                    maxHeight = constraints.maxHeight + state.addedHeight
                )
            } else {
                constraints.copy(
                    minWidth = constraints.maxWidth + state.addedHeight,
                    maxWidth = constraints.maxWidth + state.addedHeight
                )
            }

            val contentPlaceable = measurables.first { it.layoutId == "content" }.measure(contentConstraints)

            // FixedContent / FixedFront 样式：下拉刷新侧（正偏移）内容固定不动，
            // 上拉加载侧（负偏移）保持原有的跟随/解耦行为；
            // Header 放置顺序在 Content 之后，天然绘制在其上层，形成滑入覆盖效果
            val contentFixed = style != SkyRefreshStyle.Translate
            val contentOffset = if (contentFixed) {
                state.currentContentOffset.coerceAtMost(0f)
            } else {
                state.currentContentOffset
            }
            // FixedFront 样式：Header 固定在容器边缘原位（绘制在 Content 上层），不随偏移滑入
            val headerFixed = style == SkyRefreshStyle.FixedFront

            layout(constraints.maxWidth, constraints.maxHeight) {
                if (isVertical) {
                    val cxHeader = (constraints.maxWidth - headerPlaceable.width) / 2
                    val cxFooter = (constraints.maxWidth - footerPlaceable.width) / 2

                    // Content 应用可能被解耦的平移量
                    contentPlaceable.placeWithLayer(0, 0) {
                        translationY = contentOffset
                    }
                    headerPlaceable.placeWithLayer(
                        x = cxHeader,
                        y = if (headerFixed) 0 else -headerPlaceable.height
                    ) {
                        translationY = if (headerFixed) 0f else state.indicatorOffset
                    }
                    // 永远置于真实的屏幕底部，配合 indicatorOffset 平移
                    footerPlaceable.placeWithLayer(x = cxFooter, y = constraints.maxHeight) {
                        translationY = state.indicatorOffset
                    }
                } else {
                    val cyHeader = (constraints.maxHeight - headerPlaceable.height) / 2
                    val cyFooter = (constraints.maxHeight - footerPlaceable.height) / 2

                    contentPlaceable.placeWithLayer(0, 0) {
                        translationX = contentOffset
                    }
                    headerPlaceable.placeWithLayer(
                        x = if (headerFixed) 0 else -headerPlaceable.width,
                        y = cyHeader
                    ) {
                        translationX = if (headerFixed) 0f else state.indicatorOffset
                    }
                    footerPlaceable.placeWithLayer(x = constraints.maxWidth, y = cyFooter) {
                        translationX = state.indicatorOffset
                    }
                }
            }
        }
    }
}
