package com.sky.widget.refresh.header

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sky.widget.R
import com.sky.widget.refresh.SkyRefreshFlag

/**
 * [SkyClassicsRefreshHeader] 的可定制资源状态。所有字段为 null 时使用库内默认资源。
 *
 * @param iconRes 刷新图标 drawable 资源；null 时使用库内默认图标
 */
@Stable
class SkyClassicsRefreshHeaderState(
    @DrawableRes val iconRes: Int? = null
)

/** 创建并记住 [SkyClassicsRefreshHeaderState]。 */
@Composable
fun rememberSkyClassicsRefreshHeaderState(
    @DrawableRes iconRes: Int? = null
): SkyClassicsRefreshHeaderState = remember(iconRes) { SkyClassicsRefreshHeaderState(iconRes) }

/**
 * 经典默认头部指示器（根据排版方向自适应宽高分配）。
 *
 * 即 `SkyRefreshLayout` 的默认 `header`：纵向宽满高 50dp / 横向高满宽 50dp，
 * 居中旋转图标，仅在 [SkyRefreshFlag.REFRESHING] 时旋转。
 *
 * @param flag 当前头部刷新状态机标记
 * @param orientation 整体容器的滚动方向
 * @param headerState 可定制资源状态，默认全用库内资源
 */
@Composable
fun SkyClassicsRefreshHeader(
    flag: SkyRefreshFlag,
    orientation: Orientation = Orientation.Vertical,
    headerState: SkyClassicsRefreshHeaderState = rememberSkyClassicsRefreshHeaderState()
) {
    Box(
        modifier = Modifier.let {
            if (orientation == Orientation.Vertical) it.fillMaxWidth().height(50.dp)
            else it.fillMaxHeight().width(50.dp)
        }
    ) {
        // 无限动画过渡，驱动 Loading 的持续旋转
        val refreshAnimate by rememberInfiniteTransition(label = "SkyClassicsRefreshHeader").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
            label = "rotate"
        )
        Image(
            painter = painterResource(id = headerState.iconRes ?: R.drawable.sky_icon_refresh_loading),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                // 仅在明确进行刷新动作时触发旋转
                .rotate(if (flag == SkyRefreshFlag.REFRESHING) refreshAnimate else 0f)
        )
    }
}
