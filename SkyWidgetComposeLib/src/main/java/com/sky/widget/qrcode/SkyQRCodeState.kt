package com.sky.widget.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * 二维码扫描器状态。
 *
 * 持有扫描模式、取景框尺寸、闪光灯开关、扫描进行中状态等可配置项，
 * 可在 Composable 外部通过 [rememberSkyQRCodeState] 创建并传入 [SkyQRCodeScanner]。
 */
@Stable
class SkyQRCodeState internal constructor(
    initialMode: SkyQRCodeMode,
    initialFrameSize: DpSize,
    initialFrameMarginTop: Dp,
    initialFlashEnabled: Boolean,
    initialBeepEnabled: Boolean,
    initialVibrateEnabled: Boolean,
    initialBeepResId: Int
) {
    /**
     * 当前扫描模式，决定识别哪些条码格式。
     */
    var mode: SkyQRCodeMode by mutableStateOf(initialMode)
        internal set

    /**
     * 取景框尺寸。宽度/高度任意一项为 [Dp.Unspecified] 时，该维度使用容器宽度的一半。
     */
    var frameSize: DpSize by mutableStateOf(initialFrameSize)
        internal set

    /**
     * 取景框距离顶部的边距。为 [Dp.Unspecified] 时垂直居中。
     */
    var frameMarginTop: Dp by mutableStateOf(initialFrameMarginTop)
        internal set

    /**
     * 是否开启闪光灯（手电筒）。
     */
    var flashEnabled: Boolean by mutableStateOf(initialFlashEnabled)
        internal set

    /**
     * 识别成功后是否播放提示音。
     */
    var beepEnabled: Boolean by mutableStateOf(initialBeepEnabled)
        internal set

    /**
     * 识别成功提示音资源 ID（raw 资源）。0 表示使用库内置默认提示音；
     * 消费者可通过 [setBeepResId] 传入自定义提示音。
     */
    var beepResId: Int by mutableStateOf(initialBeepResId)
        internal set

    /**
     * 识别成功后是否震动。
     */
    var vibrateEnabled: Boolean by mutableStateOf(initialVibrateEnabled)
        internal set

    /**
     * 是否正在扫描中。识别成功后可设为 false 暂停扫描，或重新设为 true 继续扫描。
     */
    var isScanning: Boolean by mutableStateOf(true)
        internal set

    /**
     * 当前取景框在容器坐标系中的位置（0..1），由扫描器内部在布局完成后写入，
     * 供 [SkyQRCodeViewfinder] 与解码器使用。
     */
    internal var frameBounds: FrameBounds by mutableStateOf(FrameBounds())

    /**
     * 内部使用的取景框进度，0..1，用于驱动扫描线动画。
     */
    internal var scanProgress: Float by mutableFloatStateOf(0f)

    /**
     * 切换到指定扫描模式。
     */
    fun setMode(mode: SkyQRCodeMode) {
        this.mode = mode
    }

    /**
     * 设置取景框尺寸。
     *
     * @param size 取景框尺寸，[Dp.Unspecified] 表示使用默认值
     */
    fun setFrameSize(size: DpSize) {
        this.frameSize = size
    }

    /**
     * 设置取景框距离顶部的边距。
     *
     * @param marginTop 距离顶部的边距，[Dp.Unspecified] 表示垂直居中
     */
    fun setFrameMarginTop(marginTop: Dp) {
        this.frameMarginTop = marginTop
    }

    /**
     * 打开或关闭闪光灯。
     *
     * @param enabled true 为打开，false 为关闭
     */
    fun setFlashEnabled(enabled: Boolean) {
        this.flashEnabled = enabled
    }

    /**
     * 打开或关闭识别成功提示音。
     */
    fun setBeepEnabled(enabled: Boolean) {
        this.beepEnabled = enabled
    }

    /**
     * 设置识别成功提示音资源。
     *
     * @param resId raw 资源 ID；0 表示恢复库内置默认提示音
     */
    fun setBeepResId(resId: Int) {
        this.beepResId = resId
    }

    /**
     * 打开或关闭识别成功震动。
     */
    fun setVibrateEnabled(enabled: Boolean) {
        this.vibrateEnabled = enabled
    }

    /**
     * 继续扫描。
     */
    fun resume() {
        isScanning = true
    }

    /**
     * 暂停扫描。
     */
    fun pause() {
        isScanning = false
    }

    /**
     * 取景框在容器中的相对边界。
     *
     * left/top/right/bottom 为取景框在容器坐标系中的像素矩形；
     * containerWidth/containerHeight 为容器像素尺寸，供分析器按
     * PreviewView FILL_CENTER 裁剪规则反映射到相机帧坐标系。
     */
    @Stable
    internal data class FrameBounds(
        val left: Float = 0f,
        val top: Float = 0f,
        val right: Float = 0f,
        val bottom: Float = 0f,
        val containerWidth: Int = 0,
        val containerHeight: Int = 0
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }
}

/**
 * 创建并记住一个 [SkyQRCodeState]。
 *
 * @param mode 初始扫描模式
 * @param frameSize 取景框尺寸，默认宽度/高度均为 [Dp.Unspecified]，表示使用容器宽度的一半
 * @param frameMarginTop 取景框距离顶部的边距，默认 [Dp.Unspecified] 表示垂直居中
 * @param flashEnabled 是否默认开启闪光灯
 * @param beepEnabled 识别成功后是否播放提示音
 * @param vibrateEnabled 识别成功后是否震动
 * @param beepResId 自定义提示音 raw 资源 ID；0 表示使用库内置默认提示音
 */
@Composable
fun rememberSkyQRCodeState(
    mode: SkyQRCodeMode = SkyQRCodeMode.All,
    frameSize: DpSize = DpSize(Dp.Unspecified, Dp.Unspecified),
    frameMarginTop: Dp = Dp.Unspecified,
    flashEnabled: Boolean = false,
    beepEnabled: Boolean = true,
    vibrateEnabled: Boolean = true,
    beepResId: Int = 0
): SkyQRCodeState = remember {
    // 注意：不能使用带 key 的 remember。frameSize/frameMarginTop 等默认参数在每次重组时
    // 都会重新求值生成新实例（DpSize 无值相等语义），作为 key 会导致每次重组都重新创建
    // SkyQRCodeState，旧实例的写入无法反映到 UI（表现为 Switch 点击无响应）。
    SkyQRCodeState(
        initialMode = mode,
        initialFrameSize = frameSize,
        initialFrameMarginTop = frameMarginTop,
        initialFlashEnabled = flashEnabled,
        initialBeepEnabled = beepEnabled,
        initialVibrateEnabled = vibrateEnabled,
        initialBeepResId = beepResId
    )
}
