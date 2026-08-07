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
    namespace = "com.sky.widget.sample"
    flavorDimensions += "contentType"
    productFlavors {
        create("dev") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
        create("uat") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
        create("prod") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    if (useLocalSkyWidgetCompose) {
        implementation(project(":SkyWidgetComposeLib"))
    } else {

    }

}