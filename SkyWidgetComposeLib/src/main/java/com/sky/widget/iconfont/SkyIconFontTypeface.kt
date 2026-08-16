package com.sky.widget.iconfont

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

/**
 * sky-iconfont 字体类型实现（库内部使用）。
 *
 * 负责加载 TTF 字体文件与对应的 iconfont.cn 导出 JSON 映射，
 * 提供「图标名称 → Unicode 字符」查询能力。
 *
 * 与基于 mikepenz/iconics 的原仓库实现不同，本 Compose 版本不依赖任何三方图标库，
 * 仅使用 Android 原生 [Typeface] / [FontFamily] 完成渲染。
 *
 * JSON 文件路径由 TTF 路径推导：将 `.ttf` 扩展名替换为 `.json`。
 * 例如：`fonts/sky_iconfont.ttf` → `fonts/sky_iconfont.json`。
 * JSON 文件需放在 `assets` 目录下（与 TTF 同级）。
 *
 * @param ttfAssetPath TTF 字体文件在 assets 中的路径，默认 `fonts/sky_iconfont.ttf`
 */
internal class SkyIconFontTypeface(
    private val ttfAssetPath: String = "fonts/sky_iconfont.ttf"
) {
    companion object {
        /** 当前激活的默认字体实例，由 [SkyIconFontsLib.initRegister] 自动设置 */
        internal var default: SkyIconFontTypeface? = null
    }

    private var _typeface: Typeface? = null
    private var _fontFamily: FontFamily? = null
    private var _characters: Map<String, Char>? = null
    private var _jsonResult: IconfontParseResult? = null
    private var _jsonLoaded = false

    /** 字体文件名（不含 .ttf 扩展名），由 TTF 路径推导，作为最稳定的精确匹配键 */
    val fileBaseName: String = ttfAssetPath
        .substringAfterLast('/')
        .substringBeforeLast('.')

    /** 字体名称（JSON 中的 name 字段），解析失败回退为字体文件名 */
    val fontName: String
        get() {
            ensureJsonLoaded()
            return _jsonResult!!.name.ifEmpty { fileBaseName }
        }

    /** 图标前缀（css_prefix_text 前 3 个字符），作为备选精确匹配键 */
    val mappingPrefix: String
        get() {
            ensureJsonLoaded()
            return _jsonResult!!.prefix.take(3)
        }

    val iconCount: Int
        get() = characters.size

    /** 字体描述 */
    val description: String
        get() {
            ensureJsonLoaded()
            return _jsonResult!!.description
        }

    /** 名称→字符映射（来自 JSON 解析结果） */
    val characters: Map<String, Char>
        get() {
            if (_characters == null) {
                ensureJsonLoaded()
                _characters = HashMap(_jsonResult!!.iconMapping)
            }
            return _characters!!
        }

    /** 已加载的 Compose [FontFamily]，缓存复用 */
    val fontFamily: FontFamily
        get() {
            if (_fontFamily == null) {
                _fontFamily = FontFamily(rawTypeface)
            }
            return _fontFamily!!
        }

    /** 加载 TTF 字体文件（懒加载并缓存） */
    val rawTypeface: Typeface
        get() {
            if (_typeface == null) {
                _typeface = Typeface.createFromAsset(
                    SkyIconFontsLib.applicationContext.assets,
                    ttfAssetPath
                )
            }
            return _typeface!!
        }

    /**
     * 加载 TTF 对应的 JSON 映射文件（由 TTF 路径推导同名 .json，从 assets 读取）。
     */
    private fun ensureJsonLoaded() {
        if (_jsonLoaded) return
        _jsonLoaded = true
        _jsonResult = try {
            val jsonFilePath = ttfAssetPath.substringBeforeLast('.', ttfAssetPath) + ".json"
            val jsonContent = SkyIconFontsLib.applicationContext.assets.open(jsonFilePath)
                .bufferedReader().use { it.readText() }
            IconfontJsonParser.parse(jsonContent)
        } catch (e: java.io.FileNotFoundException) {
            throw IllegalArgumentException(
                "TTF '$ttfAssetPath' 需要同名的 JSON 映射文件。\n" +
                        "请在 assets 目录放置 iconfont.cn 导出的标准 JSON 文件（含 css_prefix_text 和 glyphs 字段）",
                e
            )
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "解析 iconfont.json 失败: ${e.message}\n请确认是 iconfont.cn 导出的标准 JSON 格式",
                e
            )
        }
    }

    /** 根据图标名称获取对应的 Unicode 字符；不存在返回 null */
    fun getIconChar(iconName: String): Char? = characters[iconName]

    /** 判断图标名称是否存在 */
    fun isIconExists(iconName: String): Boolean = characters.containsKey(iconName)

    /** 输出字体完整信息（JSON 格式），包含元数据和所有图标映射 */
    fun getSkyIconFontInfoJson(): String {
        ensureJsonLoaded()
        val r = _jsonResult!!
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"name\": \"${escapeJson(r.name)}\",\n")
        sb.append("  \"font_file\": \"${escapeJson(ttfAssetPath)}\",\n")
        sb.append("  \"prefix\": \"${escapeJson(r.prefix)}\",\n")
        sb.append("  \"description\": \"${escapeJson(r.description)}\",\n")
        sb.append("  \"icon_count\": $iconCount,\n")
        sb.append("  \"icons\": {\n")
        val iconList = characters.entries.sortedBy { it.key }
        iconList.forEachIndexed { index, (name, char) ->
            val comma = if (index < iconList.size - 1) "," else ""
            sb.append("    \"${escapeJson(name)}\": \"U+${char.code.toString(16).padStart(4, '0')}\"$comma\n")
        }
        sb.append("  }\n}")
        return sb.toString()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
