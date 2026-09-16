package com.sky.widget.samplecp

import com.sky.mvi.SkyMVILib
import com.sky.mvi.SkyMVILibConfig
import com.sky.mvi.base.BaseApplication
import com.sky.widget.iconfont.SkyIconFontsLib
import dagger.hilt.android.HiltAndroidApp

/**
 * 示例 Application：继承 SkyMVILib 的 [BaseApplication] 复用网络监听与前后台观测，
 * 通过 [@HiltAndroidApp] 启用 Hilt 依赖注入。
 *
 * SkyMVILib 必须在 super.onCreate() 之前初始化，否则后续 Activity 启动
 * 调用 SkyMVILib.requireInit() 时会抛 UninitializedException。
 */
@HiltAndroidApp
class SampleApp : BaseApplication() {
    override fun onCreate() {
        // SkyMVILib 必须在 super.onCreate() 之前初始化（BaseApplication 会用到其配置）。
        // 示例工程不依赖 XLog / OkHttp 日志输出，故此处关闭对应日志模块。
        SkyMVILib.init(
            SkyMVILibConfig.Builder(this)
                .enableXLog(enableXLogLib = true)
                .enableOkHttpLogLib(enableOkHttpLogLib = false)
                .build()
        )
        super.onCreate()
        // 注册多个 iconfont 字体（assets/fonts 下存在才注册），第一个为默认字体。
        val assets = applicationContext.assets.list("fonts").orEmpty().toSet()
        val ttfPaths = listOf("sky_iconfont", "testSky_iconfont", "adb_iconfont")
            .map { "fonts/$it.ttf" }
            .filter { ttf -> assets.contains(ttf.substringAfter("fonts/")) }
        SkyIconFontsLib.initRegister(this, ttfPaths = ttfPaths)
    }
}
