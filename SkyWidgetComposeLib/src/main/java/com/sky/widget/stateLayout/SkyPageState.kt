package com.sky.widget.stateLayout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iface.SkyIconType

/**
 * 页面状态（密封类型）：加载中 / 成功 / 空数据 / 加载失败。
 *
 * @see SkyPageStateLayout
 */
@Stable
sealed interface SkyPageState {
    /** 加载中 */
    data object Loading : SkyPageState

    /** 加载成功，将渲染业务内容 */
    data object Success : SkyPageState

    /**
     * 空数据。
     *
     * @param message 提示文案，为空时使用占位默认文案
     */
    data class Empty(val message: String = "") : SkyPageState

    /**
     * 加载失败。
     *
     * @param message 错误提示文案，为空时使用占位默认文案
     */
    data class Error(val message: String = "") : SkyPageState
}

/**
 * 状态占位的基础布局（库内部共用）：图标区域 + 提示文案 + 可选操作按钮，整体垂直居中。
 *
 * 定制优先级：[imageContent] 插槽 > [image]（[SkyIconType]）> 不显示图标。
 */
@Composable
internal fun SkyStateWidget(
    message: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    image: SkyIconType? = null,
    imageContent: (@Composable () -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val iconContent = imageContent ?: image?.let { icon ->
            @Composable { SkyStateWidgetIcon(icon) }
        }
        if (iconContent != null) {
            iconContent()
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

/**
 * 状态占位的默认图标渲染：按 [SkyIconType] 类型分派，统一 64dp 尺寸。
 *
 * [SkyIconType.PainterIcon] 保持原色（可为彩色插画）；[SkyIconType.FontIcon] 以 [tint] 着色。
 */
@Composable
internal fun SkyStateWidgetIcon(
    icon: SkyIconType,
    tint: Color = MaterialTheme.colorScheme.outline
) {
    when (icon) {
        is SkyIconType.PainterIcon -> Image(
            painter = icon.painter,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        is SkyIconType.FontIcon -> SkyIconFont(
            iconName = icon.iconName,
            fontName = icon.fontName,
            tint = tint,
            fontSize = with(LocalDensity.current) { 64.dp.toSp() },
            modifier = Modifier.size(64.dp)
        )
    }
}
