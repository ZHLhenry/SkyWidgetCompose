package com.sky.widget.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

/**
 * 可点击高亮文本。
 *
 * 支持通过正则表达式匹配并高亮指定内容（如手机号、邮箱、协议条款等），并为匹配到的文本添加点击事件。
 *
 * ```
 * SkyAnnotatedText(
 *     text = "我已阅读并同意《隐私政策》",
 *     annotatedActions = listOf(
 *         SkyAnnotatedAction(regex = "《隐私政策》") { toast("点击隐私政策") }
 *     )
 * )
 * ```
 *
 * @param text 原始文本
 * @param modifier 外层修饰符
 * @param style 普通文本样式
 * @param annotatedStyle 匹配文本的高亮样式，默认使用主题主色
 * @param annotatedActions 高亮规则与点击回调列表；为空时直接渲染普通文本
 */
@Composable
fun SkyAnnotatedText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    ),
    annotatedStyle: SpanStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
    annotatedActions: List<SkyAnnotatedAction> = emptyList()
) {
    if (annotatedActions.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            style = style
        )
        return
    }

    // 收集所有匹配项并按起始位置排序，遇到重叠时取最前面的匹配
    val matches = annotatedActions
        .flatMapIndexed { index, action ->
            action.regex.toRegex().findAll(text).map { index to it }
        }
        .sortedBy { it.second.range.first }

    val annotatedString = buildAnnotatedString {
        var cursor = 0
        matches.forEach { (actionIndex, match) ->
            if (match.range.first < cursor) return@forEach

            if (cursor < match.range.first) {
                append(text.substring(cursor, match.range.first))
            }

            val action = annotatedActions[actionIndex]
            withLink(
                LinkAnnotation.Clickable(
                    tag = tagForMatch(actionIndex, match.value),
                    styles = TextLinkStyles(style = annotatedStyle),
                    linkInteractionListener = {
                        action.onClick?.invoke(match.value)
                    }
                )
            ) {
                append(match.value)
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        style = style
    )
}

/**
 * 高亮规则与点击回调。
 *
 * @param regex 用于匹配文本的正则表达式字符串
 * @param onClick 匹配文本被点击时的回调，入参为实际匹配到的字符串
 */
data class SkyAnnotatedAction(
    val regex: String,
    val onClick: ((match: String) -> Unit)? = null
)

private fun tagForMatch(index: Int, match: String): String = "sky_annotated_${index}_${match.hashCode()}"
