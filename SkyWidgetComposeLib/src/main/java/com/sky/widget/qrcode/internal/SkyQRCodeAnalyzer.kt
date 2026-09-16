package com.sky.widget.qrcode.internal

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.sky.widget.qrcode.SkyQRCodeState
import kotlin.math.roundToInt

/**
 * CameraX 图像分析器。
 *
 * 接收相机预览帧，提取亮度数据后经 ZXing 解码，并通过 [onResult] 回调识别结果。
 *
 * @param formats 目标条码格式集合
 * @param frameBounds 取景框在容器中的相对边界（0..1），返回 null 或全屏时解码整个画面
 * @param onResult 识别成功回调
 * @param onEmpty 单帧未识别到条码回调，用于外部做"码离开视野"等去重重置判断
 */
internal class SkyQRCodeAnalyzer(
    formats: List<BarcodeFormat>,
    private val frameBounds: () -> SkyQRCodeState.FrameBounds?,
    private val onResult: (Result) -> Unit,
    private val onEmpty: () -> Unit = {}
) : ImageAnalysis.Analyzer {

    // 2D / 1D 分组解码：2D 矩阵码误报率极低，优先解码可拦截真实二维码；
    // 1D 格式（尤其 UPC_E/EAN_8 等短码）校验位弱，二维码的密集纹理容易被误判为 1D 条码
    private val twoDReader: MultiFormatReader?
    private val oneDReader: MultiFormatReader?

    init {
        val (twoD, oneD) = DecodeFormatResolver.splitByDimension(formats)
        twoDReader = twoD.takeIf { it.isNotEmpty() }?.let { buildReader(it) }
        oneDReader = oneD.takeIf { it.isNotEmpty() }?.let { buildReader(it) }
    }

    private fun buildReader(formats: List<BarcodeFormat>) = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to formats,
                DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
                // 条形码（1D）识别率关键：启用更彻底的搜索策略
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    // 1D 误报防护：要求同一 1D 结果连续命中 2 帧才上报，过滤单帧随机误报；2D 结果立即上报
    private var pendingOneDText: String? = null
    private var pendingOneDHits = 0

    @Volatile
    private var isAnalyzing = false

    override fun analyze(image: ImageProxy) {
        if (isAnalyzing) {
            image.close()
            return
        }
        isAnalyzing = true

        try {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val rotation = image.imageInfo.rotationDegrees
            val (width, height, rotatedData) = rotateYuvData(data, image.width, image.height, rotation)

            val bounds = frameBounds()
            val source = if (bounds != null &&
                bounds.width > 0f && bounds.height > 0f &&
                bounds.containerWidth > 0 && bounds.containerHeight > 0
            ) {
                // bounds 为取景框在容器坐标系的像素矩形；PreviewView FILL_CENTER 将相机帧
                // 按 max(cw/fw, ch/fh) 缩放并居中裁剪显示，此处反映射回相机帧坐标系裁剪解码，
                // 保证"只识别扫描框内"的码
                val scale = maxOf(
                    bounds.containerWidth / width.toFloat(),
                    bounds.containerHeight / height.toFloat()
                )
                val offX = (bounds.containerWidth - width * scale) / 2f
                val offY = (bounds.containerHeight - height * scale) / 2f
                val left = ((bounds.left - offX) / scale).roundToInt().coerceIn(0, width)
                val top = ((bounds.top - offY) / scale).roundToInt().coerceIn(0, height)
                val right = ((bounds.right - offX) / scale).roundToInt().coerceIn(left, width)
                val bottom = ((bounds.bottom - offY) / scale).roundToInt().coerceIn(top, height)
                PlanarYUVLuminanceSource(
                    rotatedData,
                    width,
                    height,
                    left,
                    top,
                    right - left,
                    bottom - top
                )
            } else {
                PlanarYUVLuminanceSource(rotatedData, width, height, 0, 0, width, height)
            }

            // 先解 2D 矩阵码：命中则立即上报，避免真实二维码落入 1D 误报
            val twoDResult = twoDReader?.let { decodeWithRetry(it, source) }
            if (twoDResult != null) {
                pendingOneDText = null
                pendingOneDHits = 0
                onResult(twoDResult)
            } else if (oneDReader != null) {
                val oneDResult = decodeWithRetry(oneDReader, source)
                if (oneDResult == null) {
                    // 空帧中断连续性，清除待确认的 1D 候选
                    pendingOneDText = null
                    pendingOneDHits = 0
                    onEmpty()
                } else if (confirmOneD(oneDResult.text)) {
                    onResult(oneDResult)
                } else {
                    // 未达连续确认帧数，继续下一帧
                    onEmpty()
                }
            } else {
                onEmpty()
            }
            twoDReader?.reset()
            oneDReader?.reset()
        } finally {
            image.close()
            isAnalyzing = false
        }
    }

    /**
     * HybridBinarizer 适合二维码；条形码（1D）更适合 GlobalHistogramBinarizer，
     * 因此首选 Hybrid，失败后重试一次 GlobalHistogram，兼顾两类码的识别率；
     * 仍失败则以亮度取反重试：ZXing 仅支持深色模块+浅色底的标准码，
     * 反色码（浅色模块印在深色背景上，常见于深色产品包装）必须取反后才能解码。
     */
    private fun decodeWithRetry(reader: MultiFormatReader, source: PlanarYUVLuminanceSource): Result? {
        val invertedSource = lazy { InvertedLuminanceSource(source) }
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (e: ReaderException) {
            try {
                reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
            } catch (e2: ReaderException) {
                try {
                    reader.decodeWithState(BinaryBitmap(HybridBinarizer(invertedSource.value)))
                } catch (e3: ReaderException) {
                    try {
                        reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(invertedSource.value)))
                    } catch (e4: ReaderException) {
                        null
                    }
                }
            }
        }
    }

    /**
     * 1D 结果连续帧确认：同一文本连续命中 [ONE_D_CONFIRM_FRAMES] 帧才返回 true。
     */
    private fun confirmOneD(text: String): Boolean {
        if (text == pendingOneDText) {
            pendingOneDHits++
        } else {
            pendingOneDText = text
            pendingOneDHits = 1
        }
        return if (pendingOneDHits >= ONE_D_CONFIRM_FRAMES) {
            pendingOneDText = null
            pendingOneDHits = 0
            true
        } else {
            false
        }
    }

    /**
     * 将 YUV 亮度数据按相机旋转角度进行旋转，使数据方向与屏幕显示方向一致。
     */
    private fun rotateYuvData(
        data: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ): Triple<Int, Int, ByteArray> {
        return when (rotation) {
            90 -> {
                val rotated = ByteArray(data.size)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + height - y - 1] = data[x + y * width]
                    }
                }
                Triple(height, width, rotated)
            }

            180 -> {
                val rotated = ByteArray(data.size)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(height - y - 1) * width + (width - x - 1)] = data[y * width + x]
                    }
                }
                Triple(width, height, rotated)
            }

            270 -> {
                val rotated = ByteArray(data.size)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(width - x - 1) * height + y] = data[y * width + x]
                    }
                }
                Triple(height, width, rotated)
            }

            else -> Triple(width, height, data)
        }
    }
}

/** 1D 结果上报所需的连续命中帧数。 */
private const val ONE_D_CONFIRM_FRAMES = 2
