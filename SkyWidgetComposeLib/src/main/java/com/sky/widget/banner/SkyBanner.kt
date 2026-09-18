package com.sky.widget.banner

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sky.widget.viewpage.LocalIndexToKey
import com.sky.widget.viewpage.NoSkyPagerContentTransformation
import com.sky.widget.viewpage.SkyDragInteractionSource
import com.sky.widget.viewpage.SkyPagerContentTransformation
import com.sky.widget.viewpage.SkyViewPage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 可自动循环轮播的Banner。
 *
 * 基于 [SkyViewPage] 实现无限轮播：内部把真实页数放大为 `pageCount * sumMultiple` 的“伪无限”分页，
 * 并借助 [LocalIndexToKey] 注入 `{ it % pageCount }` 映射，把内部原始索引还原为真实数据索引，
 * 因此 content 始终只感知 [0, pageCount) 的真实索引。滑到尽头时自动跳回中间基准位置续接，用户无感知。
 *
 * @param pageCount 真实页数；小于等于 0 时不渲染任何内容，等于 1 时不自动轮播。
 * @param modifier 作用于轮播容器根节点的修饰符。
 * @param bannerState 轮播状态控制器，见 [rememberSkyBannerState]，用于读取索引、代码翻页、监听偏移等。
 * @param orientation 滑动方向，默认水平 [Orientation.Horizontal]。
 * @param userEnable 用户是否可手势滑动；为 false 时手势无效，但仍可通过 [bannerState] 代码翻页。
 * @param autoScroll 是否自动轮播，默认 true；仅当 [pageCount] 大于 1 时才会真正滚动。
 * @param autoScrollTime 自动轮播的间隔时间（毫秒），默认 3000。
 * @param bannerKey 为每一页提供稳定 key，用于减少重组、提升性能，效果等同 LazyColumn items 的 key。
 * @param clip 是否对内容区域做滚动裁剪，默认 true。
 * @param contentTransformation 页面内容变换策略，默认不变换，可用于实现缩放等视差效果。
 * @param pageAnimationSpec 翻页/回弹动画的时间曲线，默认 [spring]；
 *   消费者可传 tween / spring 等任意 [AnimationSpec] 自定义滑动动画效果（自动轮播翻页同样生效）。
 * @param content 每一页的 Compose 内容，接收 [SkyBannerScope] 作为作用域，可通过其 index 拿到真实页索引。
 */
@Composable
fun SkyBanner(
    pageCount: Int,
    modifier: Modifier = Modifier,
    bannerState: SkyBannerState = rememberSkyBannerState(),
    orientation: Orientation = Orientation.Horizontal,
    userEnable: Boolean = true,
    autoScroll: Boolean = true,
    autoScrollTime: Long = 3000,
    bannerKey: (index: Int) -> Any = { it },
    clip: Boolean = true,
    contentTransformation: SkyPagerContentTransformation = NoSkyPagerContentTransformation,
    pageAnimationSpec: AnimationSpec<Float> = spring(),
    content: @Composable SkyBannerScope.() -> Unit,
) {
    // 无有效页面直接返回，不渲染任何内容。
    if (pageCount <= 0) return

    // 是否处于“自动轮播倒计时”中：开启自动轮播且多于一页时才需要滚动。
    var scrolling by remember(autoScroll, pageCount) {
        mutableStateOf(autoScroll && pageCount > 1)
    }

    // 拖拽监听源：用户开始拖拽时暂停自动轮播，松手（结束/取消）后恢复轮播。
    // 未开启自动轮播或只有一页时无需监听，返回 null 让 SkyViewPage 使用默认交互源。
    val scrollableInteractionSource = remember(autoScroll, pageCount) {
        if (!autoScroll) return@remember null
        if (pageCount <= 1) {
            // 只有一页无需轮播，关闭滚动标志。
            scrolling = false
            return@remember null
        }
        SkyDragInteractionSource { interaction ->
            // DragInteraction.Start 表示手指按下开始拖拽，此时暂停；其余（Stop/Cancel）恢复。
            scrolling = interaction !is DragInteraction.Start
        }
    }

    // 计算内部轮播容器的总页数，并把初始位置定位到中间基准（pageCount * startMultiple）处。
    // 用 remember(pageCount) 保证仅在真实页数变化时重新定位，避免打断当前浏览位置。
    val maxPageCount = remember(pageCount) {
        bannerState.pageCount = pageCount
        bannerState.viewPageState.setPageIndex(pageCount * bannerState.startMultiple)
        // 与 Int.MAX_VALUE 取小，规避 pageCount 过大时的整型溢出风险。
        minOf(pageCount * bannerState.sumMultiple, Int.MAX_VALUE)
    }

    // 自动轮播：每隔 autoScrollTime 翻到下一页；到达内部尽头时无动画跳回中间基准位置续接。
    if (scrolling) {
        LaunchedEffect(autoScrollTime) {
            while (true) {
                delay(autoScrollTime.milliseconds)
                // 使用内部原始索引判断是否临近尽头。
                val index = bannerState.viewPageState.getCurrSelectIndex()
                if (index + 1 >= maxPageCount) {
                    // 已滑到最后一页，跳回中间基准位置（无动画），用户无感知地继续循环。
                    bannerState.viewPageState.setPageIndex(pageCount * bannerState.startMultiple)
                } else {
                    // 正常向后翻一页（有动画）。
                    bannerState.viewPageState.setPageIndexWithAnimate(index + 1)
                }
            }
        }
    }

    // 注入索引映射：把内部放大的原始索引对 pageCount 取模，还原为真实数据索引供 content 使用。
    CompositionLocalProvider(LocalIndexToKey provides { it % pageCount }) {
        SkyViewPage(
            pageCount = maxPageCount,
            modifier = modifier,
            state = bannerState.viewPageState,
            orientation = orientation,
            userEnable = userEnable,
            // 轮播至少缓存两侧各 2 页，页数较多时按 (pageCount - 1) / 2 适当增大，保证滑动连贯。
            pageCache = maxOf(2, (pageCount - 1) / 2),
            scrollableInteractionSource = scrollableInteractionSource,
            pagerKey = bannerKey,
            clip = clip,
            contentTransformation = contentTransformation,
            pageAnimationSpec = pageAnimationSpec,
        ) {
            // SkyViewPageScope.index 为映射后的真实索引，realIndex 为内部原始索引。
            content(SkyBannerScope(index, realIndex))
        }
    }
}
