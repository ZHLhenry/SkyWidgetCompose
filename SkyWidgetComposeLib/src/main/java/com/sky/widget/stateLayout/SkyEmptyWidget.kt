package com.sky.widget.stateLayout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 空数据占位，支持自定义图标与「重试」按钮。
 *
 * @param modifier 外层修饰符
 * @param image 顶部图标区域，默认不显示；可传入 [Icon] / [Image] 等
 * @param message 提示文案
 * @param buttonText 重试按钮文字
 * @param onRetry 重试回调，为 null 时不显示按钮
 */
@Composable
fun SkyEmptyWidget(
    modifier: Modifier = Modifier,
    image: @Composable (() -> Unit)? = null,
    message: String = "暂无数据",
    buttonText: String = "点击重试",
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (image != null) {
            image()
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = message,
            color = MaterialTheme.colorScheme.outline
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) {
                Text(buttonText)
            }
        }
    }
}
