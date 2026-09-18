// skyBuild 配置
extra["skyBuild.appName"] = "SkyWidgetCompose"
extra["skyBuild.applicationId"] = "com.sky.widget.samplecp"
extra["skyBuild.versionCode"] = 107
extra["skyBuild.versionName"] = "1.0.7"
extra["skyBuild.compileSdk"] = 37
extra["skyBuild.minSdk"] = 24
extra["skyBuild.targetSdk"] = 35
extra["skyBuild.enableViewBinding"] = false
extra["skyBuild.enableDataBinding"] = false
// 开启后由 build-logic 自动 apply org.jetbrains.kotlin.plugin.compose 到所有子模块，
extra["skyBuild.enableBuildConfig"] = true
// 并自动注入 Compose BOM + 核心依赖；core:model / core:common 等内部模块仅注入 runtime。
extra["skyBuild.enableCompose"] = true
// 默认版本由 SkyBuildLogic 内置，此处覆盖为项目当前使用的 BOM 版本。
//extra["skyBuild.composeBomVersion"] = "2026.06.01"
// 开启后由 build-logic 内聚配置：Library release 产物（AAR classes.jar）打包前执行 R8 混淆，
// 公开 API 单一事实源为 src/main/keepRules/*.keep（AGP 自动随 AAR 分发 + 显式注入自身 R8）
extra["skyBuild.enableLibraryMinify"] = true
// 开启后由 build-logic 内聚配置：App release 执行 R8 压缩混淆与资源压缩（debug 均不开启），
// 规则汇总自 app/src/main/keepRules/*.keep，并与 library 随 AAR 分发的消费者规则协同
extra["skyBuild.enableAppMinify"] = true
