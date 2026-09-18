package com.sky.widget.banner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.sky.widget.pageIndicator.SkyPageIndexSource
import com.sky.widget.viewpage.PageChangeAnimFlag
import com.sky.widget.viewpage.SkyViewPageState
import kotlinx.coroutines.flow.Flow

/**
 * [SkyBanner] 的状态控制器。
 *
 * 轮播基于 [SkyViewPageState] 实现：内部把真实页数放大 [sumMultiple] 倍形成一个“伪无限”的分页容器，
 * 初始定位到中间位置（[startMultiple] 倍处），使得用户无论向左还是向右都能持续滑动；
 * 对外暴露的索引均对 [pageCount] 取模，映射回真实数据索引。
 *
 * 通过 [rememberSkyBannerState] 创建并跨重组持有实例。
 */
@Stable
class SkyBannerState : SkyPageIndexSource {

    /**
     * 起始倍数：初始定位到 `pageCount * startMultiple` 处。
     * 取一个较大的中间值，保证用户一开始就能向左（回到更早的页）滑动。
     */
    internal val startMultiple = 50

    /**
     * 总倍数：内部轮播容器的总页数 = `pageCount * sumMultiple`。
     * 数值足够大以近似“无限循环”，滑到尽头时会自动跳回中间位置续接。
     */
    internal val sumMultiple = 5000

    /** 内部承载分页/滑动/动画能力的 [SkyViewPage] 状态。 */
    internal val viewPageState: SkyViewPageState = SkyViewPageState()

    /** 真实页数，由 [SkyBanner] 在组合时同步写入，用于索引取模；实现 [SkyPageIndexSource]。 */
    override var pageCount: Int = 1

    /**
     * 当前真实索引（已取模）的可观察 [State]。
     *
     * 基于内部选中索引派生：翻页导致原始索引变化时会自动重算，并在 [pageCount] 变化
     * （[SkyBanner] 会同步重置内部索引）后一并发出新值。缓存为单例，避免每次订阅都新建对象。
     */
    private val currSelectIndexState: State<Int> = derivedStateOf { getCurrSelectIndex() }

    /** 实现 [SkyPageIndexSource]：当前真实索引的 [State]，随翻页自动更新。 */
    override val currentIndexState: State<Int> get() = currSelectIndexState

    /**
     * 获取当前所在的真实索引（已对 [pageCount] 取模），范围 [0, pageCount)。
     */
    fun getCurrSelectIndex(): Int = viewPageState.getCurrSelectIndex() % pageCount

    /**
     * 获取当前真实索引的 [State] 对象，便于在 Composable 中通过 `by` 订阅，随翻页自动更新。
     *
     * 典型用途：整个 Banner 挂一个点击处理时读取当前页、或驱动指示器高亮当前页。
     */
    fun getCurrSelectIndexState(): State<Int> = currSelectIndexState

    /**
     * 获取当前所在的原始索引（内部放大后的索引，未取模）。
     */
    fun getRawCurrSelectIndex(): Int = viewPageState.getCurrSelectIndex()

    /**
     * 创建当前真实索引的 [Flow]，便于在非 Composable 场景下监听真实页变化。
     */
    fun createCurrSelectIndexFlow(): Flow<Int> = snapshotFlow {
        viewPageState.getCurrSelectIndex() % pageCount
    }

    /**
     * 创建当前原始索引的 [Flow]，便于监听内部索引变化。
     */
    fun createRawCurrSelectIndexFlow(): Flow<Int> = snapshotFlow {
        viewPageState.getCurrSelectIndex()
    }

    /**
     * 翻页动画是否正在执行。
     */
    fun isAnimRunning(): Boolean = viewPageState.isAnimRunning()

    /**
     * 获取偏移量的 [State] 对象。
     */
    fun getOffsetState(): State<Float> = viewPageState.getOffsetState()

    /**
     * 创建子项偏移比例的 [Flow]，值为 -(偏移百分比 + 当前原始索引)，可用于联动指示器等外部动画。
     */
    fun createChildOffsetPercentFlow(): Flow<Float> = viewPageState.createChildOffsetPercentFlow()

    /**
     * 无动画切换到指定真实索引。
     *
     * 内部换算为原始索引 `pageCount * startMultiple + index`，即回到中间基准位置附近再偏移 index 页，
     * 避免累积滑动导致的越界。
     */
    fun setPageIndex(index: Int) {
        viewPageState.setPageIndex(pageCount * startMultiple + index)
    }

    /**
     * 有动画切换到指定真实索引。
     *
     * 相邻页（±1）直接触发内部的上/下一页翻页动画，滑动最平滑；
     * 非相邻页无法用一次相邻翻页表达，退化为无动画跳转 [setPageIndex]。
     */
    fun setPageIndexWithAnimate(index: Int) {
        val currIndex = getCurrSelectIndex()
        when (index) {
            currIndex - 1 -> viewPageState.pageChangeAnimFlag = PageChangeAnimFlag.Prev
            currIndex + 1 -> viewPageState.pageChangeAnimFlag = PageChangeAnimFlag.Next
            else -> setPageIndex(index)
        }
    }
}

/**
 * 创建并记住一个 [SkyBannerState]，跨重组复用同一实例。
 */
@Composable
fun rememberSkyBannerState(): SkyBannerState = remember { SkyBannerState() }
