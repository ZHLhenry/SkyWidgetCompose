package com.sky.widget.samplecp.statelayout

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState
import com.sky.widget.stateLayout.SkyPageState

/**
 * 页面状态布局示例的 MVI 契约。
 *
 * 演示 [SkyPageState] 的 Loading / Success / Empty / Error 四种状态切换，
 * 数据在内存中模拟，没有真实网络层。
 */
data class StateLayoutUiState(
    val pageState: SkyPageState = SkyPageState.Loading,
) : SkyUiState

sealed interface StateLayoutUiIntent : SkyUiIntent {
    /** 切换为加载中 */
    data object ToLoading : StateLayoutUiIntent

    /** 切换为成功（展示内容） */
    data object ToSuccess : StateLayoutUiIntent

    /** 切换为空数据 */
    data object ToEmpty : StateLayoutUiIntent

    /** 切换为错误 */
    data object ToError : StateLayoutUiIntent

    /** 重试：从错误 / 空回到加载中 */
    data object Retry : StateLayoutUiIntent
}

/**
 * 页面状态示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object StateLayoutUiEffect : SkyUiEffect
