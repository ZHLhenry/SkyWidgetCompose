plugins {
    alias(libs.plugins.sky.android.library)
    alias(libs.plugins.sky.android.hilt)
    alias(libs.plugins.sky.android.publish)
}

android {
    namespace = "com.sky.widget"
}

dependencies {
    // compileOnly：Lottie 完全不传递给消费者（编译期 + 运行时都不带）。
    // 消费者使用 SkyLottieRefreshHeader 时需自行导包，组件内部有运行时检查与导包提示
    compileOnly(libs.lottie.compose)
}