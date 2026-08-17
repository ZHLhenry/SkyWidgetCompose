package com.sky.widget.qrcode

/**
 * 条形码格式。
 *
 * 枚举生成能力支持的一维条形码格式，与具体编码实现解耦，
 * 公开 API 不暴露底层编码库类型。
 */
enum class SkyBarcodeFormat {

    /**
     * Code 39，支持数字、大写字母与少量符号。
     */
    CODE_39,

    /**
     * Code 93，Code 39 的高密度版本。
     */
    CODE_93,

    /**
     * Code 128，支持全部 ASCII 字符，通用性最强。
     */
    CODE_128,

    /**
     * EAN-8，8 位数字（含校验位），用于小件商品。
     */
    EAN_8,

    /**
     * EAN-13，12 位数字（校验位自动生成）或 13 位数字，用于商品条码。
     */
    EAN_13,

    /**
     * ITF（Interleaved 2 of 5），仅支持偶数位数字。
     */
    ITF,

    /**
     * Codabar，支持数字与少量字母，常用于物流、血库。
     */
    CODABAR,

    /**
     * UPC-A，11 位数字（校验位自动生成）或 12 位数字，北美商品条码。
     */
    UPC_A,

    /**
     * UPC-E，UPC-A 的压缩形式，仅支持 7~8 位数字。
     */
    UPC_E
}
