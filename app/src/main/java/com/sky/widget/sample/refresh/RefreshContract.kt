package com.sky.widget.sample.refresh

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 刷新示例（基础版）的 MVI 契约。
 *
 * 该页面是一个纯 UI 演示：列表数据在内存中模拟，没有真实网络层。
 * UiState 持有列表内容，UiIntent 表达刷新/加载更多的用户意图，
 * 具体的延时与数据变更由 ViewModel 在 reduce 中处理。
 */
data class RefreshUiState(
    val items: List<String> = List(15) { "列表项 #${it + 1}" },
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
) : SkyUiState

sealed interface RefreshUiIntent : SkyUiIntent {
    /** 用户下拉触发刷新 */
    data object Refresh : RefreshUiIntent

    /** 用户上拉触发加载更多 */
    data object LoadMore : RefreshUiIntent
}

/**
 * 刷新示例暂无需要跨层消费的副作用（如 Toast、跳转）。
 * 保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object RefreshUiEffect : SkyUiEffect
