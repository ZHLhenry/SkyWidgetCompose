package com.sky.widget.grid

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 静态网格布局。
 *
 * 按固定列数展示一组数据，每一项由业务层通过 [itemContent] 自定义。
 *
 * ```
 * SkyGridLayout(
 *     items = products,
 *     columns = 3,
 *     horizontalSpacing = 8.dp,
 *     verticalSpacing = 12.dp,
 *     contentPadding = PaddingValues(16.dp)
 * ) { product ->
 *     ProductCard(product)
 * }
 * ```
 *
 * @param items 数据列表
 * @param modifier 外层修饰符
 * @param columns 列数，至少为 1
 * @param horizontalSpacing 列间距
 * @param verticalSpacing 行间距
 * @param contentPadding 整体内边距
 * @param itemContent 每个数据项的 Composable 内容
 */
@Composable
fun <T> SkyGridLayout(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (item: T) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    Layout(
        modifier = modifier,
        content = {
            items.forEach { itemContent(it) }
        }
    ) { measurables, constraints ->
        val columnsCount = columns.coerceAtLeast(1)

        val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()

        val hGapPx = with(density) { horizontalSpacing.roundToPx() }
        val vGapPx = with(density) { verticalSpacing.roundToPx() }

        val contentWidth = constraints.maxWidth - startPadding - endPadding
        val totalGap = hGapPx * (columnsCount - 1)
        val cellWidth = if (contentWidth > totalGap) {
            (contentWidth - totalGap) / columnsCount
        } else {
            0
        }

        if (cellWidth <= 0) {
            return@Layout layout(constraints.maxWidth, constraints.minHeight) {}
        }

        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints.fixedWidth(cellWidth))
        }

        val rows = placeables.chunked(columnsCount)
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        val totalHeight = topPadding + bottomPadding +
                rowHeights.sum() + vGapPx * (rows.size - 1).coerceAtLeast(0)

        val height = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constraints.maxWidth, height) {
            var y = topPadding
            rows.forEachIndexed { rowIndex, row ->
                var x = startPadding
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += cellWidth + hGapPx
                }
                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}
