package com.sky.widget.qrcode.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.Result
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
     * @param path 图片文件路径
     * @return 解析结果，失败时返回 null
     */
    fun decodeBitmap(path: String?): Result? {
        if (path.isNullOrEmpty()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            inJustDecodeBounds = false
            inSampleSize = (outHeight / 400).coerceAtLeast(1)
        }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        return decodeBitmap(bitmap)
    }

    /**
     * 解析 [Bitmap] 中的二维码。
     *
     * @param bitmap 待解析图片
     * @return 解析结果，失败时返回 null
     */
    fun decodeBitmap(bitmap: Bitmap?): Result? {
        bitmap ?: return null

        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to DecodeFormatResolver.ALL_FORMATS,
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name()
        )
        val reader = MultiFormatReader().apply { setHints(hints) }
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(BitmapLuminanceSource(bitmap))))
        } catch (e: ReaderException) {
            null
        } finally {
            reader.reset()
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
