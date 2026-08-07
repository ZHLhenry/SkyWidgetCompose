package com.sky.widget.refresh.header

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sky.widget.R
import com.sky.widget.refresh.SkyRefreshFlag

/**
 * 运行时探测宿主是否引入了 lottie-compose。
 * 注意：探测逻辑不引用任何 Lottie 类型，避免在缺失时先触发 NoClassDefFoundError。
 */
private val isLottieComposeAvailable: Boolean by lazy {
    runCatching {
        Class.forName("com.airbnb.lottie.compose.LottieCompositionSpec")
    }.isSuccess
}

/**
 * [SkyLottieRefreshHeader] 的可定制资源状态。所有字段为 null/默认值时使用库内默认资源。
 *
 * @param rawRes Lottie json 的 raw 资源（可用消费者自己的 `R.raw.xxx`）；null 时使用库内置动画
 * @param speed 动画播放速度
 */
@Stable
class SkyLottieRefreshHeaderState(
    @RawRes val rawRes: Int? = null,
    val speed: Float = 1f
)

/** 创建并记住 [SkyLottieRefreshHeaderState]。 */
@Composable
fun rememberSkyLottieRefreshHeaderState(
    @RawRes rawRes: Int? = null,
    speed: Float = 1f
): SkyLottieRefreshHeaderState = remember(rawRes, speed) {
    SkyLottieRefreshHeaderState(rawRes, speed)
}

/**
 * Lottie 动画刷新 Header（内置动画资源，LottieHeader 风格效果）。
 *
 * [SkyRefreshFlag.REFRESHING] 时循环播放动画，其余状态暂停在当前帧。
 * 通过 [SkyRefreshLayout] 的 `header` 插槽使用：
 *
 * ```kotlin
 * SkyRefreshLayout(
 *     state = state,
 *     header = { SkyLottieRefreshHeader(flag = state.refreshFlag) },
 *     ...
 * )
 * ```
 *
 * 注意：Lottie 在库中为 compileOnly 依赖，不会传递给消费者。
 * 使用前需在宿主模块自行导包：`implementation("com.airbnb.android:lottie-compose:6.7.1")`，
 * 否则本组件会在运行时抛出带导包提示的 [IllegalStateException]。
 *
 * @param flag 头部刷新状态，传 `state.refreshFlag`
 * @param orientation 与外层 `SkyRefreshLayout` 的 orientation 保持一致，
 * 决定主轴尺寸（纵向高 150dp / 横向宽 150dp，即触发刷新的阈值）
 * @param headerState 可定制资源状态，默认全用库内默认值
 */
@Composable
fun SkyLottieRefreshHeader(
    flag: SkyRefreshFlag,
    orientation: Orientation = Orientation.Vertical,
    headerState: SkyLottieRefreshHeaderState = rememberSkyLottieRefreshHeaderState()
) {
    check(isLottieComposeAvailable) {
        "SkyLottieRefreshHeader 依赖 Lottie。" +
            "请在自行添加依赖：implementation(\"com.airbnb.android:lottie-compose:版本号\")"
    }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(headerState.rawRes ?: R.raw.sky_rl_lottie_refresh)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = flag == SkyRefreshFlag.REFRESHING,
        speed = headerState.speed,
        restartOnPlay = false
    )
    Box(
        modifier = Modifier.let {
            if (orientation == Orientation.Vertical) it.fillMaxWidth().height(150.dp)
            else it.fillMaxHeight().width(150.dp)
        },
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(150.dp)
        )
    }
}
