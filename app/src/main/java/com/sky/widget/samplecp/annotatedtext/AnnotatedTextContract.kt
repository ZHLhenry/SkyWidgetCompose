package com.sky.widget.samplecp.annotatedtext

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 高亮文字示例的 MVI 契约。
 *
 * 该页面演示 [com.sky.widget.text.SkyAnnotatedText] 的高亮与点击能力，
 * 点击反馈文案（lastClick）纳入 UiState，通过 intent 在 ViewModel 中统一变更。
 */
data class AnnotatedTextUiState(
    /** 最近一次点击高亮文本匹配到的内容，null 表示尚未点击 */
    val lastClick: String? = null,
) : SkyUiState

sealed interface AnnotatedTextUiIntent : SkyUiIntent {
    /** 高亮文本被点击，携带实际匹配到的内容 */
    data class AnnotationClick(val matched: String) : AnnotatedTextUiIntent
}

/**
 * 高亮文字示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object AnnotatedTextUiEffect : SkyUiEffect
