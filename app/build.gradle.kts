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

// Kotlin 2.3+ 的 Compose 编译器会在 R8 混淆时向 mapping 文件追加 group key 映射条目，
// 并由 report<Variant>ComposeMappingErrors 任务收集校验。该收集器的 tokenizer 存在缺陷
// （Google IssueTracker 555304803），遇到含 $ 嵌套类、<clinit> 等签名时会输出大量
// "Failed to collect Compose stack trace mapping (Failed to tokenize ...)" 警告。
// 项目未启用 GroupKeys 诊断堆栈模式（Composer.setDiagnosticStackTraceMode），该映射无实际用途，
// 故按官方建议整体关闭此功能以消除签名打包时的警告。
composeCompiler {
    includeComposeMappingFile.set(false)
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