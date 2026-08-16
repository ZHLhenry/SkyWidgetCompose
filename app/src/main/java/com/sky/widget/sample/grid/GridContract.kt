package com.sky.widget.sample.grid

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 网格布局示例的 MVI 契约。
 *
 * 演示 [com.sky.widget.grid.SkyGridLayout] 时，列数 / 间距 / 内边距等属性
 * 由页面级状态持有，点击反馈文案（lastClick）也纳入 UiState，
 * 通过 intent 在 ViewModel 中统一变更，契合 SkyMVI 三件套范式。
 */
data class GridUiState(
    val columns: Int = 2,
    val horizontalSpacing: Int = 8,
    val verticalSpacing: Int = 8,
    val contentPadding: Int = 16,
    val lastClick: String? = null,
) : SkyUiState

sealed interface GridUiIntent : SkyUiIntent {
    data class SetColumns(val value: Int) : GridUiIntent
    data class SetHorizontalSpacing(val value: Int) : GridUiIntent
    data class SetVerticalSpacing(val value: Int) : GridUiIntent
    data class SetContentPadding(val value: Int) : GridUiIntent
    data class ClickItem(val message: String) : GridUiIntent
}

/**
 * 网格布局示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object GridUiEffect : SkyUiEffect
