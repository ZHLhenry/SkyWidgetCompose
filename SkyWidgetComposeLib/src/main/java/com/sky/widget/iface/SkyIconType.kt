package com.sky.widget.iface

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * [SkyIconType] 图标定义（密封类型，保证类型安全）。
 *
 * 两种实现：
 * - [PainterIcon]：[Painter] 图片图标（本地资源 / 矢量 / 网络图片等）；
 * - [FontIcon]：iconfont 图标，经 [com.sky.widget.iconfont.SkyIconFont] 渲染，
 *   需先在 Application 中调用 `SkyIconFontsLib.initRegister` 注册字体。
 *
 * 可直接构造实现类，也可使用 [painter] / [iconFont] 工厂方法。
 */
sealed interface  SkyIconType {
    /**
     * [Painter] 图片图标。
     *
     * @property painter 任意 [Painter]（`painterResource` / `rememberVectorPainter` / Coil 等）。
     */
    @Immutable
    data class PainterIcon(val painter: Painter) : SkyIconType

    /**
     * iconfont 图标。
     *
     * @property iconName 图标名（带前缀，如 `testSkyshouye`）。
     * @property fontName 指定字体（TTF 文件名，不含 .ttf，如 `testSky_iconfont`）；
     *   为 null 时使用默认字体，多字体并存场景按需传入。
     */
    @Immutable
    data class FontIcon(val iconName: String, val fontName: String? = null) : SkyIconType

    companion object {
        /** 以 [Painter] 创建图片图标。 */
        fun painter(painter: Painter): SkyIconType = PainterIcon(painter)

        /** 以 iconfont 图标名创建图标；[fontName] 指定 TTF 字体（可空 = 默认字体）。 */
        fun iconFont(iconName: String, fontName: String? = null): SkyIconType = FontIcon(iconName, fontName)
    }
}