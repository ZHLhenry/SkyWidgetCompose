package com.sky.widget.qrcode.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

/**
 * 二维码解码器。
 *
 * 负责解析本地图片中的二维码，以及将 [Bitmap] 转为 [LuminanceSource] 供 ZXing 使用。
 */
internal object SkyQRCodeDecoder {

    /**
     * 解析本地图片中的二维码。
     *
     * 高分辨率照片按长边约 1600px 采样后解码，失败时逐步降低采样率重试，
     * 避免小尺寸码因过度采样丢失细节导致无法识别。
     *
     * @param path 图片文件路径
     * @return 解析结果，失败时返回 null
     */
    fun decodeBitmap(path: String?): Result? {
        if (path.isNullOrEmpty()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val maxDimension = maxOf(options.outWidth, options.outHeight)
        // 初始采样：长边压到约 1600px，兼顾解码耗时与细节保留
        var sample = (maxDimension / 1600).coerceAtLeast(1)
        // 采样下限：长边不超过 4000px，防止超大图全分辨率解码引发 OOM
        val minSample = (maxDimension / 4000).coerceAtLeast(1)
        options.inJustDecodeBounds = false

        while (true) {
            options.inSampleSize = sample
            val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
            val result = decodeBitmap(bitmap)
            if (result != null || sample <= minSample) return result
            sample = (sample / 2).coerceAtLeast(minSample)
        }
    }

    /**
     * 解析 [Bitmap] 中的二维码。
     *
     * @param bitmap 待解析图片
     * @return 解析结果，失败时返回 null
     */
    fun decodeBitmap(bitmap: Bitmap?): Result? {
        bitmap ?: return null

        val (twoD, oneD) = DecodeFormatResolver.splitByDimension(DecodeFormatResolver.ALL_FORMATS)
        val twoDReader = twoD.takeIf { it.isNotEmpty() }?.let { buildReader(it) }
        val oneDReader = oneD.takeIf { it.isNotEmpty() }?.let { buildReader(it) }
        val source = BitmapLuminanceSource(bitmap)
        // 与实时扫描一致：先 2D 后 1D（避免二维码纹理被 1D 解码器误报），
        // 每组内重试链为 Hybrid → GlobalHistogram → 亮度取反（反色码）
        return try {
            twoDReader?.let { decodeWithRetry(it, source) }
                ?: oneDReader?.let { decodeWithRetry(it, source) }
        } finally {
            twoDReader?.reset()
            oneDReader?.reset()
        }
    }

    private fun buildReader(formats: List<BarcodeFormat>) = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to formats,
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name()
            )
        )
    }

    private fun decodeWithRetry(reader: MultiFormatReader, source: LuminanceSource): Result? {
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
     * 基于 [Bitmap] 的亮度源。
     *
     * 取每个像素值的蓝色（低 8 位）部分作为亮度辨析内容。
     */
    private class BitmapLuminanceSource(bitmap: Bitmap) : LuminanceSource(bitmap.width, bitmap.height) {

        private val bitmapPixels: ByteArray = ByteArray(bitmap.width * bitmap.height).also { pixels ->
            val data = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(data, 0, width, 0, 0, width, height)
            data.forEachIndexed { index, pixel ->
                pixels[index] = pixel.toByte()
            }
        }

        override fun getMatrix(): ByteArray = bitmapPixels

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val target = if (row == null || row.size < width) ByteArray(width) else row
            bitmapPixels.copyInto(target, destinationOffset = 0, startIndex = y * width, endIndex = y * width + width)
            return target
        }
    }
}
