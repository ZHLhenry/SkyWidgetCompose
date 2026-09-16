package com.sky.widget.samplecp.grid

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 网格布局示例 ViewModel。
 *
 * 持有列数 / 间距 / 内边距与点击反馈文案，按 intent 在限定范围内变更。
 */
@HiltViewModel
class GridViewModel @Inject constructor() :
    SkyBaseMviViewModel<GridUiState, GridUiIntent, GridUiEffect>() {

    override fun initialState() = GridUiState()

    override fun handleIntent(intent: GridUiIntent) {
        when (intent) {
            is GridUiIntent.SetColumns ->
                setState { copy(columns = intent.value.coerceIn(1, 5)) }

            is GridUiIntent.SetHorizontalSpacing ->
                setState { copy(horizontalSpacing = intent.value.coerceIn(0, 32)) }

            is GridUiIntent.SetVerticalSpacing ->
                setState { copy(verticalSpacing = intent.value.coerceIn(0, 32)) }

            is GridUiIntent.SetContentPadding ->
                setState { copy(contentPadding = intent.value.coerceIn(0, 32)) }

            is GridUiIntent.ClickItem ->
                setState { copy(lastClick = intent.message) }
        }
    }
}
