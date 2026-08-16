package com.sky.widget.sample.annotatedtext

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 高亮文字示例 ViewModel。
 *
 * 仅收集高亮文本点击意图，无状态变更，维持 MVI 范式一致性。
 */
@HiltViewModel
class AnnotatedTextViewModel @Inject constructor() :
    SkyBaseMviViewModel<AnnotatedTextUiState, AnnotatedTextUiIntent, AnnotatedTextUiEffect>() {

    override fun initialState() = AnnotatedTextUiState

    override fun handleIntent(intent: AnnotatedTextUiIntent) {
        // 高亮文字页面无业务状态变更，点击意图可直接在此处理（如埋点），此处仅占位
    }
}
