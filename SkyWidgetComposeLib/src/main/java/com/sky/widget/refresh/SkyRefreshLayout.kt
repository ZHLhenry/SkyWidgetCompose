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
 * 核心刷新容器。
 *
 * 基于原生 [Layout] 与 [graphicsLayer] 实现的嵌套滑动刷新/加载容器，
 * 通过标准的 [NestedScrollConnection] 拦截手势，对 [LazyColumn]、[LazyRow]、[HorizontalPager] 等
 * 支持嵌套滚动的子组件实现透明包裹，不依赖具体列表实现。
 *
 * ## 能力生效条件（属性搭配关系）
 *
 * 以下列出各能力需要同时满足的条件，避免只传部分参数导致功能不生效。
 *
 * ### 1. 下拉刷新
 * 需要同时满足：
 * - [state] 的 [SkyRefreshState.enableRefresh] 为 `true`（默认 `true`）
 * - 传入非空的 [onRefresh]
 * - Header 实际有测量高度（默认 [SkyClassicsRefreshHeader] 已满足）
 *
 * ### 2. 上拉加载
 * 需要同时满足：
 * - [state] 的 [SkyRefreshState.enableLoadMore] 为 `true`（默认 `true`）
 * - 传入非空的 [onLoadMore]
 * - Footer 实际有测量高度（默认 [SkyClassicsRefreshFooter] 已满足）
 *
 * ### 3. 无更多数据终态提示
 * 需要同时满足：
 * - 在 [onLoadMore] 回调中调用 `state.finish(noMoreData = true)`
 * - 传入非空的 [noMoreDataText]
 *
 * 若 [noMoreDataText] 为 `null`，Footer 将零尺寸渲染，用户滑动到底后无法拉出“没有更多数据”提示。
 *
 * ### 4. 下拉进入二楼
 * 需要同时满足：
 * - 传入 [secondFloorRate] > 0f（建议 >= 1.5f，常用 2.0f）
 * - 传入非空的 [onSecondFloor]
 *
 * 二级阈值 = Header 高度 × [secondFloorRate]。手指下拉越过该阈值后松手，会回调 [onSecondFloor]，
 * Header 回弹隐藏，**不会触发 [onRefresh]**。
 *
 * 注意：当 [secondFloorRate] <= 1.0f 时，二楼阈值会低于或等于普通刷新阈值，普通下拉刷新将无法触发。
 * 二楼 Header 推荐使用 [com.sky.widget.refresh.header.SkyTwoLevelRefreshHeader]；
 * 若使用自定义 Header，可监听 [SkyRefreshState.indicatorOffset] 自行换算二楼进度。
 *
 * ### 5. 样式与 Header 的协作
 * [style] 只影响下拉刷新侧的视觉表现：
 * - [SkyRefreshStyle.Translate]：Content 跟随下移（默认）
 * - [SkyRefreshStyle.FixedContent]：Content 固定，Header 滑入覆盖在 Content 之上
 * - [SkyRefreshStyle.FixedFront]：Header 固定在容器边缘原位，Content 不动，仅 Header 内部动画反馈下拉状态
 *
 * [SkyRefreshStyle.FixedFront] 建议搭配能根据 [SkyRefreshState.indicatorOffset] 驱动内部进度的 Header，
 * 例如 [com.sky.widget.refresh.header.SkyCircleRefreshHeader]、
 * [com.sky.widget.refresh.header.SkyProgressRefreshHeader]。
 *
 * ### 6. 程序化触发
 * 通过 [SkyRefreshState.autoRefresh] / [SkyRefreshState.autoLoadMore] 主动触发时，
 * 同样需要传入非空的 [onRefresh] / [onLoadMore]，否则不会向业务层派发回调。
 *
 * ## 核心实现特点
 * 1. **多维排版兼容**：通过 [Orientation] 动态重定向约束与平移向量，支持纵向与横向。
 * 2. **无侵入式解耦**：不依赖具体列表实现，透明包裹支持嵌套滚动的子组件。
 * 3. **硬件加速平移**：通过 [graphicsLayer] 处理位移，避免频繁重排。
 * 4. **预布局防闪动**：[Layout] 测量阶段优先对首尾装饰层完成测量，消除首帧闪烁。
 * 5. **三层结构设计**：Header / Content / Footer 通过 [layoutId] 识别，支持样式切换。
 *
 * @param modifier 外部修饰符
 * @param state 核心状态机，控制阻尼、开关、位移、状态流转等，通常由 [rememberSkyRefreshState] 创建
 * @param orientation 排版方向，决定手势拦截轴与 Header/Footer 挂载方位
 * @param style 下拉刷新侧的位移样式
 * @param onRefresh 下拉刷新触发的异步回调；需要同时满足 [SkyRefreshState.enableRefresh] 为 `true`
 * @param onLoadMore 上拉加载触发的异步回调；需要同时满足 [SkyRefreshState.enableLoadMore] 为 `true`
 * @param secondFloorRate 二楼触发倍率。> 0 时启用“下拉进入二楼”，松手偏移超过 `Header 高度 × secondFloorRate`
 * 则回调 [onSecondFloor] 而非刷新。默认 0 关闭
 * @param onSecondFloor 进入二楼回调，由业务层自行展示二楼内容（覆盖层 / 跳转等）。必须同时设置 [secondFloorRate] > 0 才生效
 * @param noMoreDataText “没有更多数据”文案。为 `null` 时终态不展示任何提示 UI
 * @param header 自定义头部渲染器，默认 [SkyClassicsRefreshHeader]
 * @param footer 自定义尾部渲染器，默认 [SkyClassicsRefreshFooter]
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

        // 构建嵌套滑动连接器，并把业务回调注入状态机，供手势拦截与 autoRefresh/autoLoadMore 共用
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
