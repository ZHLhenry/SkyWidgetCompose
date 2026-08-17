package com.sky.widget.qrcode.internal

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.LuminanceSource

/**
 * 基于 YUV420 预览数据的亮度源。
 *
 * 相机预览帧通常为 YUV 格式，其 Y 分量（亮度）位于数据头部，
 * 此处按裁剪区域直接读取 Y 分量用于条码识别，避免格式转换带来的额外拷贝。
 *
 * @param yuvData YUV 预览帧数据
 * @param dataWidth 预览帧宽度
 * @param dataHeight 预览帧高度
 * @param left 裁剪区域左边界
 * @param top 裁剪区域上边界
 * @param width 裁剪区域宽度
 * @param height 裁剪区域高度
 */
internal class PlanarYUVLuminanceSource(
    private val yuvData: ByteArray,
    val dataWidth: Int,
    val dataHeight: Int,
    private val left: Int,
    private val top: Int,
    width: Int,
    height: Int
) : LuminanceSource(width, height) {

    init {
        require(left + width <= dataWidth && top + height <= dataHeight) {
            "Crop rectangle does not fit within image data."
        }
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        require(y in 0 until height) { "Requested row is outside the image: $y" }
        val target = if (row == null || row.size < width) ByteArray(width) else row
        val offset = (y + top) * dataWidth + left
        yuvData.copyInto(target, destinationOffset = 0, startIndex = offset, endIndex = offset + width)
        return target
    }

    override fun getMatrix(): ByteArray {
        if (width == dataWidth && height == dataHeight) {
            return yuvData
        }
        val area = width * height
        val matrix = ByteArray(area)
        var inputOffset = top * dataWidth + left
        if (width == dataWidth) {
            yuvData.copyInto(matrix, destinationOffset = 0, startIndex = inputOffset, endIndex = inputOffset + area)
            return matrix
        }
        for (y in 0 until height) {
            val outputOffset = y * width
            yuvData.copyInto(matrix, destinationOffset = outputOffset, startIndex = inputOffset, endIndex = inputOffset + width)
            inputOffset += dataWidth
        }
        return matrix
    }

    override fun isCropSupported(): Boolean = true

    /**
     * 将裁剪区域的灰度数据渲染为 Bitmap，用于识别成功后的结果回显。
     */
    fun renderCroppedGreyscaleBitmap(): Bitmap {
        val pixels = IntArray(width * height)
        var inputOffset = top * dataWidth + left
        for (y in 0 until height) {
            val outputOffset = y * width
            for (x in 0 until width) {
                val grey = yuvData[inputOffset + x].toInt() and 0xFF
                pixels[outputOffset + x] = -0x1000000 or (grey * 0x00010101)
            }
            inputOffset += dataWidth
        }
        return createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
