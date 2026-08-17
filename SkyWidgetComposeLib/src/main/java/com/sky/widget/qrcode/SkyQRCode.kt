package com.sky.widget.qrcode

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sky.widget.qrcode.internal.SkyQRCodeDecoder
import com.sky.widget.qrcode.internal.SkyQRCodeEncoder

/**
 * 二维码组件入口。
 *
 * 提供二维码 / 条形码生成、本地图片解析等工具能力，
 * 并作为 [SkyQRCodeScanner]、[SkyQRCodeImage] 与 [SkyBarcodeImage] 的命名空间。
 */
object SkyQRCode {

    /**
     * 扫码结果类型 Extra key。
     */
    const val RESULT_TYPE: String = "result_type"

    /**
     * 扫码结果内容 Extra key。
     */
    const val RESULT_STRING: String = "result_string"

    /**
     * 扫码成功。
     */
    const val RESULT_SUCCESS: Int = 1

    /**
     * 扫码失败。
     */
    const val RESULT_FAILED: Int = 2

    /**
     * 解析本地图片中的二维码 / 条形码。
     *
     * @param path 图片文件路径
     * @return 解析结果
     */
    fun analyzeBitmap(path: String?): SkyQRCodeResult {
        val result = SkyQRCodeDecoder.decodeBitmap(path)
        return if (result != null) {
            SkyQRCodeResult.Success(
                text = result.text,
                barcode = null,
                format = result.barcodeFormat?.name
            )
        } else {
            SkyQRCodeResult.Failure()
        }
    }

    /**
     * 解析 [Bitmap] 中的二维码 / 条形码。
     *
     * @param bitmap 待解析图片
     * @return 解析结果
     */
    fun analyzeBitmap(bitmap: Bitmap?): SkyQRCodeResult {
        val result = SkyQRCodeDecoder.decodeBitmap(bitmap)
        return if (result != null) {
            SkyQRCodeResult.Success(
                text = result.text,
                barcode = bitmap,
                format = result.barcodeFormat?.name
            )
        } else {
            SkyQRCodeResult.Failure()
        }
    }

    /**
     * 生成正方形二维码图片。
     *
     * @param content 二维码内容
     * @param size 生成图片的宽高，单位 px
     * @return 二维码 Bitmap
     */
    fun createQRCode(content: String, size: Int): Bitmap {
        return SkyQRCodeEncoder.createQRCode(content, size)
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
        logo: Bitmap? = null,
        logoSize: Int = 0,
        logoCornerRadius: Float = 0f
    ): Bitmap? {
        return SkyQRCodeEncoder.createQRCode(content, width, height, logo, logoSize, logoCornerRadius)
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
        return SkyQRCodeEncoder.createBarcode(content, format, width, height)
    }
}

/**
 * 二维码图片组件。
 *
 * 将文本内容编码为二维码并渲染为 Image。支持在中心嵌入 Logo，
 * Logo 支持自定义大小与圆角；Logo 大小上限为二维码的 1/3，超过时打印警告并回退默认大小。
 *
 * @param content 二维码内容
 * @param modifier 外部修饰符
 * @param size 二维码尺寸
 * @param logo 中心 Logo，可为 null
 * @param logoSize Logo 边长；0.dp 表示默认取二维码短边的 1/5；超过 1/3 时回退默认大小
 * @param logoCornerRadius Logo 圆角半径；0.dp 表示不裁剪圆角
 */
@Composable
fun SkyQRCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    logo: Bitmap? = null,
    logoSize: Dp = 0.dp,
    logoCornerRadius: Dp = 0.dp
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val logoSizePx = with(density) { logoSize.roundToPx() }
    val cornerRadiusPx = with(density) { logoCornerRadius.toPx() }
    val bitmap = remember(content, sizePx, logo, logoSizePx, cornerRadiusPx) {
        SkyQRCodeEncoder.createQRCode(content, sizePx, sizePx, logo, logoSizePx, cornerRadiusPx)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(size)
        )
    }
}

/**
 * 条形码图片组件。
 *
 * 将文本内容编码为条形码（黑条白底）并渲染为 Image，
 * 支持 CODE_128 / EAN_13 等常用一维条形码格式。
 *
 * @param content 条形码内容
 * @param modifier 外部修饰符
 * @param format 条形码格式，默认 [SkyBarcodeFormat.CODE_128]
 * @param width 条形码宽度
 * @param height 条形码高度
 */
@Composable
fun SkyBarcodeImage(
    content: String,
    modifier: Modifier = Modifier,
    format: SkyBarcodeFormat = SkyBarcodeFormat.CODE_128,
    width: Dp = 200.dp,
    height: Dp = 100.dp
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }
    val bitmap = remember(content, format, widthPx, heightPx) {
        SkyQRCodeEncoder.createBarcode(content, format, widthPx, heightPx)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(width, height)
        )
    }
}
