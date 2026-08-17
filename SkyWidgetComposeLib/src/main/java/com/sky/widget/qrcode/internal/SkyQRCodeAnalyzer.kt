package com.sky.widget.qrcode.internal

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
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

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to formats,
                DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
                // 条形码（1D）识别率关键：启用更彻底的搜索策略
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

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

            // HybridBinarizer 适合二维码；条形码（1D）更适合 GlobalHistogramBinarizer，
            // 因此首选 Hybrid，失败后重试一次 GlobalHistogram，兼顾两类码的识别率
            val result: Result? = try {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            } catch (e: ReaderException) {
                try {
                    reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
                } catch (e2: ReaderException) {
                    null
                }
            }
            if (result != null) {
                onResult(result)
            } else {
                // 未识别到条码，继续下一帧
                onEmpty()
            }
            reader.reset()
        } finally {
            image.close()
            isAnalyzing = false
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
