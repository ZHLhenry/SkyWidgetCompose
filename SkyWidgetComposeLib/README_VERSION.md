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
