package com.sky.widget.iconfont

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.jvm.JvmName

/**
 * sky-iconfont 图标 Composable（Compose 版本）。
 *
 * 与原仓库基于 mikepenz/iconics 的 `IconicsDrawable` 不同，本组件使用 Compose [Text]
 * 配合自定义 [FontFamily] 渲染单字符图标，零三方依赖。
 *
 * 必须先在 Application 中调用 [SkyIconFontsLib.initRegister] 注册字体，否则抛异常。
 *
 * @param iconName 图标名称（带前缀，如 `sky-fenxiang`）
 * @param modifier 修饰符，建议用 size 指定图标尺寸
 * @param tint 图标颜色，默认使用 [Color.Unspecified]（即 TextStyle 的颜色）
 * @param fontSize 字号，默认 24.sp；建议与 modifier 的 size 保持一致
 * @param fontName 可选：指定字体，传入 TTF 文件名（不含 `.ttf`，如 `sky_iconfont`）；
 *   不传使用默认字体，支持多字体并存。内部解析优先级为「名称精确匹配 → css 前缀匹配 → 默认字体」
 * @param state 可选：可观察的图标状态，便于在 VM/UI 中动态切换图标
 *
 * @see SkyIconFontState
 * @see SkyIconFontsLib
 */
@Composable
fun SkyIconFont(
    iconName: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp,
    fontName: String? = null,
    state: SkyIconFontState? = null
) {
    val typeface = SkyIconFontsLib.resolveTypeface(fontName)
    val name = state?.iconName ?: iconName
    val resolvedTint = (state?.tint ?: tint).takeOrElse { Color.Unspecified }
    val resolvedSize = state?.fontSize ?: fontSize

    val iconChar = typeface.getIconChar(name)
    if (iconChar == null) {
        // 名称不存在时降级渲染占位，避免崩溃且便于排查
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "?", // visible fallback
                style = TextStyle(
                    color = Color.Red,
                    fontFamily = FontFamily.Default,
                    fontSize = resolvedSize,
                    textAlign = TextAlign.Center
                )
            )
        }
        return
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = iconChar.toString(),
            style = TextStyle(
                color = resolvedTint,
                fontFamily = typeface.fontFamily,
                fontSize = resolvedSize,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * sky-iconfont 图标状态（可观察，配合 [SkyIconFont] 使用）。
 *
 * 字段基于 Compose 快照状态，调用 [setIcon] / [setTint] / [setFontSize] 后会驱动重组。
 * 当图标需随业务状态动态变化（如切换图标、调整颜色）时，使用本状态类在 VM/UI 中持有，
 * 通过 [rememberSkyIconFontState] 创建。
 */
@Stable
class SkyIconFontState(
    iconName: String,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp
) {
    var iconName: String by mutableStateOf(iconName)
        private set
    var tint: Color by mutableStateOf(tint)
        private set
    var fontSize: TextUnit by mutableStateOf(fontSize)
        private set

    /** 切换图标名称 */
    @JvmName("updateIconName")
    fun setIcon(iconName: String) {
        this.iconName = iconName
    }

    /** 切换图标颜色 */
    @JvmName("updateTintColor")
    fun setTint(tint: Color) {
        this.tint = tint
    }

    /** 切换图标字号 */
    @JvmName("updateFontSizeValue")
    fun setFontSize(fontSize: TextUnit) {
        this.fontSize = fontSize
    }
}

/**
 * 创建 [SkyIconFontState] 的工厂方法（与组件同文件，便于定制）。
 *
 * 参数为初始值，后续通过状态对象的方法动态修改。
 */
@Composable
fun rememberSkyIconFontState(
    iconName: String,
    tint: Color = Color.Unspecified,
    fontSize: TextUnit = 24.sp
): SkyIconFontState = remember(iconName, tint, fontSize) {
    SkyIconFontState(iconName, tint, fontSize)
}
