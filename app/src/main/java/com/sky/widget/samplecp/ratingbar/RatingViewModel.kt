package com.sky.widget.samplecp.ratingbar

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 评分条示例 ViewModel。
 *
 * 持有各示例卡片的分值，按 intent 在 [0, 5] 范围内变更。
 */
@HiltViewModel
class RatingViewModel @Inject constructor() :
    SkyBaseMviViewModel<RatingUiState, RatingUiIntent, RatingUiEffect>() {

    override fun initialState() = RatingUiState()

    override fun handleIntent(intent: RatingUiIntent) {
        when (intent) {
            is RatingUiIntent.UpdateBasic ->
                setState { copy(basicRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateHalf ->
                setState { copy(halfRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateVectorIcon ->
                setState { copy(vectorIconRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateCustomIcon ->
                setState { copy(customIconRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateCustomIconHalf ->
                setState { copy(customIconHalfRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateCustomStyle ->
                setState { copy(customStyleRating = intent.value.coerceIn(0f, 5f)) }

            is RatingUiIntent.UpdateCustomStyleHalf ->
                setState { copy(customStyleHalfRating = intent.value.coerceIn(0f, 5f)) }
        }
    }
}
