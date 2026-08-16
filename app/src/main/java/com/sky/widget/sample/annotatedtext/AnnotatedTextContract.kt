package com.sky.widget.sample.annotatedtext

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 高亮文字示例的 MVI 契约。
 *
 * 该页面仅演示 [com.sky.widget.text.SkyAnnotatedText] 的高亮与点击能力，
 * 无业务状态，使用空 [AnnotatedTextUiState] 维持 SkyMVI 三件套范式。
 */
data object AnnotatedTextUiState : SkyUiState

sealed interface AnnotatedTextUiIntent : SkyUiIntent {
    /** 高亮文本被点击，携带实际匹配到的内容 */
    data class AnnotationClick(val matched: String) : AnnotatedTextUiIntent
}

/**
 * 高亮文字示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object AnnotatedTextUiEffect : SkyUiEffect
