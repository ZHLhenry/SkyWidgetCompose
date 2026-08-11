package com.sky.widget.refresh

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.sky.widget.refresh.footer.SkyClassicsRefreshFooter
import com.sky.widget.refresh.header.SkyClassicsRefreshHeader

/**
 * 运行时探测宿主是否引入了 paging-compose。
 * 注意：探测逻辑不引用任何 Paging 类型，避免在缺失时先触发 NoClassDefFoundError，
 * 与 [com.sky.widget.refresh.header.SkyLottieRefreshHeader] 的探测方式一致。
 */
private val isPagingComposeAvailable: Boolean by lazy {
    runCatching {
        Class.forName("androidx.paging.compose.LazyPagingItems")
    }.isSuccess
}

/**
 * 配合 AndroidX Paging 3 使用的刷新容器状态。
 *
 * 本质上就是 [SkyRefreshState]（内部状态机完全一致），仅提供语义化别名，
 * 让调用方在 Paging 场景下能直观区分「普通刷新」与「Paging 刷新」两种用法。
 *
 * 容器本身不感知数据来源，是否到底（noMoreData）由 [LazyPagingItems.loadState]
 * 决定，本状态仅负责在下拉/上拉动作结束后复位动画。
 */
@Composable
fun rememberSkyRefreshPagingState(): SkyRefreshState = rememberSkyRefreshState()

/**
 * 将 [SkyRefreshState] 与 Paging 3 的 [LazyPagingItems] 绑定。
 *
 * 通过监听 `loadState` 在合适的时机调用 [SkyRefreshState.finish] 复位动画：
 * - 下拉刷新（[LoadState.refresh]）结束 → finish(noMoreData = 列表为空)
 * - 上拉加载更多（[LoadState.append]）结束 → finish(noMoreData = 已到末页)
 *
 * 调用方仍可在 [SkyRefreshLayout] 的 `onRefresh`/`onLoadMore` 中自行处理业务，
 * 本绑定只负责把 Paging 的加载结果同步到刷新容器的视觉状态。
 *
 * 注意：必须在组合作用域内调用（内部使用 [LaunchedEffect] 监听加载状态）。
 */
@SuppressLint("ComposableNaming")
@Composable
fun SkyRefreshState.bindPaging(lazyPagingItems: LazyPagingItems<*>) {
    // 监听下拉刷新结束
    LaunchedEffect(lazyPagingItems.loadState.refresh) {
        if (lazyPagingItems.loadState.refresh is LoadState.NotLoading) {
            finish(noMoreData = lazyPagingItems.itemCount == 0)
        }
    }
    // 监听上拉加载结束
    LaunchedEffect(lazyPagingItems.loadState.append) {
        val append = lazyPagingItems.loadState.append
        if (append is LoadState.NotLoading) {
            finish(noMoreData = append.endOfPaginationReached)
        }
    }
}

/**
 * 基于 Paging 3 的开箱即用刷新列表容器。
 *
 * 内部封装了 [SkyRefreshLayout] + [LazyColumn] + [LazyPagingItems] 的胶水逻辑：
 * - 下拉触发 [LazyPagingItems.refresh]；
 * - 滚动到底由 Paging 自动触发下一页（`onLoadMore` 默认无需手动处理）；
 * - 加载结束自动调用 [SkyRefreshState.finish] 复位动画；
 * - 列表尾部自动渲染「加载中 / 加载失败（点击重试）/ 没有更多数据」。
 *
 * 与 [SkyRefreshLayout] 完全兼容：所有样式、Header/Footer 插槽、内容区插槽均可透传，
 * 现有非 Paging 用法不受影响。
 *
 * ## 依赖与导包
 *
 * Paging 在库中为 `compileOnly` 依赖，与 `SkyLottieRefreshHeader` 的 Lottie 处理方式一致，
 * 不会传递给消费者。使用前需在宿主模块自行导包：
 *
 * ```kotlin
 * // 消费者模块 build.gradle.kts
 * dependencies {
 *     implementation(libs.androidx.paging.compose)
 * }
 * ```
 *
 * 组件内部对 Paging 相关类做有运行时检查，若运行期缺失依赖会抛出带导包提示的
 * [IllegalStateException]，便于快速定位。
 *
 * ## 属性搭配关系（能力生效条件）
 *
 * 与 [SkyRefreshLayout] 一致，下列能力需先满足对应条件：
 * - **下拉刷新**：`onRefresh` 为 null 时由 [LazyPagingItems.refresh] 自动接管；传非空则走自定义逻辑。
 * - **上拉加载**：由 Paging 自动分页，无需设置 `onLoadMore`；`onLoadMore` 非空时改为走自定义逻辑。
 * - **无更多数据终态**：依赖 [LazyPagingItems.loadState.append.endOfPaginationReached]，
 *   并结合非空的 [noMoreDataText]（默认即有）展示「没有更多数据」提示。
 * - **二楼**：需同时满足 `secondFloorRate > 0` 且 `onSecondFloor != null`，见 [SkyRefreshLayout]。
 *
 * @param T 列表数据类型
 * @param lazyPagingItems Paging 3 的 `collectAsLazyPagingItems()` 结果
 * @param modifier 容器修饰符
 * @param state 刷新状态，默认 [rememberSkyRefreshPagingState]
 * @param orientation 排版方向，默认纵向
 * @param style 位移样式，见 [SkyRefreshStyle]
 * @param noMoreDataText 「没有更多数据」文案；为 null 时不展示终态提示
 * @param onRefresh 自定义下拉刷新回调；为 null 时使用 [LazyPagingItems.refresh]
 * @param onLoadMore 自定义上拉加载回调；为 null 时由 Paging 自动加载（空实现）
 * @param secondFloorRate 触发"二楼"的下拉比例（0f 表示不启用二楼），见 [SkyRefreshLayout]
 * @param onSecondFloor 进入二楼的回调；为 null 时不启用二楼
 * @param header 自定义下拉 Header；默认 [SkyClassicsRefreshHeader]
 * @param footer 自定义上拉 Footer；默认 [SkyClassicsRefreshFooter]
 * @param itemKey 列表项稳定 key（建议用数据唯一 ID）
 * @param itemContentType 列表项 contentType
 * @param content 列表项内容（接收 Paging 取出的数据对象）
 */
@Composable
fun <T : Any> SkyRefreshPagingLayout(
    lazyPagingItems: LazyPagingItems<T>,
    modifier: Modifier = Modifier,
    state: SkyRefreshState = rememberSkyRefreshPagingState(),
    orientation: Orientation = Orientation.Vertical,
    style: SkyRefreshStyle = SkyRefreshStyle.Translate,
    noMoreDataText: String? = "没有更多数据",
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    secondFloorRate: Float = 0f,
    onSecondFloor: (() -> Unit)? = null,
    header: @Composable () -> Unit = {
        SkyClassicsRefreshHeader(flag = state.refreshFlag, orientation = orientation)
    },
    footer: @Composable () -> Unit = {
        SkyClassicsRefreshFooter(
            flag = state.loadMoreFlag,
            noMoreData = state.noMoreData,
            orientation = orientation,
            noMoreText = noMoreDataText
        )
    },
    itemKey: ((item: T) -> Any)? = null,
    itemContentType: ((item: T) -> Any)? = null,
    content: @Composable (T) -> Unit
) {
    check(isPagingComposeAvailable) {
        "SkyRefreshPagingLayout 依赖 AndroidX Paging。" +
            "请消费者自行添加依赖：implementation(\"androidx.paging:paging-compose:版本号\")"
    }
    // 绑定 Paging 加载状态到刷新容器（自动 finish 复位）
    state.bindPaging(lazyPagingItems)

    SkyRefreshLayout(
        modifier = modifier,
        state = state,
        orientation = orientation,
        style = style,
        onRefresh = { onRefresh?.invoke() ?: lazyPagingItems.refresh() },
        // SkyRefreshLayout 要求 onLoadMore 非空才启用上拉手势；Paging 自动加载，这里传空 lambda
        onLoadMore = { onLoadMore?.invoke() ?: Unit },
        secondFloorRate = secondFloorRate,
        onSecondFloor = onSecondFloor,
        noMoreDataText = noMoreDataText,
        header = header,
        footer = footer
    ) {
        LazyColumn {
            // parComposable: 列表主体，key/type 优先用调用方提供的稳定映射
            items(
                count = lazyPagingItems.itemCount,
                key = if (itemKey != null) {
                    lazyPagingItems.itemKey(itemKey)
                } else {
                    lazyPagingItems.itemKey { it }
                },
                contentType = if (itemContentType != null) {
                    lazyPagingItems.itemContentType(itemContentType)
                } else {
                    lazyPagingItems.itemContentType { it }
                }
            ) { index ->
                val item = lazyPagingItems[index] ?: return@items
                content(item)
            }
            // parComposable: append 加载中/出错时的底部提示行
            pagingAppendFooter(lazyPagingItems, noMoreDataText)
        }
    }
}

/**
 * 在 [androidx.compose.foundation.lazy.LazyListScope] 中渲染 Paging append（上拉加载）的尾部状态。
 *
 * - 加载中：显示转圈提示
 * - 出错：显示错误文案（点击重试重新触发 Paging 下一页）
 * - 已到末页：根据 [noMoreDataText] 是否非空决定是否显示「没有更多数据」
 */
private fun androidx.compose.foundation.lazy.LazyListScope.pagingAppendFooter(
    lazyPagingItems: LazyPagingItems<*>,
    noMoreDataText: String?
) {
    val append = lazyPagingItems.loadState.append
    when {
        append is LoadState.Loading -> {
            item(key = "paging_append_loading", contentType = "paging_append") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(text = "加载中...") }
            }
        }
        append is LoadState.Error -> {
            item(key = "paging_append_error", contentType = "paging_append") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clickable { lazyPagingItems.retry() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "加载失败，点击重试", modifier = Modifier.padding(8.dp))
                }
            }
        }
        append is LoadState.NotLoading && append.endOfPaginationReached && !noMoreDataText.isNullOrEmpty() -> {
            item(key = "paging_append_end", contentType = "paging_append") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(text = noMoreDataText) }
            }
        }
    }
}
