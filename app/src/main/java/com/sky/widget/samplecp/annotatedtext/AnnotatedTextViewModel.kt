package com.sky.widget.samplecp.annotatedtext

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 高亮文字示例 ViewModel。
 *
 * 收集高亮文本点击意图并更新点击反馈文案，维持 MVI 范式一致性。
 */
@HiltViewModel
class AnnotatedTextViewModel @Inject constructor() :
    SkyBaseMviViewModel<AnnotatedTextUiState, AnnotatedTextUiIntent, AnnotatedTextUiEffect>() {

    override fun initialState() = AnnotatedTextUiState()

    override fun handleIntent(intent: AnnotatedTextUiIntent) {
        when (intent) {
            is AnnotatedTextUiIntent.AnnotationClick ->
                setState { copy(lastClick = intent.matched) }
        }
    }
}
