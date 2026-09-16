package com.sky.widget.samplecp.signature

import com.sky.mvi.core.SkyBaseMviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 签名板示例 ViewModel。
 *
 * 持有是否留有笔迹与最近一次保存路径；清空 / 保存完成由 UI 派发 intent 同步状态。
 */
@HiltViewModel
class SignatureViewModel @Inject constructor() :
    SkyBaseMviViewModel<SignatureUiState, SignatureUiIntent, SignatureUiEffect>() {

    override fun initialState() = SignatureUiState()

    override fun handleIntent(intent: SignatureUiIntent) {
        when (intent) {
            is SignatureUiIntent.SignatureChanged ->
                setState { copy(hasSignature = intent.has) }

            is SignatureUiIntent.Clear ->
                setState { copy(hasSignature = false, lastSavedPath = null) }

            is SignatureUiIntent.Saved ->
                setState { copy(lastSavedPath = intent.path) }
        }
    }
}
