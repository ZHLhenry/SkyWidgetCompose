import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.sky.android.application)
    alias(libs.plugins.sky.android.hilt)
    alias(libs.plugins.sky.android.application.flavors)
}

val localProps = Properties().apply {
    val file = File(rootDir, "local.properties")
    if (file.exists()) load(FileInputStream(file))
}
val useLocalSkyWidgetCompose = localProps.getProperty("useLocalSkyWidgetCompose")?.toBooleanStrictOrNull() ?: false

android {
    namespace = "com.sky.widget.samplecp"
    flavorDimensions += "contentType"
    productFlavors {
        create("dev") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skywidgetcompose_debug"
        }
        create("uat") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skywidgetcompose"
        }
        create("prod") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skywidgetcompose"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.skymvi)
    implementation(libs.xlog)
    // 库的 SkyLottieRefreshHeader 为 compileOnly 依赖 Lottie，使用方需自行导包
    implementation(libs.lottie.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    if (useLocalSkyWidgetCompose) {
        implementation(project(":SkyWidgetComposeLib"))
    } else {
        implementation(libs.skyWidgetCompose)
    }
}