package com.sky.widget.samplecp.badge

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 徽章示例 ViewModel。
 *
 * 持有主徽章数值、方位选择、独立徽章数值；加减与方位选择由 UI 派发 intent 同步。
 */
@HiltViewModel
class BadgeViewModel @Inject constructor() :
    SkyBaseMviViewModel<BadgeUiState, BadgeUiIntent, BadgeUiEffect>() {

    override fun initialState() = BadgeUiState()

    override fun handleIntent(intent: BadgeUiIntent) {
        when (intent) {
            is BadgeUiIntent.AddBadge ->
                setState { copy(badgeNumber = badgeNumber + 1) }

            is BadgeUiIntent.MinusBadge ->
                setState { copy(badgeNumber = (badgeNumber - 1).coerceAtLeast(0)) }

            is BadgeUiIntent.SelectGravity ->
                setState { copy(gravityIndex = intent.index) }

            is BadgeUiIntent.AddStandalone ->
                setState { copy(standaloneNumber = standaloneNumber + 1) }

            is BadgeUiIntent.MinusStandalone ->
                setState { copy(standaloneNumber = (standaloneNumber - 1).coerceAtLeast(0)) }
        }
    }
}
