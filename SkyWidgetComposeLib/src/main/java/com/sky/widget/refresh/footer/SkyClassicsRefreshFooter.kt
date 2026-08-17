package com.sky.widget.refresh.footer

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.R
import com.sky.widget.refresh.SkyRefreshFlag

/**
 * [SkyClassicsRefreshFooter] 的可定制资源状态。所有字段为 null 时使用库内默认资源。
 *
 * @param loadingText “正在加载”文案；null 时使用库内默认 `sky_rl_loading` 资源
 * @param loadingIconRes 加载图标 drawable 资源；null 时使用库内默认图标
 */
@Stable
class SkyClassicsRefreshFooterState(
    val loadingText: String? = null,
    @DrawableRes val loadingIconRes: Int? = null
)

/** 创建并记住 [SkyClassicsRefreshFooterState]。 */
@Composable
fun rememberSkyClassicsRefreshFooterState(
    loadingText: String? = null,
    @DrawableRes loadingIconRes: Int? = null
): SkyClassicsRefreshFooterState = remember(loadingText, loadingIconRes) {
    SkyClassicsRefreshFooterState(loadingText, loadingIconRes)
}

/**
 * 经典默认尾部指示器（承载“没有更多了”的原生跟手交互底座）。
 *
 * 即 `SkyRefreshLayout` 的默认 `footer`：纵向宽满高 50dp / 横向高满宽 50dp，
 * 加载中为旋转图标 + 文案（仅 [SkyRefreshFlag.REFRESHING] 时旋转），
 * 数据穷尽时为静态文案（仍可被 1:1 跟手拉出停靠查看）。
 *
 * @param flag 当前尾部加载状态机标记
 * @param noMoreData 是否已经没有更多数据
 * @param orientation 整体容器的滚动方向
 * @param noMoreText 定制版“没有更多数据”文案；为 null 时终态整体不渲染
 * @param footerState 可定制资源状态，默认全用库内资源
 */
@Composable
fun SkyClassicsRefreshFooter(
    flag: SkyRefreshFlag,
    noMoreData: Boolean,
    orientation: Orientation = Orientation.Vertical,
    noMoreText: String? = null,
    footerState: SkyClassicsRefreshFooterState = rememberSkyClassicsRefreshFooterState()
) {
    // 未传入“无更多数据”文案时，终态整体不渲染（零尺寸，footerBound 归零），
    // 末尾拉出与停靠交互随之自然失效
    if (noMoreData && noMoreText == null) {
        Box(modifier = Modifier)
        return
    }
    val displayNoMoreText = noMoreText.orEmpty()
    val loadingText = footerState.loadingText ?: stringResource(R.string.sky_rl_loading)
    val loadingIconRes = footerState.loadingIconRes ?: R.drawable.sky_icon_load_more_loading

    if (orientation == Orientation.Vertical) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 经典悬停特性：即便 noMoreData 为 true 也强制保持有效尺寸。
                // 这确保了它可以随着用户在底部的惯性滑动被完美拉出，并吸附在视野中。
                .height(50.dp)
        ) {
            if (noMoreData) {
                // 无更多数据：静态提示模式
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayNoMoreText,
                        fontSize = 12.sp,
                        color = colorResource(R.color.sky_rl_text_tertiary),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 加载中：动效提示模式
                val refreshAnimate by rememberInfiniteTransition(label = "SkyClassicsRefreshFooter").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
                    label = "rotate"
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = loadingIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (flag == SkyRefreshFlag.REFRESHING) refreshAnimate else 0f)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = loadingText,
                            fontSize = 14.sp,
                            color = colorResource(R.color.sky_rl_text_title),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        // ── 横向布局模式 ──
        // 高度撑满父容器，主轴宽度固定 50dp（与纵向模式的高度 50dp 对应）。
        // 文字采用竖排显示：将字符串按字符拆分后用换行符连接，适应横向空间的狭长布局。
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(50.dp)
        ) {
            if (noMoreData) {
                // 无更多数据：竖排静态提示
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 将横排文字转为竖排：逐字符换行
                    val verticalText = displayNoMoreText.map { it.toString() }.joinToString("\n")
                    Text(
                        text = verticalText,
                        fontSize = 12.sp,
                        color = colorResource(R.color.sky_rl_text_tertiary),
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 加载中：旋转图标 + 竖排文案
                val refreshAnimate by rememberInfiniteTransition(label = "SkyClassicsRefreshFooter").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
                    label = "rotate"
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = loadingIconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (flag == SkyRefreshFlag.REFRESHING) refreshAnimate else 0f)
                    )
                    // 将横排文字转为竖排：逐字符换行
                    val verticalLoadingText = loadingText.map { it.toString() }.joinToString("\n")
                    Text(
                        text = verticalLoadingText,
                        fontSize = 14.sp,
                        color = colorResource(R.color.sky_rl_text_title),
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
