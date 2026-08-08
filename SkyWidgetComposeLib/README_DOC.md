# SkyWidgetCompose 文档

SkyWidgetCompose 是一组基于 Jetpack Compose 的通用 UI 组件库，覆盖刷新、徽章、网格、签名板、页面状态、高亮文本、跑马灯等常见场景。

---

## 目录

- [快速开始](#快速开始)
- [SkyRefreshLayout（下拉刷新 / 上拉加载）](#skyrefreshlayout下拉刷新--上拉加载)
- [SkyBadge（徽章）](#skybadge徽章)
- [SkyGridLayout（静态网格）](#skygridlayout静态网格)
- [SkySignatureView（签名板）](#skysignatureview签名板)
- [SkyPageStateLayout（页面状态布局）](#skypagestatelayout页面状态布局)
- [SkyAnnotatedText（高亮文本）](#skyannotatedtext高亮文本)
- [SkyMarqueeView（跑马灯 / 轮播）](#skymarqueeview跑马灯--轮播)

---

## 快速开始

项目根目录的 `build.gradle.kts` 已通过 `skyBuild.enableCompose = true` 自动注入 Compose BOM 与核心依赖，模块内无需再手写 Compose 依赖。

在需要使用的模块中直接引用库坐标（发布时）：

```kotlin
dependencies {
    implementation("com.sky.lib:SkyWidgetCompose:1.0.0")
}
```

本地开发时，在 `local.properties` 中设置 `useLocalSkyWidgetCompose=true`，即可通过源码模块 `:SkyWidgetComposeLib` 调试。

---

## SkyRefreshLayout（下拉刷新 / 上拉加载）

容器式刷新组件，内部自动托管 `LazyListState`，支持 `LazyColumn` / `LazyRow`。

### API

```kotlin
@Composable
fun SkyRefreshLayout(
    state: SkyRefreshState = rememberSkyRefreshState(),
    orientation: Orientation = Orientation.Vertical,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    autoLoadMoreThreshold: Dp = 0.dp,
    secondFloorRate: Float = 0f,
    onSecondFloor: () -> Unit = {},
    noMoreDataText: String? = null,
    style: SkyRefreshStyle = SkyRefreshStyle.Translate,
    header: @Composable (SkyRefreshState) -> Unit = { SkyClassicsRefreshHeader(state = it) },
    footer: @Composable (SkyRefreshState) -> Unit = { SkyClassicsRefreshFooter(state = it) },
    content: @Composable () -> Unit
)
```

### 基础用法

```kotlin
val state = rememberSkyRefreshState()

SkyRefreshLayout(
    state = state,
    onRefresh = { viewModel.refresh() },
    onLoadMore = { viewModel.loadMore() },
    autoLoadMoreThreshold = 80.dp,
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
| `state.finish(noMoreData = true)` | 结束加载并标记无更多数据 |
| `state.autoRefresh()` / `state.autoLoadMore()` | 主动触发刷新 / 加载 |
| `secondFloorRate` | 大于 0 时，下拉超过 `headerBound × rate` 松手进入二楼 |

### Header / Footer 定制

每个 Header/Footer 都提供 `@Stable` 状态类 + `rememberXxxState()` 工厂。可定制文案、图标、Lottie 资源、颜色、时间格式等。

可选 Header：

- `SkyClassicsRefreshHeader`（默认）
- `SkyBallRefreshHeader`
- `SkyLottieRefreshHeader`（需额外引入 `com.airbnb.android:lottie-compose:6.7.1`）
- `SkyCircleRefreshHeader`
- `SkyProgressRefreshHeader`
- `SkyTimeRefreshHeader`
- `SkyTwoLevelRefreshHeader`

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
)
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
)
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
    onRetry: () -> Unit = {},
    loading: @Composable () -> Unit = { SkyLoadingWidget() },
    empty: @Composable (message: String) -> Unit = { SkyEmptyWidget(message = it, onRetry = onRetry) },
    error: @Composable (message: String) -> Unit = { SkyErrorWidget(message = it, onRetry = onRetry) },
    content: @Composable () -> Unit
)
```

### 用法

```kotlin
val state by viewModel

SkyPageStateLayout(
    pageState = state.pageState,
    onRetry = { viewModel.load() }
) {
    LazyColumn {
        items(state.datas) { ArticleItem(it) }
    }
}
```

### 占位组件

- `SkyLoadingWidget`：居中转圈
- `SkyEmptyWidget(message, image, onRetry)`：空数据占位
- `SkyErrorWidget(message, image, onRetry)`：错误占位

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

## 附录：库资源前缀

库内字符串 / 颜色资源统一使用前缀 `sky_rl_*`，R 文件路径为 `com.sky.widget.R`。

---

## 附录：发布坐标

```groovy
implementation "com.sky.lib:SkyWidgetCompose:1.0.0"
```

发布仓库与凭证来自 `local.properties` 中的 `mavenCentral.*` 配置；发布账号与仓库读取账号不同，请勿混用。
