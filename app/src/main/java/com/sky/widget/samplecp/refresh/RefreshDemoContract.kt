package com.sky.widget.samplecp.refresh

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 刷新示例子页面（仅刷新 / 仅加载 / 无更多隐藏 / 吸顶 / 横向 / 自定义指示器 /
 * 自动触发 / 各类 Header / 二楼 / 固定模式 等）共享的 MVI 契约。
 *
 * 这些页面都是 SkyRefreshLayout 的能力演示，数据在内存中模拟，没有真实网络层。
 * [RefreshDemoUiState] 集中持有列表内容与各刷新能力开关；
 * [RefreshDemoUiIntent] 表达刷新 / 加载更多 / 重建数据三类用户意图。
 */
data class RefreshDemoUiState(
    val items: List<String> = List(15) { "列表项 #${it + 1}" },
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** 是否允许下拉刷新 */
    val enableRefresh: Boolean = true,
    /** 是否允许上拉加载更多 */
    val enableLoadMore: Boolean = true,
    /** 自动加载更多阈值（px），>0 时启用 */
    val autoLoadMoreThreshold: Int = 0,
    /** 吸顶阻尼等级（0~1），仅演示用 */
    val stickinessLevel: Float = 0.5f,
    /** 二楼触发比例，>0 时启用二楼 */
    val secondFloorRate: Float = 0f,
    /** 无更多数据文案 */
    val noMoreDataText: String = "没有更多数据了",
) : SkyUiState

sealed interface RefreshDemoUiIntent : SkyUiIntent {
    /** 用户下拉触发刷新 */
    data object Refresh : RefreshDemoUiIntent

    /** 用户上拉触发加载更多 */
    data object LoadMore : RefreshDemoUiIntent

    /** 数据到达末尾（由 UI 根据阈值判断后派发，用于切换 noMoreData 终态） */
    data object ReachEnd : RefreshDemoUiIntent
}

/**
 * 刷新示例子页面暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object RefreshDemoUiEffect : SkyUiEffect
