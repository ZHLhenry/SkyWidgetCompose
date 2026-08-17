package com.sky.widget.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sky.widget.qrcode.SkyBarcodeFormat
import com.sky.widget.qrcode.SkyBarcodeImage
import com.sky.widget.qrcode.SkyQRCode
import com.sky.widget.qrcode.SkyQRCodeImage
import com.sky.widget.qrcode.SkyQRCodeMode
import com.sky.widget.qrcode.SkyQRCodeResult
import com.sky.widget.qrcode.SkyQRCodeScanner
import com.sky.widget.qrcode.SkyQRCodeState
import com.sky.widget.qrcode.SkyQRCodeViewfinder
import com.sky.widget.qrcode.rememberSkyQRCodeState
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.graphics.createBitmap

/**
 * 二维码组件示例页。
 *
 * 覆盖二维码模块全部公开 API：
 * - [SkyQRCodeScanner]：modifier / state / onResult / onError / viewfinder / overlay
 * - [rememberSkyQRCodeState] 与 [com.sky.widget.qrcode.SkyQRCodeState] 全部属性、setter、pause / resume
 * - [SkyQRCodeMode] 全部枚举值
 * - [SkyQRCodeViewfinder] 自定义视觉参数
 * - [SkyQRCodeImage]：content / size / logo / logoSize / logoCornerRadius
 * - [SkyBarcodeImage]：content / format / width / height
 * - [SkyQRCode.createQRCode] 两个重载、[SkyQRCode.createBarcode]、[SkyQRCode.analyzeBitmap] 两个重载
 */
@Composable
fun QRCodeDemoScreen(onBack: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val titles = listOf("扫描", "生成", "解析")

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题栏：与其他示例页保持统一的蓝色沉浸式风格
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
                text = "二维码",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) { page ->
            when (page) {
                0 -> ScanTab()
                1 -> GenerateTab()
                else -> AnalyzeTab()
            }
        }
    }
}

/**
 * 扫描示例：演示 [SkyQRCodeScanner] 全部参数与 [com.sky.widget.qrcode.SkyQRCodeState] 全部控制能力。
 */
@Composable
private fun ScanTab() {
    val context = LocalContext.current
    // 初始参数演示：默认开启提示音与震动
    val state = rememberSkyQRCodeState(beepEnabled = true, vibrateEnabled = true)
    var scanResult by remember { mutableStateOf<SkyQRCodeResult?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "需要相机权限才能扫码")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text(text = "申请权限")
                    }
                }
            }

            else -> {
                SkyQRCodeScanner(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onResult = { scanResult = it },
                    onError = { scanResult = SkyQRCodeResult.Failure("相机错误：${it.message}") },
                    // 自定义取景框视觉参数演示
                    viewfinder = { st ->
                        SkyQRCodeViewfinder(
                            state = st,
                            modifier = Modifier.fillMaxSize(),
                            cornerColor = Color(0xFFFFC107),
                            scanLineColor = Color(0xFFFFC107),
                            scanDuration = 1600
                        )
                    },
                    // 自定义覆盖层演示：控制面板放置在预览上层
                    overlay = {
                        ScanControlPanel(state = state, scanResult = scanResult)
                    }
                )
            }
        }
    }
}

/**
 * 扫描控制面板：演示 state 的全部 setter 与 pause / resume。
 */
@Composable
private fun BoxScope.ScanControlPanel(
    state: SkyQRCodeState,
    scanResult: SkyQRCodeResult?
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        // 识别结果：Success 展示文本与格式，Failure 展示失败信息
        scanResult?.let { result ->
            Text(
                text = when (result) {
                    is SkyQRCodeResult.Success -> "结果：${result.text}（${result.format ?: "未知格式"}）"
                    is SkyQRCodeResult.Failure -> result.text ?: "识别失败"
                },
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 闪光灯 / 提示音 / 震动 / 自定义提示音开关
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabeledSwitch(
                label = "闪光灯",
                checked = state.flashEnabled,
                onCheckedChange = { state.setFlashEnabled(it) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            LabeledSwitch(
                label = "提示音",
                checked = state.beepEnabled,
                onCheckedChange = { state.setBeepEnabled(it) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            LabeledSwitch(
                label = "震动",
                checked = state.vibrateEnabled,
                onCheckedChange = { state.setVibrateEnabled(it) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 自定义提示音演示：开启时使用 app 内置 demo_beep，关闭时恢复库默认提示音
            LabeledSwitch(
                label = "自定义提示音",
                checked = state.beepResId != 0,
                onCheckedChange = { state.setBeepResId(if (it) R.raw.demo_beep else 0) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 扫描模式切换：覆盖 SkyQRCodeMode 全部枚举值
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SkyQRCodeMode.entries.forEach { mode ->
                TagButton(
                    text = mode.name,
                    selected = state.mode == mode,
                    onClick = { state.setMode(mode) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 暂停 / 继续、取景框尺寸、取景框顶部边距
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TagButton(
                text = if (state.isScanning) "暂停扫描" else "继续扫描",
                selected = false,
                onClick = { if (state.isScanning) state.pause() else state.resume() }
            )
            val isDefaultSize = state.frameSize.width == Dp.Unspecified
            TagButton(
                text = "取景框默认",
                selected = isDefaultSize,
                onClick = { state.setFrameSize(DpSize(Dp.Unspecified, Dp.Unspecified)) }
            )
            TagButton(
                text = "取景框240dp",
                selected = !isDefaultSize,
                onClick = { state.setFrameSize(DpSize(240.dp, 240.dp)) }
            )
            TagButton(
                text = "垂直居中",
                selected = state.frameMarginTop == Dp.Unspecified,
                onClick = { state.setFrameMarginTop(Dp.Unspecified) }
            )
            TagButton(
                text = "顶距60dp",
                selected = state.frameMarginTop == 60.dp,
                onClick = { state.setFrameMarginTop(60.dp) }
            )
        }
    }
}

@Composable
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TagButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2196F3) else Color(0xFF424242),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier
            .height(32.dp)
            .padding(end = 8.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}

/**
 * 生成示例：演示 [SkyQRCodeImage] 的 content / size / logo / logoSize / logoCornerRadius，
 * 点击生成后才渲染二维码；并提供超限（> 1/3）打印警告并回退默认大小的演示。
 * 同时通过 [SkyBarcodeImage] 支持生成 CODE_128 / EAN_13 条形码，
 * 便于在另一屏幕显示后验证条形码识别。
 */
@Composable
private fun GenerateTab() {
    var content by remember { mutableStateOf("https://github.com/ZHLhenry/SkyWidgetCompose") }
    var size by remember { mutableStateOf(200.dp) }
    var genFormat by remember { mutableStateOf(GenFormat.QR_CODE) }
    var logoEnabled by remember { mutableStateOf(false) }
    // Logo 大小占二维码的比例，0 表示默认（二维码短边的 1/5）
    var logoRatio by remember { mutableFloatStateOf(0f) }
    var cornerRadius by remember { mutableStateOf(0.dp) }
    // 超限 Logo 生成结果（超限后回退默认大小）
    var oversizeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 是否已点击"生成"：点击前不显示图片；点击后图片随当前参数实时渲染，
    // 避免切换格式/尺寸/Logo 等参数后图片仍停留在旧状态
    var generated by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val density = LocalDensity.current
    // 中心 Logo 演示：将应用图标绘制为 Bitmap。
    // 注意：API 26+ 的 mipmap 为 adaptive-icon XML，BitmapFactory.decodeResource 无法解码，须用 Canvas 渲染
    val logoBitmap = remember {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.let { drawable ->
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
            createBitmap(w, h).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
            }
        }
    }
    // 按比例换算 Logo 边长（px 精确换算，保证 1/3 档位不越过上限）
    val sizePx = with(density) { size.roundToPx() }
    val logoSize = with(density) { (sizePx * logoRatio).toInt().toDp() }
    val isQr = genFormat == GenFormat.QR_CODE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("码内容") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 格式选择：二维码 / 条形码（EAN_13 要求 12~13 位数字，切换时自动填充示例值）
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "格式：", fontSize = 14.sp)
            listOf(
                "二维码" to GenFormat.QR_CODE,
                "CODE_128" to GenFormat.CODE_128,
                "EAN_13" to GenFormat.EAN_13
            ).forEach { (label, format) ->
                FilterChip(
                    selected = genFormat == format,
                    onClick = {
                        genFormat = format
                        if (format == GenFormat.EAN_13 && !content.all { it.isDigit() }) {
                            content = "6901234567892"
                        }
                    },
                    label = { Text(label) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 尺寸选择
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "尺寸：", fontSize = 14.sp)
            listOf(150.dp, 200.dp, 260.dp).forEach { option ->
                FilterChip(
                    selected = size == option,
                    onClick = { size = option },
                    label = { Text("${option.value.toInt()}dp") }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        if (isQr) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo 开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "中心 Logo：", fontSize = 14.sp)
                Switch(checked = logoEnabled, onCheckedChange = { logoEnabled = it })
            }

            if (logoEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // Logo 大小选择（上限为二维码的 1/3，超限打印警告并回退默认）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Logo 大小：", fontSize = 14.sp)
                    listOf("默认" to 0f, "1/5" to 1f / 5, "1/4" to 1f / 4, "1/3" to 1f / 3).forEach { (label, ratio) ->
                        FilterChip(
                            selected = logoRatio == ratio,
                            onClick = { logoRatio = ratio },
                            label = { Text(label) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Logo 圆角选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Logo 圆角：", fontSize = 14.sp)
                    listOf("无" to 0.dp, "8dp" to 8.dp, "16dp" to 16.dp).forEach { (label, radius) ->
                        FilterChip(
                            selected = cornerRadius == radius,
                            onClick = { cornerRadius = radius },
                            label = { Text(label) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { generated = true },
            enabled = content.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isQr) "生成二维码" else "生成条形码")
        }

        if (isQr) {
            Spacer(modifier = Modifier.height(8.dp))

            // 超限演示：Logo 超过二维码 1/3 时打印警告并回退默认大小，不崩溃
            Button(
                onClick = {
                    oversizeBitmap = SkyQRCode.createQRCode(
                        content.ifEmpty { "oversize" },
                        sizePx,
                        sizePx,
                        logoBitmap,
                        logoSize = (sizePx * 0.5).toInt()
                    )
                },
                enabled = logoBitmap != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "超限 Logo 生成（警告演示）")
            }
            oversizeBitmap?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "大小超限，已回退默认大小（警告已打印到日志）",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (generated) {
            if (isQr) {
                SkyQRCodeImage(
                    content = content,
                    size = size,
                    logo = if (logoEnabled) logoBitmap else null,
                    logoSize = if (logoEnabled) logoSize else 0.dp,
                    logoCornerRadius = if (logoEnabled) cornerRadius else 0.dp,
                    modifier = Modifier.background(Color.White)
                )
            } else {
                // 条形码：宽取 size、高取一半，白底渲染便于另一屏幕显示后扫码验证
                genFormat.barcodeFormat?.let {
                    SkyBarcodeImage(
                        content = content,
                        format = it,
                        width = size,
                        height = size / 2,
                        modifier = Modifier.background(Color.White)
                    )
                }
            }
        }
    }
}

/**
 * 解析示例：演示 [SkyQRCode.analyzeBitmap] 的路径与 Bitmap 两个重载。
 */
@Composable
private fun AnalyzeTab() {
    val context = LocalContext.current
    var path by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SkyQRCodeResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("图片路径") },
            placeholder = { Text("留空时点击解析将自动生成测试二维码") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // analyzeBitmap(path) 重载演示
        Button(
            onClick = {
                val targetPath = path.takeIf { it.isNotEmpty() } ?: generateTestQRCode(context)
                if (path.isEmpty()) path = targetPath
                result = SkyQRCode.analyzeBitmap(targetPath)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "解析文件路径 analyzeBitmap(path)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // createQRCode(content, width, height, logo) 与 analyzeBitmap(bitmap) 重载演示
        Button(
            onClick = {
                val bitmap = SkyQRCode.createQRCode("SkyWidgetCompose InMemory", 400, 400, null)
                result = SkyQRCode.analyzeBitmap(bitmap)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "解析内存 Bitmap analyzeBitmap(bitmap)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        result?.let { r ->
            Text(
                text = when (r) {
                    is SkyQRCodeResult.Success -> "结果：${r.text}（${r.format ?: "未知格式"}）"
                    is SkyQRCodeResult.Failure -> "解析失败"
                },
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 在应用私有目录生成一张测试二维码图片，用于演示本地图片解析能力。
 */
private fun generateTestQRCode(context: Context): String {
    val file = File(context.filesDir, "sky_qrcode_demo.png")
    if (!file.exists()) {
        SkyQRCode.createQRCode("SkyWidgetCompose Demo", 400).let { bitmap ->
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
    return file.absolutePath
}

/**
 * 生成页的格式选项：二维码走 [SkyQRCodeImage]，条形码走 [SkyBarcodeImage]。
 *
 * @property barcodeFormat 对应的库内条形码格式；二维码时为 null
 */
private enum class GenFormat(val barcodeFormat: SkyBarcodeFormat?) {
    QR_CODE(null),
    CODE_128(SkyBarcodeFormat.CODE_128),
    EAN_13(SkyBarcodeFormat.EAN_13)
}
