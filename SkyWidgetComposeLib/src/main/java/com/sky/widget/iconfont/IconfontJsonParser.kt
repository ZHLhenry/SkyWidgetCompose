package com.sky.widget.iconfont

/**
 * iconfont.cn 导出 JSON 文件的解析结果
 *
 * @property iconMapping 图标名称→Unicode 字符的映射
 * @property name 字体名称（JSON 中的 name 字段）
 * @property prefix CSS 前缀（JSON 中的 css_prefix_text 字段）
 * @property description 字体描述（JSON 中的 description 字段）
 */
internal data class IconfontParseResult(
    val iconMapping: Map<String, Char>,
    val name: String,
    val prefix: String,
    val description: String
)

/**
 * iconfont.cn 导出 JSON 文件的轻量解析器
 * 不依赖第三方库，仅解析所需的元数据字段和 glyphs 字段（移植自 SkyWidget 原仓库）。
 *
 * JSON 格式示例：
 * {
 *   "name": "testSky-iconfont",
 *   "css_prefix_text": "testSky",
 *   "description": "测试项目",
 *   "glyphs": [
 *     { "font_class": "fenxiang", "unicode": "e7fb" }
 *   ]
 * }
 */
internal object IconfontJsonParser {
    fun parse(jsonContent: String): IconfontParseResult {
        val tokenizer = JsonTokenizer(jsonContent)
        val root = tokenizer.parseObject()

        val name = root["name"] as? String ?: ""
        val prefix = root["css_prefix_text"] as? String
            ?: throw IllegalArgumentException(
                "iconfont.json 缺少 'css_prefix_text' 字段，请确认是 iconfont.cn 导出的标准 JSON 文件"
            )
        val description = root["description"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val glyphs = root["glyphs"] as? List<Map<String, Any?>>
            ?: throw IllegalArgumentException(
                "iconfont.json 缺少 'glyphs' 数组字段，请确认是 iconfont.cn 导出的标准 JSON 文件"
            )

        if (glyphs.isEmpty()) {
            throw IllegalArgumentException("iconfont.json 的 'glyphs' 数组为空，没有可用图标")
        }

        val result = HashMap<String, Char>(glyphs.size)
        for (glyph in glyphs) {
            val fontClass = glyph["font_class"] as? String
                ?: throw IllegalArgumentException("glyph 缺少 'font_class' 字段: $glyph")
            val unicode = glyph["unicode"] as? String
                ?: throw IllegalArgumentException("glyph 缺少 'unicode' 字段: $glyph")
            val codepoint = unicode.toIntOrNull(16)
                ?: throw IllegalArgumentException("glyph 'unicode' 值格式错误: '$unicode'")
            if (codepoint !in 1..0xFFFF) {
                throw IllegalArgumentException("glyph 'unicode' 值超出 BMP 范围: U+${codepoint.toString(16)}")
            }
            result[prefix + fontClass] = codepoint.toChar()
        }

        return IconfontParseResult(result, name, prefix, description)
    }

    /**
     * 轻量级 JSON 分词器，仅解析 iconfont.cn 导出的标准 JSON 格式
     */
    private class JsonTokenizer(private val input: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?> {
            skipWs()
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                map[key] = parseValue()
                skipWs()
                when (val c = peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        break
                    }
                    else -> throw err("expected ',' or '}', got '$c'")
                }
            }
            return map
        }

        private fun parseValue(): Any? {
            skipWs()
            return when (val c = peek()) {
                '\"' -> parseString()
                '{' -> parseObject()
                '[' -> parseArray()
                't' -> {
                    expectLit("true")
                    true
                }
                'f' -> {
                    expectLit("false")
                    false
                }
                'n' -> {
                    expectLit("null")
                    null
                }
                '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber()
                else -> throw err("unexpected char '$c'")
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWs()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                skipWs()
                list.add(parseValue())
                skipWs()
                when (val c = peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        break
                    }
                    else -> throw err("expected ',' or ']', got '$c'")
                }
            }
            return list
        }

        private fun parseString(): String {
            expect('\"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val c = input[pos++]
                when {
                    c == '\\' -> {
                        if (pos >= input.length) throw err("incomplete escape")
                        when (val e = input[pos++]) {
                            '\"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) throw err("incomplete unicode escape")
                                val hex = input.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> throw err("invalid escape '\\$e'")
                        }
                    }
                    c == '\"' -> return sb.toString()
                    else -> sb.append(c)
                }
            }
            throw err("unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < input.length && input[pos].isDigit()) pos++
            if (pos < input.length && input[pos] == '.') {
                pos++
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            val numStr = input.substring(start, pos)
            return if (numStr.contains('.') || numStr.contains('e') || numStr.contains('E')) {
                numStr.toDouble()
            } else {
                numStr.toLong()
            }
        }

        private fun skipWs() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= input.length) throw err("unexpected end of input")
            return input[pos]
        }

        private fun expect(c: Char) {
            if (pos >= input.length) throw err("expected '$c', got end of input")
            if (input[pos] != c) throw err("expected '$c', got '${input[pos]}'")
            pos++
        }

        private fun expectLit(lit: String) {
            if (pos + lit.length > input.length || input.substring(pos, pos + lit.length) != lit) {
                throw err("expected '$lit'")
            }
            pos += lit.length
        }

        private fun err(msg: String) = IllegalArgumentException("JSON parse error at pos $pos: $msg")
    }
}
