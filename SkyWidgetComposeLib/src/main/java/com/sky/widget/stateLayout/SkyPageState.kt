package com.sky.widget.stateLayout

import androidx.compose.runtime.Stable

/**
 * 页面状态枚举。
 *
 * @see SkyPageStateLayout
 */
@Stable
sealed interface SkyPageState {
    /** 加载中 */
    data object Loading : SkyPageState

    /** 加载成功，将渲染业务内容 */
    data object Success : SkyPageState

    /**
     * 空数据。
     *
     * @param message 提示文案，为空时使用占位默认文案
     */
    data class Empty(val message: String = "") : SkyPageState

    /**
     * 加载失败。
     *
     * @param message 错误提示文案，为空时使用占位默认文案
     */
    data class Error(val message: String = "") : SkyPageState
}
