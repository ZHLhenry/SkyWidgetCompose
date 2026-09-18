package com.sky.widget.samplecp

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.pageIndicator.SkyPageIndicator
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.viewpage.SkyDragInteractionSource
import com.sky.widget.viewpage.SkyViewPage
import com.sky.widget.viewpage.rememberScalePagerContentTransformation
import com.sky.widget.viewpage.rememberSkyViewPageState

/**
 * 分页容器（SkyViewPage）完整 API 示例页。
 *
 * 逐节演示以下能力：
 * 1. 基本用法（pageCount / content / SkyViewPageScope.index）与页面指示器；
 * 2. 状态控制（rememberSkyViewPageState：当前索引 State、无动画/有动画翻页）；
 * 3. 滑动方向（orientation = Horizontal / Vertical）；
 * 4. 手势开关（userEnable = false，仅允许代码翻页）；
 * 5. 页面缓存（pageCache）；
 * 6. 自定义 key（pagerKey，减少重组）；
 * 7. 内容裁剪（clip）；
 * 8. 内容变换（contentTransformation 缩放效果）；
 * 9. 拖拽事件监听（scrollableInteractionSource）；
 * 10. 画廊轮播（两侧露出 + 中间放大 + 页码角标，复刻常见 Banner 画廊效果）。
 */
@Composable
fun ViewPageDemoScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "分页容器示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            BasicSection()
            StateControlSection()
            OrientationSection()
            UserEnableSection()
            PageCacheSection()
            PagerKeySection()
            TransformationSection()
            DragListenerSection()
            GallerySection()
        }
    }
}

/** 示例统一使用的页面配色，按索引循环取色。 */
private val demoColors = listOf(
    Color(0xFF2196F3),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
)

// ---------------------------------------------------------------------------
// 1. 基本用法 + 指示器
// ---------------------------------------------------------------------------

/**
 * 基本用法：仅传 pageCount 与 content。
 * content 的作用域为 SkyViewPageScope，可直接用 index 拿到当前页索引。
 */
@Composable
private fun BasicSection() {
    val state = rememberSkyViewPageState()

    SectionTitle("1. 基本用法（pageCount / content / index）")
    SkyViewPage(
        pageCount = 5,
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        // this 为 SkyViewPageScope，index 即当前页索引。
        DemoPage(index = index)
    }
    // 指示器与 SkyViewPage 联动：当前页自动随翻页更新，无需手动订阅索引。
    SkyPageIndicator(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    )
}

// ---------------------------------------------------------------------------
// 2. 状态控制
// ---------------------------------------------------------------------------

/**
 * 状态控制：通过 SkyViewPageState 读取当前索引、代码翻页（有/无动画）。
 */
@Composable
private fun StateControlSection() {
    val state = rememberSkyViewPageState()
    val currentIndex by state.getCurrSelectIndexState()

    SectionTitle("2. 状态控制（rememberSkyViewPageState）")
    SkyViewPage(
        pageCount = 5,
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        DemoPage(index = index)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "当前索引 getCurrSelectIndex() = $currentIndex；动画中 isAnimRunning() = ${state.isAnimRunning()}",
            fontSize = 13.sp,
            color = Color(0xFF2196F3)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 无动画跳转到第 0 页。
            OutlinedButton(onClick = { state.setPageIndex(0) }) { Text("跳到首页") }
            // 有动画跳转到最后一页。
            OutlinedButton(onClick = { state.setPageIndexWithAnimate(4) }) { Text("动画到末页") }
            // 有动画跳转到中间某页。
            OutlinedButton(onClick = { state.setPageIndexWithAnimate(2) }) { Text("动画到第3页") }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. 滑动方向
// ---------------------------------------------------------------------------

/**
 * orientation：支持水平与垂直两种滑动方向。
 */
@Composable
private fun OrientationSection() {
    SectionTitle("3. 滑动方向（orientation）")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("水平 Horizontal", fontSize = 12.sp, color = Color(0xFF999999))
            SkyViewPage(
                pageCount = 4,
                orientation = Orientation.Horizontal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { DemoPage(index = index) }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("垂直 Vertical", fontSize = 12.sp, color = Color(0xFF999999))
            SkyViewPage(
                pageCount = 4,
                orientation = Orientation.Vertical,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { DemoPage(index = index) }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. 手势开关
// ---------------------------------------------------------------------------

/**
 * userEnable = false 时用户手势滑动无效，但仍可通过 state 代码翻页。
 */
@Composable
private fun UserEnableSection() {
    val state = rememberSkyViewPageState()
    val currentIndex by state.getCurrSelectIndexState()

    SectionTitle("4. 手势开关（userEnable = false）")
    SkyViewPage(
        pageCount = 5,
        state = state,
        userEnable = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { DemoPage(index = index) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { state.setPageIndexWithAnimate((currentIndex - 1).coerceAtLeast(0)) },
            enabled = currentIndex > 0
        ) { Text("上一页") }
        Button(
            onClick = { state.setPageIndexWithAnimate((currentIndex + 1).coerceAtMost(4)) },
            enabled = currentIndex < 4
        ) { Text("下一页") }
        Text("手势被禁用，仅按钮翻页", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

// ---------------------------------------------------------------------------
// 5. 页面缓存
// ---------------------------------------------------------------------------

/**
 * pageCache：当前页左右（上下）各缓存的页数，值越大预渲染的相邻页越多，滑动手感更连贯。
 */
@Composable
private fun PageCacheSection() {
    SectionTitle("5. 页面缓存（pageCache = 2）")
    SkyViewPage(
        pageCount = 6,
        pageCache = 2,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { DemoPage(index = index) }
}

// ---------------------------------------------------------------------------
// 6. 自定义 key
// ---------------------------------------------------------------------------

/**
 * pagerKey：为每一页提供稳定 key，用于减少重组、提升性能（等同 LazyColumn items 的 key）。
 */
@Composable
private fun PagerKeySection() {
    SectionTitle("6. 自定义 key（pagerKey）")
    SkyViewPage(
        pageCount = 5,
        pagerKey = { index -> "demo_page_$index" },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { DemoPage(index = index) }
}

// ---------------------------------------------------------------------------
// 7 & 8. 内容裁剪与内容变换（缩放）
// ---------------------------------------------------------------------------

/**
 * contentTransformation：按页面相对偏移做缩放变换；
 * 配合 clip = false 可让相邻页的缩放效果溢出显示，形成画廊视差感。
 */
@Composable
private fun TransformationSection() {
    SectionTitle("7/8. 内容裁剪与变换（clip / contentTransformation）")
    SkyViewPage(
        pageCount = 5,
        clip = false,
        contentTransformation = rememberScalePagerContentTransformation(
            maxScale = 1f,
            minScale = 0.8f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 24.dp)
    ) { DemoPage(index = index) }
}

// ---------------------------------------------------------------------------
// 9. 拖拽事件监听
// ---------------------------------------------------------------------------

/**
 * scrollableInteractionSource：通过 SkyDragInteractionSource 监听拖拽开始/结束/取消事件。
 */
@Composable
private fun DragListenerSection() {
    var dragEventText by remember { mutableStateOf("尚未拖拽") }
    // remember 一个拖拽监听源，回调中更新事件文案。
    val interactionSource = remember {
        SkyDragInteractionSource { drag ->
            dragEventText = when (drag) {
                is DragInteraction.Start -> "拖拽开始（Start）"
                is DragInteraction.Stop -> "拖拽结束（Stop）"
                is DragInteraction.Cancel -> "拖拽取消（Cancel）"
                else -> dragEventText
            }
        }
    }

    SectionTitle("9. 拖拽事件监听（scrollableInteractionSource）")
    SkyViewPage(
        pageCount = 5,
        scrollableInteractionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) { DemoPage(index = index) }
    Text(
        text = dragEventText,
        fontSize = 13.sp,
        color = Color(0xFF2196F3),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// 10. 画廊轮播（两侧露出 + 中间放大 + 页码角标）
// ---------------------------------------------------------------------------

/**
 * 画廊轮播效果，复刻常见 Banner 画廊样式：
 * - 两侧露出相邻页：给 [SkyViewPage] 的 modifier 加水平 padding，并设 clip = false，
 *   使相邻页绘制进两侧留白，形成左右窄条；
 * - 中间大、两侧小：contentTransformation 使用缩放变换，当前页满尺寸、相邻页缩小；
 * - 页码角标：在每页 content 内以 BottomEnd 对齐放置白色圆角数字块。
 *
 * 注意：缩放会把相邻页朝自身中心收缩，从而抵消部分留白，实际露出宽度约为
 * `padding - (1 - minScale) / 2 * 页宽`。因此这里用较大的 padding（48.dp）
 * 配合较小的缩放差（minScale = 0.9f），保证两侧露出足够明显。
 */
@Composable
private fun GallerySection() {
    SectionTitle("10. 画廊轮播（两侧露出 + 缩放 + 页码角标）")
    SkyViewPage(
        pageCount = 5,
        clip = false,
        pageCache = 2,
        contentTransformation = rememberScalePagerContentTransformation(
            maxScale = 1f,
            minScale = 0.9f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            // 两侧留白即相邻页的露出宽度；值越大露出越明显。
            .padding(horizontal = 48.dp)
    ) {
        GalleryPage(index = index)
    }
}

/**
 * 画廊单页：渐变背景 + 圆角 + 右下角白色页码角标。
 *
 * @param index 当前页索引（来自 SkyViewPageScope）。
 */
@Composable
private fun GalleryPage(index: Int) {
    val base = demoColors[index % demoColors.size]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(base, base.copy(alpha = 0.6f))),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Page $index",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        // 右下角页码角标：白色圆角块 + 数字（index 从 0 起，展示时 +1）。
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${index + 1}",
                color = Color(0xFF333333),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 通用小组件
// ---------------------------------------------------------------------------

/**
 * 分组标题。
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color(0xFF999999),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/**
 * 单页内容：圆角色块 + 页码文字。
 *
 * @param index 当前页索引（来自 SkyViewPageScope）。
 */
@Composable
private fun DemoPage(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(demoColors[index % demoColors.size], RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "第 $index 页",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ViewPageDemoScreenPreview() {
    SkyWidgetComposeTheme {
        ViewPageDemoScreen(onBack = {})
    }
}
