package com.sky.widget.qrcode

import androidx.compose.runtime.Immutable

/**
 * 二维码扫描模式。
 *
 * 每个模式对应一组 [com.google.zxing.BarcodeFormat]，用于限制扫码器只识别特定类型的条码。
 */
@Immutable
enum class SkyQRCodeMode {
    /**
     * 识别所有支持的条码格式。
     */
    All,

    /**
     * 仅识别一维条码（UPC、EAN、Code 39、Code 128、ITF 等）。
     */
    OneD,

    /**
     * 仅识别 UPC / EAN 商品条码。
     */
    Product,

    /**
     * 仅识别二维码。
     */
    QRCode,

    /**
     * 仅识别 Data Matrix 码。
     */
    DataMatrix
}
