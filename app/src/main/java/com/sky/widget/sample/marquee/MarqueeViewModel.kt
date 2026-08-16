package com.sky.widget.sample.marquee

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 跑马灯示例 ViewModel。
 *
 * 持有播放状态与方向索引；播放 / 暂停、方向切换由 UI 派发 intent 同步。
 */
@HiltViewModel
class MarqueeViewModel @Inject constructor() :
    SkyBaseMviViewModel<MarqueeUiState, MarqueeUiIntent, MarqueeUiEffect>() {

    override fun initialState() = MarqueeUiState()

    override fun handleIntent(intent: MarqueeUiIntent) {
        when (intent) {
            is MarqueeUiIntent.TogglePlay -> setState { copy(isPlaying = !isPlaying) }
            is MarqueeUiIntent.SetDirection -> setState { copy(directionIndex = intent.index) }
        }
    }
}
