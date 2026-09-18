package com.sky.widget.samplecp

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.banner.SkyBanner
import com.sky.widget.banner.rememberSkyBannerState
import com.sky.widget.pageIndicator.SkyNumberIndicatorStyle
import com.sky.widget.pageIndicator.SkyPageIndicator
import com.sky.widget.pageIndicator.SkyUnderlineIndicatorStyle
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.viewpage.rememberScalePagerContentTransformation
import kotlin.math.roundToInt

/**
 * 轮播容器（SkyBanner）完整 API 示例页。
 *
 * 逐节演示以下能力：
 * 1. 基本用法（pageCount / content / SkyBannerScope.index）与页面指示器；
 * 2. 状态控制（rememberSkyBannerState：当前索引 State、Flow、无动画/有动画翻页、动画状态）；
 * 3. 点击事件（单页点击用 scope.index；整体点击用 getCurrSelectIndexState）；
 * 4. 自动轮播开关（autoScroll）与间隔时间（autoScrollTime）；
 * 5. 滑动方向（orientation = Horizontal / Vertical）；
 * 6. 手势开关（userEnable = false，仅允许代码翻页）；
 * 7. 自定义 key（bannerKey，减少重组）；
 * 8. 画廊轮播（clip = false + contentTransformation 缩放，两侧露出）；
 * 9. 偏移量读取（getOffsetState）；
 * 10. 自定义指示器（SkyPageIndicator 的 itemContent）；
 * 11. 内置指示器样式（圆点 / 下划线 / 数字）与消费者自定义指示器；
 * 12. 自定义滑动动画（pageAnimationSpec）。
 */
@Composable
fun BannerDemoScreen(onBack: () -> Unit) {
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
                text = "轮播容器示例",
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
            BannerBasicSection()
            BannerStateControlSection()
            BannerClickSection()
            BannerAutoScrollSection()
            BannerOrientationSection()
            BannerUserEnableSection()
            BannerKeySection()
            BannerGallerySection()
            BannerOffsetSection()
            BannerCustomIndicatorSection()
            BannerBuiltinIndicatorSection()
            BannerAnimationSpecSection()
        }
    }
}

/** 示例统一使用的页面配色，按索引循环取色。 */
private val bannerDemoColors = listOf(
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
 * content 的作用域为 SkyBannerScope，可直接用 index 拿到当前页的真实索引（已对 pageCount 取模）。
 */
@Composable
private fun BannerBasicSection() {
    val bannerState = rememberSkyBannerState()

    BannerSectionTitle("1. 基本用法（pageCount / content / index）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        // this 为 SkyBannerScope，index 即当前页真实索引。
        BannerDemoPage(index = index)
    }
    // 指示器与 SkyBanner 联动：当前页与总页数均自动同步，零配置。
    SkyPageIndicator(
        state = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    )
}

// ---------------------------------------------------------------------------
// 2. 状态控制
// ---------------------------------------------------------------------------

/**
 * 状态控制：通过 SkyBannerState 读取当前索引（State / Flow）、代码翻页（有/无动画）、查询动画状态。
 */
@Composable
private fun BannerStateControlSection() {
    val bannerState = rememberSkyBannerState()
    val currentIndex by bannerState.getCurrSelectIndexState()
    // createCurrSelectIndexFlow() 返回 Flow<Int>，用 collectAsState 收集为可观察状态。
    val flowIndex by remember { bannerState.createCurrSelectIndexFlow() }
        .collectAsState(initial = 0)

    BannerSectionTitle("2. 状态控制（rememberSkyBannerState）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        autoScroll = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        BannerDemoPage(index = index)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "State 索引 = $currentIndex；Flow 索引 = $flowIndex；动画中 = ${bannerState.isAnimRunning()}",
            fontSize = 13.sp,
            color = Color(0xFF2196F3)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 无动画跳转到第 0 页（真实索引）。
            OutlinedButton(onClick = { bannerState.setPageIndex(0) }) { Text("跳到首页") }
            // 有动画跳转到最后一页。
            OutlinedButton(onClick = { bannerState.setPageIndexWithAnimate(4) }) { Text("动画到末页") }
            // 有动画跳转到中间某页。
            OutlinedButton(onClick = { bannerState.setPageIndexWithAnimate(2) }) { Text("动画到第3页") }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. 点击事件（消费者获知点击的是哪一页）
// ---------------------------------------------------------------------------

/**
 * 点击事件：演示两种获知“当前是第几页”的方式。
 * - 单页各自点击：在 content 内直接使用 SkyBannerScope.index；
 * - 整体统一点击：在 content 外通过 getCurrSelectIndexState() 订阅当前页后读取。
 */
@Composable
private fun BannerClickSection() {
    val bannerState = rememberSkyBannerState()
    val currentIndex by bannerState.getCurrSelectIndexState()
    var clickTip by remember { mutableStateOf("尚未点击") }

    BannerSectionTitle("3. 点击事件（scope.index / getCurrSelectIndexState）")
    SkyBanner(
        pageCount = 4,
        bannerState = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        // 每页各自可点击，直接用 index 知道点的是哪一页。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { clickTip = "单页点击：第 $index 页" }
        ) {
            BannerDemoPage(index = index)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 整体按钮：读取当前订阅到的页索引。
        Button(onClick = { clickTip = "整体点击：当前第 $currentIndex 页" }) {
            Text("读取当前页")
        }
        Text(text = clickTip, fontSize = 13.sp, color = Color(0xFF333333))
    }
}

// ---------------------------------------------------------------------------
// 4. 自动轮播开关与间隔
// ---------------------------------------------------------------------------

/**
 * autoScroll 控制是否自动轮播；autoScrollTime 控制轮播间隔（毫秒）。
 * 拖动滑块可实时调整间隔，切换开关可启停自动轮播。
 */
@Composable
private fun BannerAutoScrollSection() {
    var autoScroll by remember { mutableStateOf(true) }
    var intervalSeconds by remember { mutableFloatStateOf(2f) }

    BannerSectionTitle("4. 自动轮播（autoScroll / autoScrollTime）")
    SkyBanner(
        pageCount = 5,
        autoScroll = autoScroll,
        // autoScrollTime 单位为毫秒，这里由秒换算。
        autoScrollTime = (intervalSeconds * 1000).roundToInt().toLong(),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        BannerDemoPage(index = index)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("自动轮播", fontSize = 13.sp, color = Color(0xFF666666))
        Switch(checked = autoScroll, onCheckedChange = { autoScroll = it })
        Spacer(Modifier.size(12.dp))
        Text(
            text = "间隔 ${"%.1f".format(intervalSeconds)}s",
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )
    }
    Slider(
        value = intervalSeconds,
        onValueChange = { intervalSeconds = it },
        valueRange = 0.5f..5f,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ---------------------------------------------------------------------------
// 5. 滑动方向
// ---------------------------------------------------------------------------

/**
 * orientation：支持水平与垂直两种轮播方向。
 */
@Composable
private fun BannerOrientationSection() {
    BannerSectionTitle("5. 滑动方向（orientation）")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("水平 Horizontal", fontSize = 12.sp, color = Color(0xFF999999))
            SkyBanner(
                pageCount = 4,
                orientation = Orientation.Horizontal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { BannerDemoPage(index = index) }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("垂直 Vertical", fontSize = 12.sp, color = Color(0xFF999999))
            SkyBanner(
                pageCount = 4,
                orientation = Orientation.Vertical,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { BannerDemoPage(index = index) }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. 手势开关
// ---------------------------------------------------------------------------

/**
 * userEnable = false 时用户手势滑动无效，但仍可通过 bannerState 代码翻页（自动轮播仍生效）。
 */
@Composable
private fun BannerUserEnableSection() {
    val bannerState = rememberSkyBannerState()
    val currentIndex by bannerState.getCurrSelectIndexState()

    BannerSectionTitle("6. 手势开关（userEnable = false）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        userEnable = false,
        autoScroll = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { BannerDemoPage(index = index) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { bannerState.setPageIndexWithAnimate((currentIndex - 1 + 5) % 5) }) {
            Text("上一页")
        }
        Button(onClick = { bannerState.setPageIndexWithAnimate((currentIndex + 1) % 5) }) {
            Text("下一页")
        }
        Text("手势被禁用，仅按钮翻页", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

// ---------------------------------------------------------------------------
// 7. 自定义 key
// ---------------------------------------------------------------------------

/**
 * bannerKey：为每一页提供稳定 key，用于减少重组、提升性能（等同 LazyColumn items 的 key）。
 */
@Composable
private fun BannerKeySection() {
    BannerSectionTitle("7. 自定义 key（bannerKey）")
    SkyBanner(
        pageCount = 5,
        bannerKey = { index -> "banner_page_$index" },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { BannerDemoPage(index = index) }
}

// ---------------------------------------------------------------------------
// 8. 画廊轮播（两侧露出 + 缩放）
// ---------------------------------------------------------------------------

/**
 * 画廊轮播效果：
 * - clip = false 让相邻页可绘制到两侧留白区；
 * - contentTransformation 使用缩放变换，当前页满尺寸、相邻页缩小；
 * - 给 modifier 加水平 padding 形成两侧露出。
 */
@Composable
private fun BannerGallerySection() {
    BannerSectionTitle("8. 画廊轮播（clip / contentTransformation）")
    SkyBanner(
        pageCount = 5,
        clip = false,
        contentTransformation = rememberScalePagerContentTransformation(
            maxScale = 1f,
            minScale = 0.9f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            // 两侧留白即相邻页的露出宽度；值越大露出越明显。
            .padding(horizontal = 48.dp)
    ) {
        BannerGalleryPage(index = index)
    }
}

// ---------------------------------------------------------------------------
// 9. 偏移量读取
// ---------------------------------------------------------------------------

/**
 * getOffsetState()：获取内部拖拽/翻页偏移量的 State，可用于联动自定义动画。
 */
@Composable
private fun BannerOffsetSection() {
    val bannerState = rememberSkyBannerState()
    val offset by bannerState.getOffsetState()

    BannerSectionTitle("9. 偏移量读取（getOffsetState）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { BannerDemoPage(index = index) }
    Text(
        text = "当前偏移量 offset = ${"%.1f".format(offset)} px",
        fontSize = 13.sp,
        color = Color(0xFF2196F3),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// 10. 自定义指示器（itemContent）
// ---------------------------------------------------------------------------

/**
 * 自定义指示器：通过 [SkyPageIndicator] 的 itemContent 提供每一项的绘制，实现任意样式。
 * 此处演示“选中变宽的胶囊 + 页码数字”样式，作用域可读到 index / selected / count。
 */
@Composable
private fun BannerCustomIndicatorSection() {
    val bannerState = rememberSkyBannerState()

    BannerSectionTitle("10. 自定义指示器（itemContent）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { BannerDemoPage(index = index) }
    SkyPageIndicator(
        state = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        // this 为 SkyPageIndicatorScope：index 当前项、selected 是否选中、count 总数。
        Box(
            modifier = Modifier
                .background(
                    color = if (selected) Color(0xFF2196F3) else Color(0xFFDDDDDD),
                    shape = RoundedCornerShape(50),
                )
                .padding(
                    horizontal = if (selected) 12.dp else 6.dp,
                    vertical = 4.dp
                )
        ) {
            Text(text = "${index + 1}", color = Color.White, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// 11. 内置指示器样式（圆点 / 下划线 / 数字）
// ---------------------------------------------------------------------------

/**
 * 内置与自定义指示器样式：同一个 Banner 状态驱动多种样式，供消费者选用。
 * 圆点为默认（不传 itemContent）；下划线、数字通过 itemContent 选用对应内置 Item；
 * 消费者也可完全不依赖内置 Item，直接在 itemContent 里自行绘制（自定义）。
 */
@Composable
private fun BannerBuiltinIndicatorSection() {
    val bannerState = rememberSkyBannerState()

    BannerSectionTitle("11. 指示器样式（圆点/下划线/数字/自定义）")
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) { BannerDemoPage(index = index) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 圆点：默认样式，不传 itemContent。
        SkyPageIndicator(state = bannerState, modifier = Modifier.fillMaxWidth())
        // 下划线：选用 SkyUnderlineIndicatorStyle。
        SkyPageIndicator(state = bannerState, modifier = Modifier.fillMaxWidth()) {
            SkyUnderlineIndicatorStyle()
        }
        // 数字：选用 SkyNumberIndicatorStyle。
        SkyPageIndicator(state = bannerState, modifier = Modifier.fillMaxWidth()) {
            SkyNumberIndicatorStyle()
        }
        // 自定义：消费者不依赖任何内置 Item，直接在 itemContent 里自行绘制（此处为选中放大的菱形）。
        SkyPageIndicator(state = bannerState, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(if (selected) 12.dp else 8.dp)
                    .rotate(45f)
                    .background(
                        color = if (selected) Color(0xFFE91E63) else Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 12. 自定义滑动动画（pageAnimationSpec）
// ---------------------------------------------------------------------------

/**
 * 自定义滑动动画：通过 pageAnimationSpec 切换翻页/回弹的时间曲线。
 * 提供默认 spring、慢速 tween、高回弹 spring 三种循环切换对比手感。
 */
@Composable
private fun BannerAnimationSpecSection() {
    val bannerState = rememberSkyBannerState()
    var specIndex by remember { mutableIntStateOf(0) }
    // 可选时间曲线：默认 spring / 慢速 tween / 高回弹 spring。
    val specs = listOf(
        "默认 spring" to spring<Float>(),
        "慢速 tween 800ms" to tween<Float>(durationMillis = 800, easing = FastOutSlowInEasing),
        "高回弹 spring" to spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy),
    )

    BannerSectionTitle("12. 自定义滑动动画（pageAnimationSpec）")
    Button(onClick = { specIndex = (specIndex + 1) % specs.size }) {
        Text("切换曲线：${specs[specIndex].first}")
    }
    SkyBanner(
        pageCount = 5,
        bannerState = bannerState,
        autoScroll = false,
        pageAnimationSpec = specs[specIndex].second,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) { BannerDemoPage(index = index) }
    SkyPageIndicator(
        state = bannerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    )
}

// ---------------------------------------------------------------------------
// 通用小组件
// ---------------------------------------------------------------------------

/**
 * 分组标题。
 */
@Composable
private fun BannerSectionTitle(title: String) {
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
 * @param index 当前页真实索引（来自 SkyBannerScope）。
 */
@Composable
private fun BannerDemoPage(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(bannerDemoColors[index % bannerDemoColors.size], RoundedCornerShape(12.dp)),
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

/**
 * 画廊单页：渐变背景 + 圆角 + 右下角白色页码角标。
 *
 * @param index 当前页真实索引（来自 SkyBannerScope）。
 */
@Composable
private fun BannerGalleryPage(index: Int) {
    val base = bannerDemoColors[index % bannerDemoColors.size]
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun BannerDemoScreenPreview() {
    SkyWidgetComposeTheme {
        BannerDemoScreen(onBack = {})
    }
}
