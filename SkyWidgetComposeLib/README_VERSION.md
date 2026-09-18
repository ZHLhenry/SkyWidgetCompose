## [v1.0.6] - 2026-09-18
- 新增 SkyViewPage（分页容器）：手动测量/放置实现，支持水平/垂直方向、pageCache 页面缓存、pagerKey 稳定键、contentTransformation 内容变换（内置缩放）、pageAnimationSpec 自定义翻页动画；SkyViewPageState 提供索引/偏移的 State 与 Flow 监听、代码翻页（有/无动画）
- 新增 SkyBanner（无限轮播）：基于 SkyViewPage 的伪无限循环（内部索引放大 + 取模还原），autoScroll/autoScrollTime 自动轮播、拖拽暂停松手恢复；SkyBannerState 暴露真实索引，setPageIndexWithAnimate 相邻页平滑翻页
- 新增 SkyPageIndicator（页面指示器）：SkyPageIndexSource 单一入口零配置联动 ViewPage/Banner，工厂函数可适配任意页码来源；内置圆点/下划线/数字三种样式，itemContent 支持完全自定义
- 新增 SkySwipeMenu（侧滑菜单）：Animatable 进度 + 惯性预测吸附，threshold/direction 可配，SwipeState 代码开合与 isOpen 查询，rememberSaveable 状态恢复；背景高度跟随内容层，兼容无界高度容器
- 构建增强：启用 Library/App release R8 混淆（enableLibraryMinify/enableAppMinify）；keep 规则重构为 keepRules（公开 API 单一事实源，随 AAR 分发）+ minifyRules（自身 R8 元数据规则）双目录
- 示例应用：新增 ViewPage、Banner、侧滑菜单示例页与首页入口
- 同步更新 README_DOC.md（新增上述四组件章节与目录项）

## [v1.0.5] - 2026-09-16
- 新增 SkyBottomSheet（底部弹窗）：基于 Material3 ModalBottomSheet 实现；SkyBottomSheetState 提供 show/hide/animateTo/isVisible；支持 initialValue 初始锚点（Hidden/HalfExpanded/Expanded）；容器背景色 sheetBackgroundColor、顶部圆角 sheetCornerRadius、modifier 可自定义
- 新增 SkyNumberKeyBoard（数字键盘）：经 SkyBottomSheet 弹出或内嵌页面（asBottomSheet）；随机乱序键盘（0-9 全部参与乱序）、showDot 控制小数点键、deleteIcon 支持 SkyKeyBoardIcon.Vector/IconFont 双形式、确认键自定义（confirmText/confirmModifier/confirmDisable）、按键尺寸与间距可调
- 新增 SkyRatingBar（评分条）：点击/滑动打分、allowHalf 半星（宽度百分比裁剪）、readOnly 只读；图标支持 SkyRatingIcon.Vector/Iconfont 双形式，默认内置五角星
- 新增 SkyVerifyCodeEdit（验证码输入框）：Canvas 绘制格子（BottomLine 下划线 / Square 正方形）；经 PlatformTextInputModifierNode 与 IME 会话；仅接受数字、删除回退一位、光标闪烁、输满 onComplete 回调
- SkyQRCode 增强：取景框与条码识别增强，修复反色二维码无法识别问题
- 示例应用：包名迁移 com.sky.widget.samplecp；新增数字键盘、底部弹窗、评分条、验证码示例页与首页入口
- 同步更新 README_DOC.md（新增 SkyBottomSheet、SkyNumberKeyBoard、SkyRatingBar、SkyVerifyCodeEdit 章节与目录项）

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
