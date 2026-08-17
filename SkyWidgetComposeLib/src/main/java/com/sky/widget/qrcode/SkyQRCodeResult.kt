package com.sky.widget.qrcode

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

/**
 * 二维码识别结果。
 *
 * @property text 识别出的文本内容
 * @property barcode 识别到的码图（可能为 null，例如从本地图片解析失败时）
 * @property format 条码格式名称，与 [com.google.zxing.BarcodeFormat.name] 保持一致
 */
@Immutable
sealed class SkyQRCodeResult {
    abstract val text: String?

    /**
     * 识别成功。
     */
    data class Success(
        override val text: String,
        val barcode: Bitmap?,
        val format: String? = null
    ) : SkyQRCodeResult()

    /**
     * 识别失败。
     */
    data class Failure(
        override val text: String? = null
    ) : SkyQRCodeResult()
}
