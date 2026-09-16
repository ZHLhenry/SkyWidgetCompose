package com.sky.widget.samplecp.signature

import com.sky.mvi.core.SkyUiEffect
import com.sky.mvi.core.SkyUiIntent
import com.sky.mvi.core.SkyUiState

/**
 * 签名板示例的 MVI 契约。
 *
 * 演示 [com.sky.widget.signatureView.SkySignatureView] 的清空与保存，
 * 笔迹本身由 SkySignatureViewState 持有，这里只管理"是否有笔迹"与"保存成功"副作用。
 */
data class SignatureUiState(
    val hasSignature: Boolean = false,
    val lastSavedPath: String? = null,
) : SkyUiState

sealed interface SignatureUiIntent : SkyUiIntent {
    /** 笔迹变化（由 UI 根据 SkySignatureViewState.isEmpty 派发） */
    data class SignatureChanged(val has: Boolean) : SignatureUiIntent

    /** 清空签名 */
    data object Clear : SignatureUiIntent

    /** 保存完成（由 UI 在拿到 Bitmap 后派发） */
    data class Saved(val path: String?) : SignatureUiIntent
}

/** 保存成功后的轻量提示等跨层副作用可在此扩展 */
data object SignatureUiEffect : SkyUiEffect
