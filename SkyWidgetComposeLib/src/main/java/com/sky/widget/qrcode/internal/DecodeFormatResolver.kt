package com.sky.widget.qrcode.internal

import com.google.zxing.BarcodeFormat
import com.sky.widget.qrcode.SkyQRCodeMode

/**
 * 条码格式解析器。
 *
 * 将 [SkyQRCodeMode] 映射为 ZXing 的 [BarcodeFormat] 列表。
 */
internal object DecodeFormatResolver {

    private val PRODUCT_FORMATS: List<BarcodeFormat> = listOf(
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.RSS_14
    )

    private val ONE_D_FORMATS: List<BarcodeFormat> = PRODUCT_FORMATS + listOf(
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR
    )

    private val QR_CODE_FORMATS: List<BarcodeFormat> = listOf(BarcodeFormat.QR_CODE)

    private val DATA_MATRIX_FORMATS: List<BarcodeFormat> = listOf(BarcodeFormat.DATA_MATRIX)

    val ALL_FORMATS: List<BarcodeFormat> = ONE_D_FORMATS + QR_CODE_FORMATS + DATA_MATRIX_FORMATS

    /** 2D 矩阵码格式集合（QR_CODE / DATA_MATRIX）。 */
    private val TWO_D_FORMATS: List<BarcodeFormat> = QR_CODE_FORMATS + DATA_MATRIX_FORMATS

    /**
     * 根据扫描模式获取对应的条码格式列表。
     */
    fun formatsFor(mode: SkyQRCodeMode): List<BarcodeFormat> = when (mode) {
        SkyQRCodeMode.All -> ALL_FORMATS
        SkyQRCodeMode.OneD -> ONE_D_FORMATS
        SkyQRCodeMode.Product -> PRODUCT_FORMATS
        SkyQRCodeMode.QRCode -> QR_CODE_FORMATS
        SkyQRCodeMode.DataMatrix -> DATA_MATRIX_FORMATS
    }

    /**
     * 将格式列表按维度拆分为 2D（矩阵码）与 1D（条形码）两组。
     *
     * 2D 码有定位图形与 Reed-Solomon 纠错，误报率极低；1D 格式（尤其 UPC_E/EAN_8 等
     * 短码）校验位弱，二维码的密集纹理容易被误判为 1D 条码。先解 2D 再解 1D
     * 可确保真实二维码不会落入 1D 误报。
     *
     * @return first 为 2D 格式列表，second 为 1D 格式列表
     */
    fun splitByDimension(formats: List<BarcodeFormat>): Pair<List<BarcodeFormat>, List<BarcodeFormat>> =
        formats.filter { it in TWO_D_FORMATS } to formats.filter { it !in TWO_D_FORMATS }
}
