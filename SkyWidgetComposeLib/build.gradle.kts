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

    // compileOnly：Paging 完全不传递给消费者（编译期 + 运行时都不带），与 Lottie 一致。
    // 消费者使用 SkyRefreshPagingLayout 时需自行导包：implementation(libs.androidx.paging.compose)，
    // 组件内部有运行时检查与导包提示
    compileOnly(libs.androidx.paging.compose)

    // compileOnly：Coil 完全不传递给消费者（编译期 + 运行时都不带），与 Lottie 一致。
    // 消费者使用 SkyPreView 时需自行导包：implementation(libs.coil.compose)，
    // GIF/WebP 动图还需 implementation(libs.coil.gif)，组件内部有运行时检查与导包提示；
    // coil-gif 存在时组件自动注册动图解码器（Class.forName 守卫），消费者无需手动配置 ImageLoader
    compileOnly(libs.coil.compose)
    compileOnly(libs.coil.gif)

    // CameraX：二维码扫描组件运行时必需
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // ZXing：二维码编解码核心
    implementation(libs.zxing.core)
}