package com.sky.widget.banner

import androidx.compose.runtime.Stable

/**
 * [SkyBanner] 每一页的 Compose 作用域。
 *
 * 轮播内部会把真实页数放大若干倍来实现无限循环，因此存在两套索引：
 * [index] 是取模后映射回真实数据的索引，业务侧用它区分/取用页面数据；
 * [rawIndex] 是内部 [com.sky.widget.viewpage.SkyViewPage] 使用的原始（放大后）索引，
 * 供内容变换等需要计算相对偏移的内部逻辑使用。
 *
 * @property index 当前 content 对应的真实数据索引（已对 pageCount 取模），范围 [0, pageCount)。
 * @property rawIndex 当前 content 在内部轮播容器中的原始索引，可能远大于 pageCount。
 */
@Stable
class SkyBannerScope(
    val index: Int,
    val rawIndex: Int,
)
