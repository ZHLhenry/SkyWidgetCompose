package com.sky.widget.stateLayout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sky.widget.iface.SkyIconType

/**
 * 加载失败占位，支持自定义图标与「重新加载」按钮。
 *
 * @param modifier 外层修饰符
 * @param image 顶部图标（[SkyIconType]：Painter 图片或 iconfont 名称），默认 64dp 渲染；为 null 时不显示
 * @param imageContent 完全自定义顶部图标区域（如 Icon / Image 等）；优先级高于 [image]
 * @param message 错误提示文案
 * @param buttonText 重新加载按钮文字
 * @param onRetry 重试回调，为 null 时不显示按钮
 */
@Composable
fun SkyErrorWidget(
    modifier: Modifier = Modifier,
    image: SkyIconType? = null,
    imageContent: (@Composable () -> Unit)? = null,
    message: String = "加载失败，请稍后重试",
    buttonText: String = "重新加载",
    onRetry: (() -> Unit)? = null
) {
    SkyStateWidget(
        message = message,
        buttonText = buttonText,
        modifier = modifier,
        image = image,
        imageContent = imageContent,
        onRetry = onRetry
    )
}
