package com.sky.widget.refresh

/**
 * 刷新容器的位移样式枚举。
 *
 * 定义了下拉刷新时 Header、Content、Footer 三层结构的空间布局策略。
 * 样式仅影响**下拉刷新侧** Content 是否跟随位移；上拉加载侧行为不受影响。
 *
 * 三种样式的视觉差异：
 * - [Translate]：经典微信式，Header 从顶部推出，Content 整体跟随下移
 * - [FixedContent]：类淘宝效果，Content 固定不动，Header 从顶部滑入覆盖
 * - [FixedFront]：类美团效果，Header 始终悬浮在内容顶层，仅驱动内部动画
 */
enum class SkyRefreshStyle {

    /**
     * 默认样式：Header 从顶部被推出，Content 整体跟随下移（经典微信式）。
     */
    Translate,

    /**
     * 内容固定（类淘宝效果）：下拉时 Content 纹丝不动，
     * Header 从顶部滑入并**覆盖**在内容之上（Header 绘制层级高于 Content）。
     */
    FixedContent,

    /**
     * 固定在前面（类美团效果）：Header 始终固定悬浮在内容顶层原位，
     * 下拉时 Header 与 Content 的位置都不动，仅由 Header 内部动画/进度反馈下拉状态。
     * 建议搭配能根据 `state.indicatorOffset` 驱动内部进度的 Header（如 `SkyCircleRefreshHeader`）。
     */
    FixedFront
}
