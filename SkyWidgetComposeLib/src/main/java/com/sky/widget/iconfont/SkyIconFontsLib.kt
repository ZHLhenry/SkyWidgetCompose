package com.sky.widget.iconfont

import android.app.Application
import android.content.Context

/**
 * 已注册字体的摘要信息
 *
 * @property fontName 字体名称（JSON 中的 name 字段）
 * @property mappingPrefix 图标前缀（css_prefix_text 前 3 个字符）
 * @property iconCount 该字体包含的图标数量
 */
data class FontInfo(
    val fontName: String,
    val mappingPrefix: String,
    val iconCount: Int
)

/**
 * sky-iconfont 全局注册与查询入口（Compose 版本，支持多字体）。
 *
 * 与原仓库基于 mikepenz/iconics 的实现不同：本版本不依赖任何三方图标库，
 * 仅使用 Android 原生 [android.graphics.Typeface] 与 Compose [androidx.compose.ui.text.font.FontFamily]
 * 完成图标渲染。调用方通过 [initRegister] 初始化（建议在 Application.onCreate 中调用）。
 *
 * 多字体设计思想（对齐原仓库）：
 * 1. [initRegister] 接收 TTF 路径**列表**，可一次注册多个字体，第一个注册为默认字体；
 * 2. 内部通过「字体名称精确匹配 → 前缀匹配 → null 回退默认字体」三级解析 [resolveTypeface]；
 * 3. 所有公开查询方法均带可选 `fontName` 参数，不传时路由到默认字体，支持多字体并存与按需混用。
 *
 * 使用示例：
 * ```kotlin
 * // Application.onCreate 中注册多个字体
 * SkyIconFontsLib.initRegister(
 *     this,
 *     ttfPaths = listOf("fonts/sky_iconfont.ttf", "fonts/adb_iconfont.ttf")
 * )
 *
 * // Composable 中使用默认字体
 * SkyIconFont(iconName = "sky-fenxiang", tint = Color.Red, fontSize = 24.sp)
 *
 * // 指定字体（fontName 为 TTF 文件名，不含扩展名）
 * SkyIconFont(iconName = "adb-device", fontName = "adb_iconfont", tint = Color.Red)
 * ```
 */
object SkyIconFontsLib {

    /** 全局 Application Context（用于加载 assets / resources） */
    internal lateinit var applicationContext: Context
        private set

    /** 所有已注册字体（注册顺序保持输入顺序，第一个为默认字体） */
    private val registered: MutableList<SkyIconFontTypeface> = mutableListOf()

    /**
     * 初始化并注册自定义字体，应在 Application.onCreate 中调用。
     * 传入 assets 下 TTF 文件路径列表，内部自动创建 [SkyIconFontTypeface] 并完成注册。
     * 第一个注册的字体被设为默认字体；未传入路径时自动使用库内置默认路径。
     *
     * @param application Application 实例
     * @param ttfPaths TTF 字体文件在 assets 中的路径列表，默认仅 `fonts/sky_iconfont.ttf`
     */
    @JvmOverloads
    fun initRegister(
        application: Application,
        ttfPaths: List<String> = listOf("fonts/sky_iconfont.ttf")
    ) {
        require(ttfPaths.isNotEmpty()) { "ttfPaths 不能为空" }
        applicationContext = application.applicationContext
        registered.clear()
        ttfPaths.forEach { path -> registered.add(SkyIconFontTypeface(ttfAssetPath = path)) }
        // 列表第一个注册为默认字体；重复初始化时同样以本次第一个为准
        SkyIconFontTypeface.default = registered.first()
    }

    /** 是否已初始化（至少注册了一个字体） */
    val isInitialized: Boolean
        get() = SkyIconFontTypeface.default != null

    /** 获取默认字体实例（未初始化抛异常） */
    internal fun getRegisteredTypeface(): SkyIconFontTypeface =
        SkyIconFontTypeface.default ?: throw IllegalArgumentException(
            "SkyIconFontsLib 尚未初始化，请在 Application.onCreate 中调用 SkyIconFontsLib.initRegister(this)"
        )

    /**
     * 按字体名称解析字体实例。
     *
     * 查找优先级：
     * 1. [fontName] 作为字体文件名（不含 `.ttf`）精确匹配；
     * 2. [fontName] 作为 JSON name 字段精确匹配；
     * 3. [fontName] 作为 css_prefix_text 前 3 个字符匹配；
     * 4. [fontName] 为 null 时，使用默认字体（第一个注册的字体）。
     *
     * @throws IllegalArgumentException 未找到匹配字体时抛出
     */
    internal fun resolveTypeface(fontName: String?): SkyIconFontTypeface {
        if (fontName != null) {
            registered.firstOrNull { it.fileBaseName == fontName }?.let { return it }
            registered.firstOrNull { it.fontName == fontName }?.let { return it }
            registered.firstOrNull { it.mappingPrefix == fontName }?.let { return it }
            throw IllegalArgumentException(
                "未找到字体 '$fontName'，已注册的字体: " +
                    registered.joinToString { "${it.fileBaseName}(name=${it.fontName}, prefix=${it.mappingPrefix})" }
            )
        }
        return getRegisteredTypeface()
    }

    /** 所有已注册字体的摘要信息 */
    fun getRegisteredFonts(): List<FontInfo> =
        registered.map { FontInfo(it.fontName, it.mappingPrefix, it.iconCount) }

    /** 根据图标名称查询对应的 Unicode 字符；不存在返回 null */
    fun getIconChar(iconName: String, fontName: String? = null): Char? =
        resolveTypeface(fontName).getIconChar(iconName)

    /** 判断图标名称在指定字体中是否存在（默认字体当 fontName 为 null） */
    fun isIconExists(iconName: String, fontName: String? = null): Boolean =
        resolveTypeface(fontName).isIconExists(iconName)

    /** 指定字体的所有图标名称（已排序）；不传 fontName 使用默认字体 */
    fun iconNames(fontName: String? = null): List<String> =
        resolveTypeface(fontName).characters.keys.sorted()

    /** 指定字体的名称（JSON name 字段）；不传 fontName 使用默认字体 */
    fun fontName(fontName: String? = null): String = resolveTypeface(fontName).fontName

    /** 指定字体的图标数量；不传 fontName 使用默认字体 */
    fun iconCount(fontName: String? = null): Int = resolveTypeface(fontName).iconCount

    /** 指定字体的图标前缀（css_prefix_text 前 3 字符）；不传 fontName 使用默认字体 */
    fun mappingPrefix(fontName: String? = null): String = resolveTypeface(fontName).mappingPrefix

    /** 输出指定字体的完整信息 JSON（含元数据和所有图标映射）；不传 fontName 使用默认字体 */
    fun getSkyIconFontInfoJson(fontName: String? = null): String =
        resolveTypeface(fontName).getSkyIconFontInfoJson()
}
