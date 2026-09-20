package com.sky.widget.preview

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.sky.widget.iconfont.SkyIconFont
import com.sky.widget.iface.SkyIconType
import com.sky.widget.stateLayout.SkyErrorWidget
import com.sky.widget.stateLayout.SkyLoadingWidget
import com.sky.widget.viewpage.SkyViewPage
import com.sky.widget.viewpage.SkyViewPageState
import com.sky.widget.viewpage.rememberSkyViewPageState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 运行时探测宿主是否引入了 coil-compose。
 * 注意：探测逻辑不引用任何 Coil 类型，避免在缺失时先触发 NoClassDefFoundError。
 */
private val isCoilComposeAvailable: Boolean by lazy {
    runCatching {
        Class.forName("coil.ImageLoader")
    }.isSuccess
}

/**
 * 运行时探测宿主是否引入了 coil-gif（GIF/WebP 动图解码器所在工件）。
 * 探测逻辑不引用任何 coil-gif 类型，避免在缺失时先触发 NoClassDefFoundError。
 */
private val isCoilGifAvailable: Boolean by lazy {
    runCatching {
        Class.forName("coil.decode.ImageDecoderDecoder")
    }.isSuccess
}

/**
 * [SkyPreView] 的缩放状态。
 *
 * @param minScale 最小缩放倍数（双指捏合的下限）
 * @param maxScale 最大缩放倍数（双指张开的上限）
 * @param doubleTapScale 双击放大到的目标倍数（自动收敛到 [minScale, maxScale] 区间）
 */
@Stable
class SkyPreViewState(
    val minScale: Float = 1f,
    val maxScale: Float = 5f,
    val doubleTapScale: Float = 2.5f
) {
    init {
        require(minScale > 0f) { "minScale 必须大于 0" }
        require(maxScale >= minScale) { "maxScale 必须大于等于 minScale" }
    }

    /** 当前缩放倍数，只读，手势驱动 */
    var scale by mutableFloatStateOf(1f)
        private set

    /** 当前平移偏移（缩放后的像素位移），只读，手势驱动 */
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** 容器尺寸，由组件内部 onSizeChanged 写入 */
    internal var containerSize by mutableStateOf(IntSize.Zero)

    /** 立即复位到初始状态（scale=1、offset=Zero）。公开 API，供消费者调用 */
    @Suppress("unused")
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** 双指变换：以双指中心为锚点缩放 + 跟手平移，结果收敛到合法区间 */
    internal fun onTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val oldScale = scale
        val newScale = (oldScale * zoom).coerceIn(minScale, maxScale)
        val pivot = Offset(containerSize.width / 2f, containerSize.height / 2f)
        // 保持双指中心下的内容点不动：offset' = c - pivot - (c - pivot - offset) * (scale'/scale)
        var newOffset = if (oldScale > 0f) {
            centroid - pivot - (centroid - pivot - offset) * (newScale / oldScale)
        } else {
            offset
        }
        newOffset += pan
        scale = newScale
        offset = clampOffset(newOffset, newScale)
    }

    /** 计算双击的目标状态：已放大则复位，未放大则以点击位置为锚点放大到 doubleTapScale */
    internal fun doubleTapTarget(position: Offset): Pair<Float, Offset> {
        if (scale > 1f + 0.01f) return 1f to Offset.Zero
        val targetScale = doubleTapScale.coerceIn(minScale, maxScale)
        val pivot = Offset(containerSize.width / 2f, containerSize.height / 2f)
        val targetOffset = position - pivot - (position - pivot) * targetScale
        return targetScale to clampOffset(targetOffset, targetScale)
    }

    /** 动画过渡到目标缩放与偏移 */
    internal suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        Animatable(0f).animateTo(1f, tween(250)) {
            scale = startScale + (targetScale - startScale) * value
            offset = startOffset + (targetOffset - startOffset) * value
        }
    }

    /** 平移量钳制：缩放后内容不超出容器边缘留白的最大允许位移 */
    private fun clampOffset(value: Offset, targetScale: Float): Offset {
        val maxX = ((containerSize.width * targetScale - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((containerSize.height * targetScale - containerSize.height) / 2f).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
}

/** 创建并记住 [SkyPreViewState]。 */
@Composable
fun rememberSkyPreViewState(
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapScale: Float = 2.5f
): SkyPreViewState = remember(minScale, maxScale, doubleTapScale) {
    SkyPreViewState(minScale, maxScale, doubleTapScale)
}

/**
 * 图片预览（基于 Coil + [SkyViewPage] 翻页），黑底全屏，左右滑动切换图片。
 *
 * 单图即单元素列表，无需区分单图/多图：[models] 支持 Coil 的全部图片源混用，
 * 网络 URL（String）、本地资源（`R.drawable.xxx`）、[android.net.Uri]、
 * [java.io.File]、[android.graphics.Bitmap] 等；GIF/WebP 动图需消费者额外引入 `coil-gif`，
 * 组件检测到 coil-gif 后会自动注册动图解码器，无需手动配置 ImageLoader。
 *
 * 每页独立的缩放状态，支持双指捏合缩放、双指平移、双击放大/复位；
 * 未放大（scale=1）时单指横滑切换图片，放大后横滑平移图片（复位后方可继续翻页）。
 *
 * 内置底部 `当前页/总页数` 指示器（可经 [indicator] 插槽定制、[showIndicator] 关闭）与
 * 右上角关闭按钮（传入 [onClose] 即显示）；单击图片默认无行为（避免误触退出），可经 [onPageClick] 定制。
 *
 * 加载中与加载失败默认复用库内 [SkyLoadingWidget] / [SkyErrorWidget]，可经 [loading] / [error] 插槽定制。
 *
 * 注意：Coil 在库中为 compileOnly 依赖，不会传递给消费者。
 * 使用前需在宿主模块自行导包：
 * `implementation("io.coil-kt:coil-compose:版本号")`，
 * GIF/WebP 动图还需 `implementation("io.coil-kt:coil-gif:版本号")`，
 * 否则本组件会在运行时抛出带导包提示的 [IllegalStateException]。
 *
 * @param models 图片源列表（Coil model），单图传单元素列表
 * @param modifier 外层修饰符
 * @param initialIndex 初始展示页索引
 * @param pagerState 翻页状态，可代码翻页/监听索引，默认 [rememberSkyViewPageState]
 * @param minScale 每页最小缩放倍数
 * @param maxScale 每页最大缩放倍数
 * @param doubleTapScale 每页双击放大目标倍数
 * @param contentScale 图片初始适配方式，默认 [ContentScale.Fit]
 * @param backgroundColor 预览背景色，默认黑色
 * @param showIndicator 是否显示页码指示器
 * @param indicator 指示器插槽，入参为（当前索引, 总页数）；null 时使用默认 `1/4` 文本样式
 * @param onClose 关闭回调，非 null 时右上角显示关闭按钮
 * @param closeIcon 关闭按钮图标，见 [SkyIconType]（[Painter] 图片或 iconfont 名称）；
 * null 时使用默认 Canvas 自绘 X
 * @param closeIconTint 关闭按钮图标着色，默认白色；传 null 不着色（保留图片原色，iconfont 回落白色）
 * @param onPageClick 单击图片回调（入参为页索引），null 时单击无行为
 * @param onPageChanged 翻页回调（入参为新页索引）
 * @param onLongClick 长按图片回调（入参为页索引）
 * @param loading 加载中占位插槽，默认居中 [SkyLoadingWidget]
 * @param error 加载失败占位插槽，默认 [SkyErrorWidget]
 * @param imageLoader 自定义 Coil [ImageLoader]（如自定义缓存/网络配置的实例）；
 * null 时使用 Coil 单例（`Coil.imageLoader(context)`），且检测到 coil-gif 时自动附带动图解码器
 */
@Composable
fun SkyPreView(
    models: List<Any?>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    pagerState: SkyViewPageState = rememberSkyViewPageState(),
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapScale: Float = 2.5f,
    contentScale: ContentScale = ContentScale.Fit,
    backgroundColor: Color = Color.Black,
    showIndicator: Boolean = true,
    indicator: (@Composable (index: Int, count: Int) -> Unit)? = null,
    closeIcon: SkyIconType? = null,
    closeIconTint: Color? = Color.White,
    onClose: (() -> Unit)? = null,
    onPageClick: ((Int) -> Unit)? = null,
    onPageChanged: ((Int) -> Unit)? = null,
    onLongClick: ((Int) -> Unit)? = null,
    loading: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
    imageLoader: ImageLoader? = null
) {
    checkCoilAvailable()
    if (models.isEmpty()) return

    // 初始页定位（无动画）
    LaunchedEffect(models, initialIndex) {
        if (initialIndex in models.indices) pagerState.setPageIndex(initialIndex)
    }
    // 翻页回调
    LaunchedEffect(pagerState, onPageChanged) {
        pagerState.createCurrSelectIndexFlow().collect { onPageChanged?.invoke(it) }
    }

    val currentIndex = pagerState.getCurrSelectIndexState().value

    Box(modifier = modifier.background(backgroundColor)) {
        SkyViewPage(
            pageCount = models.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) {
            val pageIndex = index
            val pageState = rememberSkyPreViewState(minScale, maxScale, doubleTapScale)
            SkyPreViewCore(
                model = models[pageIndex],
                modifier = Modifier.fillMaxSize(),
                state = pageState,
                contentScale = contentScale,
                onClick = onPageClick?.let { callback -> { callback(pageIndex) } },
                onLongClick = onLongClick?.let { callback -> { callback(pageIndex) } },
                loading = loading,
                error = error,
                imageLoader = imageLoader
            )
        }

        // 底部页码指示器
        if (showIndicator) {
            SkyPreViewPageIndicator(
                currentIndex = currentIndex,
                count = models.size,
                indicator = indicator
            )
        }

        // 右上角关闭按钮
        if (onClose != null) {
            SkyPreViewCloseButton(
                onClick = onClose,
                icon = closeIcon,
                iconTint = closeIconTint,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 显式注册 GIF/WebP 动图解码器（coil-gif 经 ServiceLoader 自动注册可能被打包/R8 剥离）。
 * 仅在 [isCoilGifAvailable] 为 true 时调用：JVM 在方法首次执行时才做类验证，
 * 未导包时不会触发 NoClassDefFoundError。
 */
private fun ComponentRegistry.Builder.addGifDecoders() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        add(ImageDecoderDecoder.Factory())
    } else {
        add(GifDecoder.Factory())
    }
}

/** Coil 运行时探测，未导包抛出带导包提示的异常。 */
private fun checkCoilAvailable() {
    check(isCoilComposeAvailable) {
        "SkyPreView 依赖 Coil。" +
            "请消费者自行添加依赖：implementation(\"io.coil-kt:coil-compose:版本号\")；" +
            "GIF/WebP 动图还需 implementation(\"io.coil-kt:coil-gif:版本号\")"
    }
}

/**
 * 可缩放图片核心（[SkyPreView] 每一页的实现）。
 *
 * 手势条件消费：仅放大状态（scale>1）或双指缩放才消费拖拽，
 * scale=1 的单指横滑不消费，事件传递给外层 [SkyViewPage] 完成翻页。
 */
@Composable
private fun SkyPreViewCore(
    model: Any?,
    modifier: Modifier,
    state: SkyPreViewState,
    contentScale: ContentScale,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    loading: (@Composable () -> Unit)?,
    error: (@Composable () -> Unit)?,
    imageLoader: ImageLoader?
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val coilImageLoader = remember(imageLoader, context) {
        val singleton = Coil.imageLoader(context)
        imageLoader ?: if (isCoilGifAvailable) {
            // coil-gif 存在时，基于宿主 ImageLoader 派生一个带 GIF/WebP 解码器的实例
            // （newBuilder 继承其组件与缓存配置，消费者无需手动配置动图支持）。
            singleton.newBuilder()
                .components { addGifDecoders() }
                .build()
        } else {
            singleton
        }
    }
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state) {
                detectSkyTransformGestures(
                    shouldConsumePan = { state.scale > 1f },
                    onGesture = { centroid, pan, zoom -> state.onTransform(centroid, pan, zoom) }
                )
            }
            .pointerInput(state, onClick, onLongClick) {
                detectTapGestures(
                    onTap = { onClick?.invoke() },
                    onLongPress = { onLongClick?.invoke() },
                    onDoubleTap = { position ->
                        val (targetScale, targetOffset) = state.doubleTapTarget(position)
                        scope.launch { state.animateTo(targetScale, targetOffset) }
                    }
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                },
            contentScale = contentScale,
            imageLoader = coilImageLoader,
            loading = { loading?.invoke() ?: SkyLoadingWidget() },
            error = { error?.invoke() ?: SkyErrorWidget(message = "图片加载失败") }
        )
    }
}

/**
 * 变换手势检测（改自官方 detectTransformGestures，支持条件消费）。
 *
 * 与官方实现差异：平移事件仅在 [shouldConsumePan] 为 true 时才消费并参与手势，
 * 为 false 时平移不消费、不透传给 [onGesture]，让外层可滚容器（如 [SkyViewPage]）接管；
 * 双指缩放始终消费。
 */
private suspend fun PointerInputScope.detectSkyTransformGestures(
    shouldConsumePan: () -> Boolean,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()
                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val consumePan = shouldConsumePan()
                    if (zoomChange != 1f || (consumePan && panChange != Offset.Zero)) {
                        onGesture(centroid, if (consumePan) panChange else Offset.Zero, zoomChange)
                    }
                    if (zoomChange != 1f || consumePan) {
                        event.changes.fastForEach { if (it.positionChanged()) it.consume() }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}

/** 底部页码指示器（单图/多图共用），默认 `当前页/总页数` 白色文本，可经 [indicator] 插槽定制。 */
@Composable
private fun BoxScope.SkyPreViewPageIndicator(
    currentIndex: Int,
    count: Int,
    indicator: (@Composable (index: Int, count: Int) -> Unit)?
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        if (indicator != null) {
            indicator(currentIndex, count)
        } else {
            Text(
                text = "${currentIndex + 1}/$count",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 右上角关闭按钮（半透明圆形底 + 图标）。
 *
 * [icon] 为 null 时使用默认 Canvas 自绘 X（库模块无 material-icons 依赖）；
 * 非 null 时按 [SkyIconType] 类型分派渲染（[Image] / [SkyIconFont]）。
 */
@Composable
private fun SkyPreViewCloseButton(
    onClick: () -> Unit,
    icon: SkyIconType?,
    iconTint: Color?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 半透明圆形底，保证在浅色图片上可见
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(color = Color.White.copy(alpha = 0.2f))
        }
        when (icon) {
            null -> SkyPreViewDefaultCloseIcon()
            is SkyIconType.PainterIcon -> Image(
                painter = icon.painter,
                contentDescription = "关闭",
                modifier = Modifier.size(18.dp),
                colorFilter = iconTint?.let { ColorFilter.tint(it) }
            )

            is SkyIconType.FontIcon -> SkyIconFont(
                iconName = icon.iconName,
                modifier = Modifier.size(18.dp),
                tint = iconTint ?: Color.White,
                fontSize = with(LocalDensity.current) { 18.dp.toSp() },
                fontName = icon.fontName
            )
        }
    }
}

/** 默认关闭图标：Canvas 自绘 X。 */
@Composable
private fun SkyPreViewDefaultCloseIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val padding = size.minDimension * 0.3f
        val strokeWidth = size.minDimension * 0.07f
        drawLine(
            color = Color.White,
            start = Offset(padding, padding),
            end = Offset(size.width - padding, size.height - padding),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width - padding, padding),
            end = Offset(padding, size.height - padding),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
