package com.sky.widget.sample.refresh

import androidx.lifecycle.viewModelScope
import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 刷新示例子页面共享 ViewModel。
 *
 * 通过 [SkyBaseMviViewModel] 管理 items 与刷新 / 加载中状态；
 * 各屏幕根据演示目标控制 SkyRefreshLayout 的行为，这里只负责数据维度的状态变更。
 */
@HiltViewModel
class RefreshDemoViewModel @Inject constructor() :
    SkyBaseMviViewModel<RefreshDemoUiState, RefreshDemoUiIntent, RefreshDemoUiEffect>() {

    override fun initialState() = RefreshDemoUiState()

    override fun handleIntent(intent: RefreshDemoUiIntent) {
        when (intent) {
            is RefreshDemoUiIntent.Refresh -> {
                if (currentState.isRefreshing) return
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

            is RefreshDemoUiIntent.LoadMore -> {
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

            is RefreshDemoUiIntent.ReachEnd -> {
                // 终态切换由 UI 在 finish(noMoreData=true) 时处理，此处无需额外变更
            }
        }
    }

    /** 是否到达数据末尾 */
    fun isNoMoreData(): Boolean = currentState.items.size >= 45
}
