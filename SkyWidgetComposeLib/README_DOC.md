# SkyWidgetCompose 文档

SkyWidgetCompose 是一组基于 Jetpack Compose 的通用 UI 组件库，覆盖刷新、徽章、网格、签名板、页面状态、高亮文本、跑马灯等常见场景。

---

## 目录

- [快速开始](#快速开始)
- [SkyRefreshLayout（下拉刷新 / 上拉加载）](#skyrefreshlayout下拉刷新--上拉加载)
- [SkyRefreshPagingLayout（Paging 3 分页刷新）](#skyrefreshpaginglayoutpaging-3-分页刷新)
- [SkyBadge（徽章）](#skybadge徽章)
- [SkyGridLayout（静态网格）](#skygridlayout静态网格)
- [SkySignatureView（签名板）](#skysignatureview签名板)
- [SkyPageStateLayout（页面状态布局）](#skypagestatelayout页面状态布局)
- [SkyAnnotatedText（高亮文本）](#skyannotatedtext高亮文本)
- [SkyMarqueeView（跑马灯 / 轮播）](#skymarqueeview跑马灯--轮播)
- [SkyIconFont（图标字体）](#skyiconfont图标字体)
- [SkyPercentImage（按比例图片）](#skypercentimage按比例图片)
- [SkyQRCode（二维码扫描与生成）](#skyqrcode二维码扫描与生成)

---

## 快速开始

项目根目录的 `build.gradle.kts` 已通过 `skyBuild.enableCompose = true` 自动注入 Compose BOM 与核心依赖，模块内无需再手写 Compose 依赖。

在需要使用的模块中直接引用库坐标（发布时）：

```kotlin
dependencies {
    implementation("com.sky.lib:SkyWidgetCompose:1.0.4")
}
```

本地开发时，在 `local.properties` 中设置 `useLocalSkyWidgetCompose=true`，即可通过源码模块 `:SkyWidgetComposeLib` 调试。

---

## SkyRefreshLayout（下拉刷新 / 上拉加载）

容器式刷新组件，基于原生 [Layout] 与 [graphicsLayer] 实现的嵌套滑动刷新/加载容器，
通过标准的 [NestedScrollConnection] 拦截手势，对 [LazyColumn]、[LazyRow]、[HorizontalPager] 等
支持嵌套滚动的子组件实现透明包裹。

### API

```kotlin
@Composable
fun SkyRefreshLayout(
    modifier: Modifier = Modifier,
    state: SkyRefreshState = rememberSkyRefreshState(),
    orientation: Orientation = Orientation.Vertical,
    style: SkyRefreshStyle = SkyRefreshStyle.Translate,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    secondFloorRate: Float = 0f,
    onSecondFloor: (() -> Unit)? = null,
    noMoreDataText: String? = null,
    header: @Composable () -> Unit = { SkyClassicsRefreshHeader(flag = state.refreshFlag, orientation = orientation) },
    footer: @Composable () -> Unit = { SkyClassicsRefreshFooter(flag = state.loadMoreFlag, noMoreData = state.noMoreData, orientation = orientation, noMoreText = noMoreDataText) },
    content: @Composable () -> Unit
)
```

> **注意**：参数较多时容易遗漏组合条件，下文“属性搭配关系”列出了各能力生效必须同时满足的条件。

### 属性搭配关系（能力生效条件）

以下列出各能力需要同时满足的条件，避免只传部分参数导致功能不生效。

#### 下拉刷新

| 条件 | 说明 |
|------|------|
| `state.enableRefresh == true` | 默认为 `true` |
| `onRefresh != null` | 非空回调 |
| Header 有实际测量高度 | 默认 [SkyClassicsRefreshHeader] 已满足 |

三者缺一不可。若 `onRefresh` 为空或 `enableRefresh` 被置为 `false`，即使下拉也不会触发任何行为。

#### 上拉加载

| 条件 | 说明 |
|------|------|
| `state.enableLoadMore == true` | 默认为 `true` |
| `onLoadMore != null` | 非空回调 |
| Footer 有实际测量高度 | 默认 [SkyClassicsRefreshFooter] 已满足 |

三者缺一不可。

#### 无更多数据终态提示

| 条件 | 说明 |
|------|------|
| 在 `onLoadMore` 回调中调用 `state.finish(noMoreData = true)` | 标记数据穷尽 |
| `noMoreDataText != null` | 传入非空文案 |

若只调用了 `finish(noMoreData=true)` 但未传 `noMoreDataText`，Footer 将零尺寸渲染，用户无法看到“没有更多数据”提示。

#### 下拉进入二楼

| 条件 | 说明 |
|------|------|
| `secondFloorRate > 0f` | 启用二楼能力，建议 ≥ 1.5f（常用 2.0f） |
| `onSecondFloor != null` | 二楼触发后的业务回调 |

二级阈值 = **Header 高度 × secondFloorRate**。手指下拉越过该阈值后松手，会回调 `onSecondFloor`，Header 回弹隐藏，**不会触发 `onRefresh`**。

> ⚠️ 当 `secondFloorRate <= 1.0f` 时，二楼阈值 ≤ 普通刷新阈值，普通下拉刷新将无法触发。
>
> 二楼场景推荐搭配 [SkyTwoLevelRefreshHeader]，该 Header 会根据下拉进度自动切换文案（释放立即刷新 → 释放进入二楼）。若使用自定义 Header，可监听 `state.indicatorOffset` 自行换算二楼进度。

#### 样式与 Header 的协作

[style] 只影响**下拉刷新侧**的视觉表现：

| Style | 效果 | 推荐搭配的 Header |
|-------|------|-------------------|
| `Translate` | Content 跟随下移（默认） | 任意 Header |
| `FixedContent` | Content 固定，Header 滑入覆盖在 Content 之上 | 任意 Header |
| `FixedFront` | Header 固定在容器边缘原位，Content 不动，仅 Header 内部动画反馈下拉状态 | 能根据 `indicatorOffset` 驱动进度的 Header，如 [SkyCircleRefreshHeader]、[SkyProgressRefreshHeader] |

#### 程序化触发

通过 `state.autoRefresh()` / `state.autoLoadMore()` 主动触发时，同样需要传入非空的 `onRefresh` / `onLoadMore`，否则不会向业务层派发回调。

### 基础用法

```kotlin
val state = rememberSkyRefreshState()

SkyRefreshLayout(
    state = state,
    onRefresh = { viewModel.refresh() },
    onLoadMore = { viewModel.loadMore() },
    noMoreDataText = "没有更多数据"
) {
    LazyColumn(state = state.listState) {
        items(viewModel.items) { Item(it) }
    }
}
```

### 关键概念

| 名称 | 说明 |
|------|------|
| `SkyRefreshFlag` | `IDLE` / `PULLING` / `REFRESHING` / `FINISHING` |
| `SkyRefreshStyle` | `Translate`（内容跟随）、`FixedContent`（内容固定，Header 滑入覆盖）、`FixedFront`（Header 固定边缘原位，绘制在上层） |
| `state.finish(noMoreData = true)` | 结束加载并标记无更多数据；需配合 `noMoreDataText` 使用 |
| `state.autoRefresh()` / `state.autoLoadMore()` | 主动触发刷新 / 加载；需配合非空的 `onRefresh` / `onLoadMore` |
| `secondFloorRate` + `onSecondFloor` | 两者同时设置才启用二楼能力；阈值 = Header 高度 × rate |

### Header / Footer 定制

每个 Header/Footer 都提供 `@Stable` 状态类 + `rememberXxxState()` 工厂。可定制文案、图标、Lottie 资源、颜色、时间格式等。

可选 Header：

- `SkyClassicsRefreshHeader`（默认）
- `SkyBallRefreshHeader`
- `SkyLottieRefreshHeader`（需额外引入 `com.airbnb.android:lottie-compose:6.7.1`）
- `SkyCircleRefreshHeader`
- `SkyProgressRefreshHeader`
- `SkyTimeRefreshHeader`
- `SkyTwoLevelRefreshHeader`（推荐用于二楼场景）

---

## SkyRefreshPagingLayout（配合 Paging 3 使用）

在 [SkyRefreshLayout] 之上封装的 **Paging 3 开箱即用列表容器**。内部自动完成三件事：

1. 下拉触发 `LazyPagingItems.refresh()`；
2. 滚动到底由 Paging 自动加载下一页（`onLoadMore` 默认无需手动处理）；
3. 监听 `LoadState`，在加载结束后自动调用 `state.finish(noMoreData)` 复位动画，并在列表尾部渲染「加载中 / 加载失败（点击重试）/ 没有更多数据」。

> 容器本身不感知数据来源，所有样式、Header/Footer、覆盖层插槽均透传给 [SkyRefreshLayout]，
> 非 Paging 用法完全不受影响。

### 依赖

Paging 在库中为 `compileOnly` 依赖，**与 `SkyLottieRefreshHeader` 的 Lottie 处理方式一致，不会传递给消费者**。
使用前需在宿主模块自行导包：

```toml
androidx-paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "pagingCompose" }
```

```kotlin
// 消费者模块 build.gradle.kts
dependencies {
    implementation(libs.androidx.paging.compose)
}
```

组件内部对 Paging 相关类做有运行时检查；若运行期缺失依赖会抛出 `IllegalStateException`
并附带明确的导包提示，便于快速定位。

### 基础用法

```kotlin
// ViewModel 侧：暴露 PagingData 流（Pager + PagingSource 自行实现）
val articles: Flow<PagingData<ArticleBean>> = pager.flow.cachedIn(viewModelScope)

// Composable 侧
val lazyPagingItems = viewModel.articles.collectAsLazyPagingItems()

SkyRefreshPagingLayout(
    lazyPagingItems = lazyPagingItems,
    itemKey = { it.id },
    content = { article -> ArticleItem(article) }
)
```

### 关键 API

| 名称 | 说明 |
|------|------|
| `SkyRefreshPagingLayout` | 封装好的 Paging 列表容器，直接传 `lazyPagingItems` + `content` |
| `rememberSkyRefreshPagingState()` | Paging 场景下的刷新状态（等价于 `rememberSkyRefreshState()` 的语义别名） |
| `SkyRefreshState.bindPaging(lazyPagingItems)` | 仅把 Paging 的 `LoadState` 绑定到刷新状态，供需要自定义列表结构时使用 |
| `onRefresh` / `onLoadMore` | 可选自定义回调；为 null 时分别使用 `lazyPagingItems.refresh()` 和 Paging 自动加载 |

### 自定义列表结构（进阶）

若不想用内置 `LazyColumn`，可自行组合 [SkyRefreshLayout] 与 `bindPaging`：

```kotlin
val state = rememberSkyRefreshPagingState()
state.bindPaging(lazyPagingItems)

SkyRefreshLayout(
    state = state,
    onRefresh = { lazyPagingItems.refresh() }
) {
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(lazyPagingItems.itemCount) { index ->
            lazyPagingItems[index]?.let { Item(it) }
        }
    }
}
```

---

### 属性搭配关系

| 能力 | 生效条件 |
|------|----------|
| 下拉刷新 | `onRefresh` 为 null 时由 `LazyPagingItems.refresh()` 自动接管；非空则走自定义逻辑 |
| 上拉加载 | 由 Paging 自动分页，无需设置 `onLoadMore`；非空时改为自定义逻辑 |
| 无更多数据终态 | 依赖 `loadState.append.endOfPaginationReached` + 非空 `noMoreDataText`（默认即有） |
| 二楼 | `secondFloorRate > 0` 且 `onSecondFloor != null`，见 [SkyRefreshLayout] |

---

## SkyBadge（徽章）

提供容器式徽章 `SkyBadgeBox` 与独立徽章 `SkyBadgeView`，支持数字、文本、圆点、九宫格方位以及拖拽消除动画。

### API

```kotlin
@Composable
fun SkyBadgeBox(
    modifier: Modifier = Modifier,
    state: SkyBadgeState = rememberSkyBadgeState(),
    gravity: SkyBadgeGravity = SkyBadgeGravity.TOP_END,
    offset: DpOffset = DpOffset.Zero,
    backgroundColor: Color = Color.Red,
    textColor: Color = Color.White,
    textSize: TextUnit = 11.sp,
    horizontalPadding: Dp = 4.dp,
    verticalPadding: Dp = 0.dp,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 0.dp,
    showShadow: Boolean = true,
    draggable: Boolean = false,
    maxDragDistance: Dp = 80.dp,
    onDragStateChanged: ((Int) -> Unit)? = null,
    badge: @Composable (SkyBadgeState) -> Unit = { DefaultSkyBadgeContent(it) },
    content: @Composable () -> Unit
:)
```

### 基础用法

```kotlin
val state = rememberSkyBadgeState(initialNumber = 12)

SkyBadgeBox(state = state) {
    Icon(imageVector = Icons.Default.Mail, contentDescription = null)
}
```

### 九宫格方位 + 拖拽消除

```kotlin
val state = rememberSkyBadgeState(initialNumber = 8)

SkyBadgeBox(
    state = state,
    gravity = SkyBadgeGravity.TOP_END,
    draggable = true,
    maxDragDistance = 90.dp,
    onDragStateChanged = { dragState ->
        when (dragState) {
            SkyBadgeDragState.START -> { }
            SkyBadgeDragState.DRAGGING -> { }
            SkyBadgeDragState.DRAGGING_OUT_OF_RANGE -> { }
            SkyBadgeDragState.CANCELED -> state.reset()
            SkyBadgeDragState.SUCCEED -> { }
        }
    }
) {
    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Text("目标")
    }
}
```

> 组件尺寸**包裹 content**，badge 基于 content 实际尺寸定位。拖拽绘制层（连接线、爆炸动画）与 content 共用同一坐标系，不会随外部容器宽度偏移。

### 状态说明

| 字段 | 说明 |
|------|------|
| `number` | 0 隐藏，负数显示圆点 |
| `text` | 优先级高于 `number` |
| `maxNumber` | 超过后显示 `maxNumber+`，默认 99 |
| `isExact` | 为 true 时精确显示大数字 |
| `circleShapeThreshold` | 数字位数 ≤ 该值时按正圆绘制，默认 2 |
| `showBadgeThreshold` | 数字 ≤ 该值时隐藏，默认 0 |
| `reset(n)` | 重置数字并清空拖拽偏移 / 状态 |
| `hide()` / `show()` / `toggle()` | 可见性控制 |

---

## SkyGridLayout（静态网格）

按固定列数展示一组数据，类似 `LazyVerticalGrid` 的静态版本，适用于数据量较小、不需要懒加载的场景。

### API

```kotlin
@Composable
fun <T> SkyGridLayout(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (item: T) -> Unit
:)
```

### 用法

```kotlin
SkyGridLayout(
    items = products,
    columns = 3,
    horizontalSpacing = 8.dp,
    verticalSpacing = 12.dp,
    contentPadding = PaddingValues(16.dp)
) { product ->
    ProductCard(product)
}
```

### 特点

- 单元格宽度 = `(内容宽 - 列间距) / columns`
- 每行高度取该行最大 item 高度
- 支持 RTL
- 不满列的最后一行照常摆放

---

## SkySignatureView（签名板）

基于 Android `Paint` 与 `Path` 实现的签名板，支持自定义笔触、清空、保存为 Bitmap。

### API

```kotlin
@Composable
fun SkySignatureView(
    modifier: Modifier = Modifier,
    state: SkySignatureViewState = rememberSkySignatureViewState(),
    color: Color = Color.Black,
    strokeWidth: Dp = 4.dp,
    backgroundColor: Color = Color.White
)

@Composable
fun SkySignatureView(
    modifier: Modifier = Modifier,
    state: SkySignatureViewState = rememberSkySignatureViewState(),
    paint: Paint,
    backgroundColor: Color = Color.White
)
```

### 用法

```kotlin
val state = rememberSkySignatureViewState()

SkySignatureView(state = state)

Button(onClick = { state.clear() }) { Text("清空") }
Button(onClick = {
    val bitmap = state.save(Color.White)
    // 保存或展示 bitmap
}) { Text("保存") }
```

### 状态说明

| 方法 | 说明 |
|------|------|
| `state.clear()` | 清空所有笔迹 |
| `state.save(backgroundColor)` | 输出 ARGB_8888 Bitmap |
| `state.isEmpty` | 当前是否没有任何笔迹 |

---

## SkyPageStateLayout（页面状态布局）

根据页面状态在 Loading / Success / Empty / Error 之间自动切换。

### API

```kotlin
@Composable
fun SkyPageStateLayout(
    pageState: SkyPageState,
    modifier: Modifier = Modifier,
    onEmptyRetry: () -> Unit = {},
    onErrorRetry: () -> Unit = {},
    loading: @Composable () -> Unit = { SkyLoadingWidget() },
    empty: @Composable (message: String) -> Unit = { SkyEmptyWidget(message = it, onRetry = onEmptyRetry) },
    error: @Composable (message: String) -> Unit = { SkyErrorWidget(message = it, onRetry = onErrorRetry) },
    content: @Composable () -> Unit
)
```

### 用法

```kotlin
val state by viewModel

SkyPageStateLayout(
    pageState = state.pageState,
    onEmptyRetry = { viewModel.load() },   // Empty 占位点击重试
    onErrorRetry = { viewModel.load() }    // Error 占位点击重试
) {
    LazyColumn {
        items(state.datas) { ArticleItem(it) }
    }
}
```

### 占位组件

- `SkyLoadingWidget`：居中转圈
- `SkyEmptyWidget(message, image, onRetry)`：空数据占位，`onRetry` 对应 `onEmptyRetry`
- `SkyErrorWidget(message, image, onRetry)`：错误占位，`onRetry` 对应 `onErrorRetry`

### 状态说明

```kotlin
sealed interface SkyPageState {
    data object Loading : SkyPageState
    data object Success : SkyPageState
    data class Empty(val message: String = "") : SkyPageState
    data class Error(val message: String = "") : SkyPageState
}
```

---

## SkyAnnotatedText（高亮文本）

支持通过正则表达式匹配文本并高亮，同时可为匹配内容添加点击事件。

### API

```kotlin
@Composable
fun SkyAnnotatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    annotatedStyle: SpanStyle = SpanStyle(color = MaterialTheme.colorScheme.primary),
    annotatedActions: List<SkyAnnotatedAction> = emptyList()
)

data class SkyAnnotatedAction(
    val regex: String,
    val onClick: ((match: String) -> Unit)? = null
)
```

### 用法

```kotlin
SkyAnnotatedText(
    text = "我已阅读并同意《隐私政策》和《用户协议》",
    annotatedActions = listOf(
        SkyAnnotatedAction(regex = "《隐私政策》") { toast("隐私政策") },
        SkyAnnotatedAction(regex = "《用户协议》") { toast("用户协议") }
    )
)
```

### 特点

- 多规则匹配按起始位置排序，重叠时取最前面的匹配
- 使用 `LinkAnnotation.Clickable` + Material3 `Text`，替代已废弃的 `ClickableText`

---

## SkyMarqueeView（跑马灯 / 轮播）

通用轮播组件，支持上下左右四种切入方向，点击可暂停 / 继续或派发点击事件。

### API

```kotlin
@Composable
fun <T> SkyMarqueeView(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: SkyMarqueeState = rememberSkyMarqueeState(),
    direction: SkyMarqueeDirection = SkyMarqueeDirection.LEFT,
    onItemClick: ((Int, T) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
)
```

### 用法

```kotlin
val items = listOf("公告一", "公告二", "公告三")

SkyMarqueeView(
    items = items,
    direction = SkyMarqueeDirection.LEFT,
    onItemClick = { index, item -> toast("点击 $index: $item") }
) { item ->
    Text(item)
}
```

### 状态控制

```kotlin
val state = rememberSkyMarqueeState(initialFlipping = true, initialFlipInterval = 3000L)

state.startFlipping()   // 开始自动轮播
state.stopFlipping()    // 停止
state.toggleFlipping()  // 切换
state.next(items.size)  // 下一项
state.previous()        // 上一项
state.reset()           // 回到第一项
state.setCurrentIndex(index, items.size)
```

### 特点

- 只有一条数据时不轮播，但仍展示内容
- `onItemClick` 为 null 时，点击内容暂停 / 继续；传入时派发点击事件不自动暂停

---

## SkyIconFont（图标字体）

纯 Compose 重写自原仓库 `SkyWidget/iconfont`，不依赖 mikepenz/iconics 等三方图标库，
使用 Compose `Text` + 原生 `Typeface` / `FontFamily` 渲染 iconfont.cn 导出的图标字体。支持多字体并存与按需混用。

### 资源准备

将 TTF 字体与同名 JSON 映射文件一同放入宿主模块的 `assets/fonts/`：

```
app/src/main/assets/fonts/
├── sky_iconfont.ttf
├── sky_iconfont.json
├── adb_iconfont.ttf
└── adb_iconfont.json
```

- **TTF**：iconfont.cn 下载的字体文件。
- **JSON**：iconfont.cn 导出标准 JSON（含 `name` / `css_prefix_text` / `description` / `glyphs[font_class, unicode]`）。
  JSON 文件名需与 TTF 同名（仅扩展名不同），由 TTF 路径自动推导。

### 初始化

在 `Application.onCreate` 中注册（第一个注册的字体为默认字体）：

```kotlin
class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SkyIconFontsLib.initRegister(
            this,
            ttfPaths = listOf(
                "fonts/sky_iconfont.ttf",
                "fonts/adb_iconfont.ttf"
            )
        )
    }
}
```

多字体设计：内部按「字体文件名精确匹配 → JSON name 精确匹配 → css 前缀匹配 → 默认字体」四级解析；
所有查询方法均带可选 `fontName` 参数，不传时路由到默认字体。

### 基础用法

```kotlin
// 使用默认字体
SkyIconFont(
    iconName = "sky-fenxiang",
    tint = Color.Red,
    fontSize = 24.sp
)

// 指定字体（fontName 为 TTF 文件名，不含扩展名）
SkyIconFont(
    iconName = "adb-device",
    fontName = "adb_iconfont",
    tint = Color.Red
)
```

> 图标名称需带前缀（如 `sky-fenxiang`），前缀来自 JSON 的 `css_prefix_text`。
> 若名称不存在，回落渲染红色 `?` 占位，便于排查映射缺失。

### 动态切换（状态）

当图标需随业务状态（切换图标 / 颜色 / 字号）动态变化时，使用可观察状态：

```kotlin
val iconState = rememberSkyIconFontState(iconName = "sky-fenxiang", tint = Color.Red)

// 在 VM / UI 中调用即可驱动重组
iconState.setIcon("sky-share")
iconState.setTint(Color.Blue)

SkyIconFont(state = iconState)
```

### 公开查询 API

| 方法 | 说明 |
|------|------|
| `initRegister(application, ttfPaths)` | 注册字体（建议 Application 中调用），首个为默认字体 |
| `isInitialized` | 是否已注册字体 |
| `getRegisteredFonts()` | 所有已注册字体摘要（名称 / 前缀 / 图标数） |
| `iconNames(fontName?)` | 指定字体的全部图标名称（已排序） |
| `getIconChar(name, fontName?)` | 名称 → Unicode 字符，不存在返回 null |
| `isIconExists(name, fontName?)` | 图标是否存在 |
| `fontName(fontName?) / iconCount(fontName?) / mappingPrefix(fontName?)` | 字体名称 / 图标数量 / css 前缀 |
| `getSkyIconFontInfoJson(fontName?)` | 输出字体完整信息 JSON（含全部图标映射） |

---

## SkyPercentImage（按比例图片）

纯 Compose 重写自原仓库 `SkyWidget/image/SkyPercentImageView`（View 体系），不依赖 Android View。
按宽度或高度为基准，根据比例自动计算另一维度的尺寸，常用于封面图、头像等固定比例场景。

### API

```kotlin
@Composable
fun SkyPercentImage(
    modifier: Modifier = Modifier,
    painter: Painter,                       // 必需：图片源（本地 / 矢量 / 网络等）
    contentDescription: String? = null,
    basics: SkyPercentBasics = SkyPercentBasics.Width,
    percent: Float = 1f,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop
)
```

`SkyPercentBasics` 枚举：

- `Width`（默认）：高度 = 宽度 × `percent`
- `Height`：宽度 = 高度 × `percent`

### 用法

```kotlin
// 以宽度为基准，高度 = 宽度 × 0.5（即 2:1 比例）
SkyPercentImage(
    painter = painterResource(id = R.drawable.ic_sample_cover),
    basics = SkyPercentBasics.Width,
    percent = 0.5f,
    modifier = Modifier.fillMaxWidth()
)

// 以高度为基准，宽度 = 高度 × 0.75
SkyPercentImage(
    painter = rememberAsyncImagePainter("https://example.com/avatar.png"),
    basics = SkyPercentBasics.Height,
    percent = 0.75f,
    modifier = Modifier.fillMaxHeight()
)
```

### 特点

- 纯图片组件，内部直接 `Image(painter, ...)`；`painter` 必需且支持任意图片源（含 Coil 的 `rememberAsyncImagePainter`）。
- 测量受父约束 min/max 钳制；`percent` 经 `coerceAtLeast(0f)` 兜底，避免非法比例。
- `modifier` 位于第一参数，符合 Compose 约定；尺寸由基准维与比例共同决定，无需额外 `content` 插槽。

---

## SkyQRCode（二维码扫描与生成）

基于 **CameraX + ZXing** 的二维码 / 条形码模块，提供声明式扫码组件 [SkyQRCodeScanner]、
生成组件 [SkyQRCodeImage] / [SkyBarcodeImage]，以及 `SkyQRCode` 工具对象（生成、解析本地图片）。

### 依赖与权限

CameraX 与 ZXing 在库内以 `implementation` 引入，**不会传递给消费者**，无需额外导包。

扫码需要相机权限、震动需要 VIBRATE 权限，均由消费者在 `AndroidManifest.xml` 中声明并在运行时申请：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
```

> ⚠️ 未授予相机权限时，`SkyQRCodeScanner` 会抛出 `SecurityException` 并附带 Manifest 声明提示。

### SkyQRCodeScanner（扫码）

```kotlin
@Composable
fun SkyQRCodeScanner(
    modifier: Modifier = Modifier,
    state: SkyQRCodeState = rememberSkyQRCodeState(),
    onResult: (SkyQRCodeResult) -> Unit,
    onError: (Exception) -> Unit = {},
    viewfinder: @Composable BoxScope.(SkyQRCodeState) -> Unit = { SkyQRCodeViewfinder(state = it, ...) },
    overlay: @Composable BoxScope.(SkyQRCodeState) -> Unit = {}
)
```

| 参数 | 说明 |
|------|------|
| `state` | 扫码器状态：扫描模式、取景框尺寸/位置、闪光灯、提示音、震动、暂停/继续 |
| `onResult` | 识别结果回调（**主线程**派发，可安全执行 UI 操作） |
| `onError` | 相机初始化或绑定失败回调 |
| `viewfinder` | 取景框渲染插槽，默认 [SkyQRCodeViewfinder]（四角边框 + 扫描线动画） |
| `overlay` | 覆盖层插槽，可放置标题、返回按钮、闪光灯开关等自定义 UI |

#### 基础用法

```kotlin
val state = rememberSkyQRCodeState(mode = SkyQRCodeMode.All)

SkyQRCodeScanner(
    state = state,
    modifier = Modifier.fillMaxSize(),
    onResult = { result ->
        when (result) {
            is SkyQRCodeResult.Success -> { /* result.text / result.format */ }
            is SkyQRCodeResult.Failure -> { /* 解析失败（本地图片解析场景） */ }
        }
    },
    onError = { e -> /* 相机异常处理 */ },
    overlay = { s ->
        IconButton(onClick = { s.setFlashEnabled(!s.flashEnabled) }, modifier = Modifier.align(Alignment.BottomCenter)) {
            Icon(if (s.flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, contentDescription = null)
        }
    }
)
```

#### 扫描模式（SkyQRCodeMode）

| 模式 | 识别范围 |
|------|---------|
| `All`（默认） | 所有支持的格式 |
| `OneD` | 一维条码（UPC、EAN、Code 39、Code 128、ITF 等） |
| `Product` | UPC / EAN 商品条码 |
| `QRCode` | 仅二维码 |
| `DataMatrix` | 仅 Data Matrix 码 |

#### 状态能力（SkyQRCodeState）

| 方法 | 说明 |
|------|------|
| `setMode(mode)` | 切换扫描模式（重新绑定分析器生效） |
| `setFrameSize(DpSize)` / `setFrameMarginTop(Dp)` | 取景框尺寸 / 顶部边距；`Dp.Unspecified` 分别表示容器宽度的一半 / 垂直居中 |
| `setFlashEnabled(enabled)` | 闪光灯开关；息屏回前台后自动重新应用 |
| `setBeepEnabled(enabled)` / `setBeepResId(resId)` | 提示音开关与自定义 raw 资源；`resId = 0` 使用库内置默认提示音 |
| `setVibrateEnabled(enabled)` | 震动开关 |
| `pause()` / `resume()` | 暂停 / 继续扫描（识别成功后建议先 `pause()` 处理业务） |

#### 识别行为约定

- **所见即所扫**：识别区域与视觉取景框严格一致（按 PreviewView FILL_CENTER 规则反映射到相机帧）。
- **结果去重**：连续相同结果不重复上报；切换为不同内容的码立即上报；码离开视野（连续空帧）后去重状态自动重置。
- 解码在后台线程执行（CPU 密集），回调切回主线程。

### SkyQRCodeImage / SkyBarcodeImage（生成）

```kotlin
// 二维码：支持中心 Logo（大小、圆角可配）
SkyQRCodeImage(
    content = "https://example.com",
    size = 200.dp,
    logo = logoBitmap,        // 可空
    logoSize = 40.dp,         // 0.dp = 默认取码短边的 1/5
    logoCornerRadius = 8.dp   // 0.dp = 不裁剪圆角
)

// 条形码：黑条白底，支持常用一维条码格式
SkyBarcodeImage(
    content = "6901234567892",
    format = SkyBarcodeFormat.EAN_13,
    width = 200.dp,
    height = 100.dp
)
```

> Logo 大小上限为二维码短边的 **1/3**，超过时打印警告并回退默认大小（1/5），不会抛出异常。
> `SkyBarcodeFormat` 支持：CODE_39 / CODE_93 / CODE_128 / EAN_8 / EAN_13 / ITF / CODABAR / UPC_A / UPC_E；
> 内容不符合格式要求（如 EAN_13 位数错误）时组件不渲染、工具方法返回 null。

### SkyQRCode 工具对象

| 方法 | 说明 |
|------|------|
| `analyzeBitmap(path)` / `analyzeBitmap(bitmap)` | 解析本地图片中的二维码/条形码，返回 `SkyQRCodeResult` |
| `createQRCode(content, size)` | 生成正方形二维码 Bitmap |
| `createQRCode(content, width, height, logo?, logoSize, logoCornerRadius)` | 生成带 Logo 二维码，失败返回 null |
| `createBarcode(content, format, width, height)` | 生成条形码 Bitmap，失败返回 null |

---

## 附录：库资源前缀

库内字符串 / 颜色资源统一使用前缀 `sky_rl_*`，R 文件路径为 `com.sky.widget.R`。

---

## 附录：发布坐标

```groovy
implementation "com.sky.lib:SkyWidgetCompose:1.0.4"
```

发布仓库与凭证来自 `local.properties` 中的 `mavenCentral.*` 配置；发布账号与仓库读取账号不同，请勿混用。