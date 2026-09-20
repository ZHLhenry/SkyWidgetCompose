package com.sky.widget.samplecp

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import com.sky.widget.cell.SkyCellLayout
import com.sky.widget.iface.SkyIconType
import com.sky.widget.preview.SkyPreView
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
/**
 * 图片预览（SkyPreView）示例页。
 *
 * 演示：网络图片 / 高清长图 / 本地资源图 / GIF 动图 / 多图的全屏预览（单图即单元素列表），
 * 支持双指捏合缩放、双指平移、双击放大/复位、左右翻页、长按提示。
 */
@Composable
fun PreViewDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var previewModels by remember { mutableStateOf<List<Any?>?>(null) }
    var previewStartIndex by remember { mutableIntStateOf(0) }
    var previewCloseIcon by remember { mutableStateOf<SkyIconType?>(null) }
    var previewImageLoader by remember { mutableStateOf<ImageLoader?>(null) }
    val closePainter = rememberVectorPainter(Icons.Default.Close)

    // 自定义 ImageLoader 示例：crossfade 渐显 + 25% 内存缓存 + 显式 GIF 解码器
    val customImageLoader = remember {
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // 单图预览（单元素列表）
    val openSingle: (Any?) -> Unit = { model ->
        previewStartIndex = 0
        previewCloseIcon = null
        previewImageLoader = null
        previewModels = listOf(model)
    }

    // 多图示例数据
    val networkModels = remember {
        listOf<Any?>(
            "https://picsum.photos/seed/sky1/1080/720",
            "https://picsum.photos/seed/sky2/720/1080",
            "https://picsum.photos/seed/sky3/1080/1080",
            "https://picsum.photos/seed/sky4/900/1200"
        )
    }
    val mixedModels = remember {
        listOf<Any?>(
            "https://picsum.photos/seed/mix1/1080/720",
            R.drawable.ic_sample_orange,
            "https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif",
            R.drawable.ic_sample_purple
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = "图片预览",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "点击进入全屏预览：双指缩放 · 双指平移 · 双击放大/复位 · 左右翻页",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF666666),
                fontSize = 13.sp
            )

            SkyCellLayout(
                text = "网络图片",
                secondaryText = "https://picsum.photos/1080/720",
                showDivider = true,
                onClick = { openSingle("https://picsum.photos/1080/720") }
            )
            SkyCellLayout(
                text = "高清长图",
                secondaryText = "https://picsum.photos/720/1600",
                showDivider = true,
                onClick = { openSingle("https://picsum.photos/720/1600") }
            )
            SkyCellLayout(
                text = "本地资源图",
                secondaryText = "R.drawable.ic_sample_blue",
                showDivider = true,
                onClick = { openSingle(R.drawable.ic_sample_blue) }
            )
            SkyCellLayout(
                text = "GIF 动图",
                secondaryText = "需要消费者额外引入 coil-gif",
                showDivider = true,
                onClick = {
                    openSingle("https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif")
                }
            )
            SkyCellLayout(
                text = "多图预览（网络图片）",
                secondaryText = "4 张网图，左右滑动切换，初始第 2 张",
                showDivider = true,
                onClick = {
                    previewStartIndex = 1
                    previewCloseIcon = null
                    previewImageLoader = null
                    previewModels = networkModels
                }
            )
            SkyCellLayout(
                text = "多图预览（混合图源）",
                secondaryText = "网图 + 本地资源 + GIF",
                showDivider = true,
                onClick = {
                    previewStartIndex = 0
                    previewCloseIcon = null
                    previewImageLoader = null
                    previewModels = mixedModels
                }
            )
            SkyCellLayout(
                text = "自定义 ImageLoader",
                secondaryText = "imageLoader：crossfade + 25% 内存缓存 + GIF 解码器",
                showDivider = true,
                onClick = {
                    previewStartIndex = 0
                    previewCloseIcon = null
                    previewImageLoader = customImageLoader
                    previewModels = mixedModels
                }
            )
            SkyCellLayout(
                text = "自定义关闭按钮（iconfont）",
                secondaryText = "closeIcon = SkyIconType.iconFont(...)",
                showDivider = true,
                onClick = {
                    previewStartIndex = 0
                    // 演示 iconfont 关闭按钮（示例字体无 close 字形，用 shouye 代替演示能力）
                    previewCloseIcon = SkyIconType.iconFont("testSkyshouye", "testSky_iconfont")
                    previewImageLoader = null
                    previewModels = networkModels
                }
            )
            SkyCellLayout(
                text = "自定义关闭按钮（Painter）",
                secondaryText = "closeIcon = SkyIconType.painter(...)",
                showDivider = true,
                onClick = {
                    previewStartIndex = 0
                    previewCloseIcon = SkyIconType.painter(closePainter)
                    previewImageLoader = null
                    previewModels = mixedModels
                }
            )
        }

        // 全屏预览层（单图/多图同一入口）
        previewModels?.let { models ->
            key(models) {
                SkyPreView(
                    models = models,
                    modifier = Modifier.fillMaxSize(),
                    initialIndex = previewStartIndex,
                    onClose = { previewModels = null },
                    closeIcon = previewCloseIcon,
                    imageLoader = previewImageLoader,
                    onLongClick = { index ->
                        Toast.makeText(context, "长按第 ${index + 1} 张", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreViewDemoScreenPreview() {
    SkyWidgetComposeTheme {
        PreViewDemoScreen(onBack = {})
    }
}
