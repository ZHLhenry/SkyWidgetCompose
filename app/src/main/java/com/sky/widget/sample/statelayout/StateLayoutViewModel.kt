package com.sky.widget.sample.statelayout

import androidx.lifecycle.viewModelScope
import com.sky.mvi.core.SkyBaseMviViewModel
import com.sky.widget.stateLayout.SkyPageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 页面状态布局示例 ViewModel。
 *
 * 持有当前 [SkyPageState]，根据 intent 在 Loading / Success / Empty / Error 间切换。
 */
@HiltViewModel
class StateLayoutViewModel @Inject constructor() :
    SkyBaseMviViewModel<StateLayoutUiState, StateLayoutUiIntent, StateLayoutUiEffect>() {

    override fun initialState() = StateLayoutUiState()

    override fun handleIntent(intent: StateLayoutUiIntent) {
        when (intent) {
            is StateLayoutUiIntent.ToLoading -> setState { copy(pageState = SkyPageState.Loading) }
            is StateLayoutUiIntent.ToSuccess -> setState { copy(pageState = SkyPageState.Success) }
            is StateLayoutUiIntent.ToEmpty ->
                setState { copy(pageState = SkyPageState.Empty("暂无数据，去逛逛吧")) }

            is StateLayoutUiIntent.ToError ->
                setState { copy(pageState = SkyPageState.Error("网络异常，请稍后重试")) }

            is StateLayoutUiIntent.Retry -> {
                viewModelScope.launch {
                    setState { copy(pageState = SkyPageState.Loading) }
                    delay(1500.milliseconds)
                    // 模拟三种结果：成功 / 空数据 / 失败
                    setState {
                        copy(
                            pageState = when ((0..2).random()) {
                                0 -> SkyPageState.Success
                                1 -> SkyPageState.Empty("没有搜索到相关结果")
                                else -> SkyPageState.Error("网络异常，请检查网络后重试")
                            }
                        )
                    }
                }
            }
        }
    }
}
