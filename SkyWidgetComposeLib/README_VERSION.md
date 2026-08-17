## [v1.0.4] - 2026-08-17
- 新增 SkyQRCode（二维码/条形码）模块：基于 CameraX + ZXing 的扫码与生成能力
  - SkyQRCodeScanner 声明式扫码组件：扫描模式（All/OneD/Product/QRCode/DataMatrix）、取景框尺寸与位置、闪光灯、成功提示音（支持自定义 raw 资源，未设置回退库内置默认音）与震动、pause/resume；连续相同结果去重，不同码即时上报
  - SkyQRCodeImage / SkyBarcodeImage 生成组件；SkyQRCode.createQRCode 支持中心 Logo（上限为码短边 1/3，超过打印警告并回退默认 1/5）与圆角；createBarcode 支持 CODE_128/EAN_13/UPC_A 等 9 种一维条码格式
  - SkyQRCode.analyzeBitmap 解析本地图片中的二维码/条形码（支持文件路径与 Bitmap）
- 微调：修正 SkyPercentImage KDoc、清理实验性注解与 import 顺序，无 API 变更
- 同步更新 README_DOC.md（新增 SkyQRCode 章节）

## [v1.0.3] - 2026-08-16
- 新增 SkyIconFont（图标字体）：纯 Compose 重写自原仓库 iconfont，不依赖 mikepenz/iconics，基于 Text + 原生 Typeface/FontFamily 渲染
- 新增 SkyPercentImage（按比例图片）：纯 Compose 重写自 SkyPercentImageView，按宽/高为基准根据比例计算另一维度，支持任意 Painter 图片源
- SkyIconFontsLib 支持多字体并存与混用，initRegister 注册字体（首个为默认字体），resolveTypeface 四级解析（文件名 → JSON name → css 前缀 → 默认）
- SkyIconFontState 改为可观察状态（mutableStateOf），setIcon/setTint/setFontSize 驱动重组；修复显式 setter 与属性委托 JVM 签名冲突（@JvmName）
- 修正 initRegister 重复初始化默认字体不更新问题；修复 fontName 文件名匹配链路
- 同步更新 README_DOC.md（新增 SkyIconFont、SkyPercentImage 章节）

## [v1.0.2] - 2026-08-11
- 新增 SkyRefreshPagingLayout：基于现有 SkyRefreshLayout 封装的 Paging 3 开箱即用列表容器
- 新增 SkyRefreshState.bindPaging / rememberSkyRefreshPagingState 桥接 Paging 的 LoadState 与刷新动画
- androidx.paging:paging-compose 以 compileOnly 方式引入（与 Lottie 一致，不传递给消费者）；
  使用 SkyRefreshPagingLayout 需自行 implementation(libs.androidx.paging.compose)，
  缺失依赖时组件入口抛 IllegalStateException 并给出导包提示
- SkyPageStateLayout 的 onRetry 拆分为 onEmptyRetry / onErrorRetry
- 修正 SkyRefreshLayout 注释与 README_DOC 文档（header/footer 签名、Paging 章节去重）

## [v1.0.1] - 2026-08-08
- 新增/完善 Badge、GridLayout、SignatureView、StateLayout、AnnotatedText、MarqueeView 等组件
- 修复 SkyBadgeBox 拖拽消除绘制层坐标偏移问题，拖拽效果与 content 严格对齐
- 统一修正各组件 KDoc 与示例，新增完整版 README_DOC 组件文档

## [v1.0.0] - 2026-08-07
- SkyWidgetCompose包重磅首发
