package com.sky.widget.sample.badge

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 徽章示例的 MVI 契约。
 *
 * 演示 [com.sky.widget.badge.SkyBadgeBox] / [com.sky.widget.badge.SkyBadgeView]
 * 的数字控制、九宫格方位、拖拽消除等能力。徽章数字等由本页面级状态持有，
 * 拖拽消除的瞬时动画由 SkyBadgeState 自身负责，这里管理数字与方位选择。
 */
data class BadgeUiState(
    /** 主徽章数值（0 隐藏、负数圆点、>maxNumber 显示 maxNumber+） */
    val badgeNumber: Int = 8,
    /** 已选方位索引（对应 SkyBadgeGravity 九宫格） */
    val gravityIndex: Int = 1,
    /** 独立徽章数值 */
    val standaloneNumber: Int = 99,
) : SkyUiState

sealed interface BadgeUiIntent : SkyUiIntent {
    /** 主徽章数值 +1 */
    data object AddBadge : BadgeUiIntent

    /** 主徽章数值 -1（不低于 0） */
    data object MinusBadge : BadgeUiIntent

    /** 选择方位 */
    data class SelectGravity(val index: Int) : BadgeUiIntent

    /** 独立徽章数值 +1 */
    data object AddStandalone : BadgeUiIntent

    /** 独立徽章数值 -1 */
    data object MinusStandalone : BadgeUiIntent
}

/**
 * 徽章示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object BadgeUiEffect : SkyUiEffect
