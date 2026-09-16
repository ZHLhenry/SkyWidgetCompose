package com.sky.widget.samplecp.ratingbar

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 评分条示例的 MVI 契约。
 *
 * 演示 [com.sky.widget.ratingbar.SkyRatingBar] 时，各示例卡片的分值由页面级状态持有，
 * 通过 intent 在 ViewModel 中统一变更，契合 SkyMVI 三件套范式。
 */
data class RatingUiState(
    val basicRating: Float = 0f,
    val halfRating: Float = 2.5f,
    val vectorIconRating: Float = 3f,
    val customIconRating: Float = 3f,
    val customIconHalfRating: Float = 3.5f,
    val customStyleRating: Float = 3f,
    val customStyleHalfRating: Float = 2.5f,
) : SkyUiState

sealed interface RatingUiIntent : SkyUiIntent {
    data class UpdateBasic(val value: Float) : RatingUiIntent
    data class UpdateHalf(val value: Float) : RatingUiIntent
    data class UpdateVectorIcon(val value: Float) : RatingUiIntent
    data class UpdateCustomIcon(val value: Float) : RatingUiIntent
    data class UpdateCustomIconHalf(val value: Float) : RatingUiIntent
    data class UpdateCustomStyle(val value: Float) : RatingUiIntent
    data class UpdateCustomStyleHalf(val value: Float) : RatingUiIntent
}

/**
 * 评分条示例暂无跨层副作用，保留空 Effect 以契合 SkyMVI 三件套范式。
 */
data object RatingUiEffect : SkyUiEffect
