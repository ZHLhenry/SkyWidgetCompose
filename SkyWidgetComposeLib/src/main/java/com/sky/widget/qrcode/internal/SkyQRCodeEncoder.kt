package com.sky.widget.qrcode.internal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sky.widget.qrcode.SkyBarcodeFormat
import kotlin.math.min

/**
 * 码图编码器。
 *
 * 负责将文本内容编码为二维码 / 条形码 [Bitmap]，二维码支持在中心嵌入 Logo。
 */
internal object SkyQRCodeEncoder {

    private const val PIXEL_BLACK = 0xFF000000.toInt()
    private const val PIXEL_WHITE = 0xFFFFFFFF.toInt()
    private const val TAG = "SkyQRCodeEncoder"

    /**
     * 生成正方形二维码图片。
     *
     * @param content 二维码内容，不能为空
     * @param size 生成图片的宽高，单位 px
     * @return 二维码 Bitmap（黑色码点，白色背景）
     * @throws WriterException 编码失败时抛出
     * @throws IllegalArgumentException 内容为空时抛出
     */
    @Throws(WriterException::class)
    fun createQRCode(content: String, size: Int): Bitmap {
        require(content.isNotEmpty()) { "二维码内容不能为空" }

        val hints = mapOf(EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
        val matrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix.get(x, y)) PIXEL_BLACK else PIXEL_WHITE
            }
        }
        return createBitmap(width, height).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    /**
     * 生成二维码图片，支持在中心嵌入 Logo。
     *
     * @param content 二维码内容
     * @param width 生成图片宽度，单位 px
     * @param height 生成图片高度，单位 px
     * @param logo 中心 Logo，可为 null
     * @param logoSize Logo 目标边长，单位 px；0 表示默认取二维码短边的 1/5；
     * 超过二维码短边的 1/3 时打印警告并回退默认大小，不会抛出异常
     * @param logoCornerRadius Logo 圆角半径，单位 px；0 表示不裁剪圆角
     * @return 生成的二维码 Bitmap，内容为空或生成失败时返回 null
     */
    fun createQRCode(
        content: String,
        width: Int,
        height: Int,
        logo: Bitmap?,
        logoSize: Int = 0,
        logoCornerRadius: Float = 0f
    ): Bitmap? {
        if (content.isEmpty()) return null

        return try {
            val scaleLogo = getScaleLogo(logo, width, height, logoSize, logoCornerRadius)
            var offsetX = width / 2
            var offsetY = height / 2
            var scaleWidth = 0
            var scaleHeight = 0
            if (scaleLogo != null) {
                scaleWidth = scaleLogo.width
                scaleHeight = scaleLogo.height
                offsetX = (width - scaleWidth) / 2
                offsetY = (height - scaleHeight) / 2
            }

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 0
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val qrPixel = if (bitMatrix.get(x, y)) PIXEL_BLACK else PIXEL_WHITE
                    pixels[y * width + x] = if (scaleLogo != null &&
                        x in offsetX until offsetX + scaleWidth &&
                        y in offsetY until offsetY + scaleHeight
                    ) {
                        val logoPixel = scaleLogo[x - offsetX, y - offsetY]
                        if (logoPixel != 0) logoPixel else qrPixel
                    } else {
                        qrPixel
                    }
                }
            }
            createBitmap(width, height).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: WriterException) {
            Log.e(TAG, "QR code encode failed: content=$content", e)
            null
        }
    }

    /**
     * 生成条形码图片（黑条白底）。
     *
     * @param content 条形码内容
     * @param format 条形码格式
     * @param width 生成图片宽度，单位 px
     * @param height 生成图片高度，单位 px
     * @return 生成的条形码 Bitmap；内容为空、不符合格式要求（如 EAN_13 位数错误）
     * 或编码失败时返回 null
     */
    fun createBarcode(content: String, format: SkyBarcodeFormat, width: Int, height: Int): Bitmap? {
        if (content.isEmpty()) return null

        return try {
            val hints = mapOf(EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
            val matrix = MultiFormatWriter().encode(content, format.toZxing(), width, height, hints)

            val w = matrix.width
            val h = matrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    pixels[y * w + x] = if (matrix.get(x, y)) PIXEL_BLACK else PIXEL_WHITE
                }
            }
            createBitmap(w, h).apply {
                setPixels(pixels, 0, w, 0, 0, w, h)
            }
        } catch (e: WriterException) {
            Log.e(TAG, "Barcode encode failed: format=$format, content=$content", e)
            null
        } catch (e: IllegalArgumentException) {
            // 内容不符合条码格式要求（如 EAN_13 位数错误）
            Log.e(TAG, "Barcode content invalid: format=$format, content=$content", e)
            null
        }
    }

    /**
     * 将 Logo 缩放至目标尺寸（默认二维码短边的 1/5），并按需裁剪圆角。
     *
     * Logo 边长上限为二维码短边的 1/3；超过时打印警告并回退默认大小，避免消费者程序崩溃。
     */
    private fun getScaleLogo(
        logo: Bitmap?,
        width: Int,
        height: Int,
        logoSize: Int,
        cornerRadius: Float
    ): Bitmap? {
        logo ?: return null
        val defaultSize = min(width, height) / 5
        val maxAllowed = min(width, height) / 3
        val target = when {
            logoSize <= 0 -> defaultSize
            logoSize > maxAllowed -> {
                Log.w(
                    TAG,
                    "Logo 大小（${logoSize}px）超过二维码大小的 1/3（${maxAllowed}px），已回退默认大小（${defaultSize}px）"
                )
                defaultSize
            }
            else -> logoSize
        }
        val scaleFactor = min(target * 1.0f / logo.width, target * 1.0f / logo.height)
        val matrix = Matrix().apply { postScale(scaleFactor, scaleFactor) }
        val scaled = Bitmap.createBitmap(logo, 0, 0, logo.width, logo.height, matrix, true)
        return if (cornerRadius > 0f) roundCorners(scaled, cornerRadius) else scaled
    }

    /**
     * 通过 BitmapShader 将 [src] 裁剪为圆角 Bitmap。
     */
    private fun roundCorners(src: Bitmap, radius: Float): Bitmap {
        val output = createBitmap(src.width, src.height)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()),
                radius,
                radius,
                Path.Direction.CCW
            )
        }
        canvas.drawPath(path, paint)
        return output
    }
}

/**
 * 将公开格式枚举映射为底层编码库格式。
 */
private fun SkyBarcodeFormat.toZxing(): BarcodeFormat = when (this) {
    SkyBarcodeFormat.CODE_39 -> BarcodeFormat.CODE_39
    SkyBarcodeFormat.CODE_93 -> BarcodeFormat.CODE_93
    SkyBarcodeFormat.CODE_128 -> BarcodeFormat.CODE_128
    SkyBarcodeFormat.EAN_8 -> BarcodeFormat.EAN_8
    SkyBarcodeFormat.EAN_13 -> BarcodeFormat.EAN_13
    SkyBarcodeFormat.ITF -> BarcodeFormat.ITF
    SkyBarcodeFormat.CODABAR -> BarcodeFormat.CODABAR
    SkyBarcodeFormat.UPC_A -> BarcodeFormat.UPC_A
    SkyBarcodeFormat.UPC_E -> BarcodeFormat.UPC_E
}
