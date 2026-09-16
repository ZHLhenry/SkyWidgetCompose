package com.sky.widget.samplecp.refresh

import androidx.lifecycle.viewModelScope
import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 刷新示例（基础版）ViewModel。
 *
 * 继承 [SkyBaseMviViewModel]，通过 handleIntent(intent) 处理刷新 / 加载更多意图，
 * 内部用模拟延时与内存数据变更替换真实网络请求。
 */
@HiltViewModel
class RefreshViewModel @Inject constructor() :
    SkyBaseMviViewModel<RefreshUiState, RefreshUiIntent, RefreshUiEffect>() {

    override fun initialState() = RefreshUiState()

    override fun handleIntent(intent: RefreshUiIntent) {
        when (intent) {
            is RefreshUiIntent.Refresh -> {
                viewModelScope.launch {
                    setState { copy(isRefreshing = true) }
                    delay(3000.milliseconds)
                    setState {
                        copy(
                            items = List(15) { "列表项 #${it + 1}" },
                            isRefreshing = false,
                        )
                    }
                }
            }

            is RefreshUiIntent.LoadMore -> {
                if (currentState.isLoadingMore || currentState.items.size >= 45) return
                viewModelScope.launch {
                    setState { copy(isLoadingMore = true) }
                    delay(3000.milliseconds)
                    val appended = List(10) { "列表项 #${currentState.items.size + it + 1}" }
                    setState {
                        copy(
                            items = items + appended,
                            isLoadingMore = false,
                        )
                    }
                }
            }
        }
    }

    /** 是否到达数据末尾（供 SkyRefreshState.finish(noMoreData) 判断） */
    fun isNoMoreData(): Boolean = currentState.items.size >= 45
}
