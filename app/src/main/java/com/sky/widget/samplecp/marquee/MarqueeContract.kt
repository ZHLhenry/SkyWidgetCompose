package com.sky.widget.samplecp.marquee

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 跑马灯（轮播）示例的 MVI 契约。
 *
 * 演示 [com.sky.widget.marqueeView.SkyMarqueeView] 的播放控制；
 * 当前索引由 [com.sky.widget.marqueeView.SkyMarqueeState] 自身持有，
 * 这里管理播放状态（是否轮播中）与方向选择等页面级状态。
 */
data class MarqueeUiState(
    val isPlaying: Boolean = true,
    val directionIndex: Int = 0,
) : SkyUiState

sealed interface MarqueeUiIntent : SkyUiIntent {
    /** 切换播放 / 暂停 */
    data object TogglePlay : MarqueeUiIntent

    /** 切换轮播方向（0=上 1=下 2=左 3=右） */
    data class SetDirection(val index: Int) : MarqueeUiIntent
}

/**
 * 跑马灯示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object MarqueeUiEffect : SkyUiEffect
