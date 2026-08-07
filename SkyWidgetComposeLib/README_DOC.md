# SkyWidgetCompose 技术文档

> 一个基于 Jetpack Compose 的高性能下拉刷新 / 上拉加载组件库

---

## 大纲

- [1. 项目概述](#1-项目概述)
- [2. 核心架构](#2-核心架构)
  - [2.1 状态机驱动](#21-状态机驱动)
  - [2.2 手势拦截与路由分发](#22-手势拦截与路由分发)
  - [2.3 硬件加速渲染](#23-硬件加速渲染)
  - [2.4 视口解耦与增量渲染](#24-视口解耦与增量渲染)
- [3. 核心组件](#3-核心组件)
  - [3.1 SkyRefreshLayout](#31-skyrefreshlayout)
  - [3.2 SkyRefreshState](#32-skyrefreshstate)
  - [3.3 SkyRefreshFlag](#33-skyrefreshflag)
  - [3.4 SkyRefreshStyle](#34-skyrefreshstyle)
- [4. 内置指示器](#4-内置指示器)
  - [4.1 Header 组件](#41-header-组件)
  - [4.2 Footer 组件](#42-footer-组件)
- [5. 位移样式](#5-位移样式)
- [6. 资源定制](#6-资源定制)
- [7. 使用指南](#7-使用指南)
  - [7.1 基础用法](#71-基础用法)
  - [7.2 仅下拉刷新](#72-仅下拉刷新)
  - [7.3 仅上拉加载](#73-仅上拉加载)
  - [7.4 横向滑动](#74-横向滑动)
  - [7.5 自定义指示器](#75-自定义指示器)
  - [7.6 程序化触发](#76-程序化触发)
  - [7.7 下拉进入二楼](#77-下拉进入二楼)
- [8. 行为约定](#8-行为约定)
- [9. 构建与依赖](#9-构建与依赖)
- [10. 资源清单](#10-资源清单)

---

## 1. 项目概述

SkyWidgetCompose 是一个专为 Jetpack Compose 设计的下拉刷新 / 上拉加载组件库，提供声明式 API、流畅的动画效果和高度可定制的指示器组件。

**核心特性：**

- **声明式 API**：完全符合 Compose 设计哲学，通过状态驱动 UI
- **高性能渲染**：基于 `graphicsLayer` 硬件加速，零重绘位移
- **无侵入式设计**：通过 `NestedScrollConnection` 透明包裹任意滚动组件
- **丰富的指示器**：7 种内置 Header + 1 种 Footer，支持完全自定义
- **双端泛型适配**：单套逻辑兼容纵向 / 横向滚动
- **二楼机制**：支持下拉进入二楼（参考 SmartRefreshLayout TwoLevelHeader）
- **视口解耦**：上拉加载完成时 Content 驻留，新数据无缝衔接

---

## 2. 核心架构

### 2.1 状态机驱动

组件采用有限状态机（FSM）管理刷新生命周期，定义了四个确定性状态：

```
┌─────────┐     下拉越界      ┌──────────┐     达到阈值松手     ┌────────────┐
│  IDLE   │ ───────────────→ │ PULLING  │ ─────────────────→ │ REFRESHING │
└─────────┘                  └──────────┘                    └────────────┘
     ↑                            │                                │
     │                            │ 未达阈值松手                     │ 业务层调用
     │                            ↓                                ↓
     │                       ┌──────────┐                    ┌────────────┐
     └───────────────────────│  FINISHING │←───────────────────│  FINISHING  │
           回弹动画完成        └──────────┘   回弹动画完成      └────────────┘
```

| 状态 | 描述 | 触发条件 |
|------|------|----------|
| `IDLE` | 空闲状态 | 默认状态，组件静止或正常滚动 |
| `PULLING` | 越界拉拽 | 手势越过物理边界，但未达触发阈值 |
| `REFRESHING` | 异步执行 | 偏移量突破阈值，已派发业务回调 |
| `FINISHING` | 闭幕回弹 | 业务层调用 `finish()`，执行归位动画 |

### 2.2 手势拦截与路由分发

通过实现 `NestedScrollConnection` 接口，在滚动事件流的 Pre / Post 阶段拦截手势能量：

```
┌─────────────────────────────────────────────────────────────────┐
│                     NestedScrollConnection                       │
├─────────────────────────────────────────────────────────────────┤
│  onPreScroll   │ 子视图滚动前：优先收起已拉出的 Header/Footer      │
│  onPostScroll  │ 子视图滚动后：将溢出位移转化为阻尼拉长效果        │
│  onPreFling    │ 惯性滚动前：判断是否触发刷新/加载/二楼           │
│  onPostFling   │ 惯性滚动后：复位未达阈值的 PULLING 状态          │
└─────────────────────────────────────────────────────────────────┘
```

**关键设计：**

- **双端泛型适配**：通过 `mainAxis()` 扩展函数将二维向量降维为主轴一维向量，单套逻辑兼容横/纵向
- **阻尼控制**：Header 侧使用 `stickinessLevel` 阻尼系数（0~1），Footer 侧 1:1 线性跟手
- **惯性滚动限制**：Fling 最多消费前 5 帧，阻尼倍率 0.3，避免 Header 缓慢爬出
- **互斥约束**：下拉刷新与上拉加载不能同时进行

### 2.3 硬件加速渲染

摒弃传统改变约束导致的高频 UI 重排，采用 `graphicsLayer` 图层平移：

```
┌─────────────────────────────────────────────────────────────────┐
│                        Layout 阶段                               │
│  ┌─────────┐  ┌─────────────┐  ┌─────────┐                      │
│  │ Header  │  │   Content   │  │ Footer  │  ← 静态约束测量       │
│  │ (测量)  │  │   (测量)    │  │ (测量)  │                      │
│  └─────────┘  └─────────────┘  └─────────┘                      │
├─────────────────────────────────────────────────────────────────┤
│                        Draw 阶段                                 │
│  ┌─────────┐  ┌─────────────┐  ┌─────────┐                      │
│  │ Header  │  │   Content   │  │ Footer  │  ← graphicsLayer     │
│  │translationY│  │translationY│  │translationY│   硬件加速平移    │
│  └─────────┘  └─────────────┘  └─────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

**优势：**

1. **静态约束测量**：Content 始终保持全屏测量，不因拖拽发生约束变更
2. **硬件加速平移**：仅在 Draw 阶段修改 translation 属性，CPU 开销极低
3. **边界动态扩容**：解耦驻留时通过 `addedHeight` 动态延展物理渲染范围

### 2.4 视口解耦与增量渲染

**问题场景**：上拉加载完成后，新数据追加到列表底部。如果直接移除 Footer，列表内容会突然上跳。

**解决方案**：视口解耦分离

```
┌─────────────────────────────────────────────────────────────────┐
│  加载前                                                          │
│  ┌─────────────┐  ← indicatorOffset = -footerBound              │
│  │   Content   │                                               │
│  │  (驻留中)   │  ← decoupledContentOffset = -footerBound       │
│  ├─────────────┤                                               │
│  │   Footer    │  ← 正在加载...                                 │
│  └─────────────┘                                               │
├─────────────────────────────────────────────────────────────────┤
│  加载后（解耦）                                                  │
│  ┌─────────────┐  ← indicatorOffset = 0（瞬间归位）              │
│  │   Content   │                                               │
│  │  (驻留中)   │  ← decoupledContentOffset = -footerBound       │
│  │             │    （保持不动，用户无感知）                      │
│  │  新数据区域  │  ← addedHeight = footerBound（动态扩容）        │
│  └─────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

**数学关系**：
```
H + addedHeight + decoupledContentOffset == H + indicatorOffset
→ addedHeight = indicatorOffset - decoupledContentOffset
```

---

## 3. 核心组件

### 3.1 SkyRefreshLayout

核心刷新容器，基于原生 `Layout` 与硬件加速的复合渲染引擎。

**函数签名：**

```kotlin
@Composable
fun SkyRefreshLayout(
    modifier: Modifier = Modifier,
    state: SkyRefreshState,
    orientation: Orientation = Orientation.Vertical,
    style: SkyRefreshStyle = SkyRefreshStyle.Translate,
    onRefresh: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    secondFloorRate: Float = 0f,
    onSecondFloor: (() -> Unit)? = null,
    noMoreDataText: String? = null,
    header: @Composable () -> Unit = { SkyClassicsRefreshHeader(...) },
    footer: @Composable () -> Unit = { SkyClassicsRefreshFooter(...) },
    content: @Composable () -> Unit
)
```

**参数说明：**

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `modifier` | `Modifier` | `Modifier` | 外部修饰符 |
| `state` | `SkyRefreshState` | 必填 | 核心状态机引擎 |
| `orientation` | `Orientation` | `Vertical` | 排版方向（纵向/横向） |
| `style` | `SkyRefreshStyle` | `Translate` | 位移样式 |
| `onRefresh` | `(() -> Unit)?` | `null` | 下拉刷新回调 |
| `onLoadMore` | `(() -> Unit)?` | `null` | 上拉加载回调 |
| `secondFloorRate` | `Float` | `0f` | 二楼触发倍率（>0 启用） |
| `onSecondFloor` | `(() -> Unit)?` | `null` | 进入二楼回调 |
| `noMoreDataText` | `String?` | `null` | "无更多数据"文案（不传则终态不展示） |
| `header` | `@Composable () -> Unit` | `SkyClassicsRefreshHeader` | 自定义头部渲染器 |
| `footer` | `@Composable () -> Unit` | `SkyClassicsRefreshFooter` | 自定义尾部渲染器 |
| `content` | `@Composable () -> Unit` | 必填 | 内容主体 |

### 3.2 SkyRefreshState

核心状态机与数据源控制中心，作为唯一的数据源（Source of Truth）。

**创建方式：**

```kotlin
val state = rememberSkyRefreshState(
    stickinessLevel = 0.5f,  // 阻尼系数（0~1）
    enableRefresh = true,    // 是否启用下拉刷新
    enableLoadMore = true    // 是否启用上拉加载
)
```

**属性：**

| 属性 | 类型 | 描述 |
|------|------|------|
| `stickinessLevel` | `Float` | 阻尼系数，可动态修改 |
| `enableRefresh` | `Boolean` | 全局开关：是否允许下拉刷新 |
| `enableLoadMore` | `Boolean` | 全局开关：是否允许上拉加载 |
| `noMoreData` | `Boolean` | 数据边界标识（仅应通过 `finish()` 变更） |
| `indicatorOffset` | `Float` | 当前指示器的实际物理偏移量（px） |
| `refreshFlag` | `SkyRefreshFlag` | 顶部下拉刷新状态机标识 |
| `loadMoreFlag` | `SkyRefreshFlag` | 底部上拉加载状态机标识 |
| `headerBound` | `Float` | Header 的实际测量大小（internal） |
| `footerBound` | `Float` | Footer 的实际测量大小（internal） |

**方法：**

| 方法 | 描述 |
|------|------|
| `finish(noMoreData: Boolean = false)` | 统一结束当前活跃的刷新/加载操作 |
| `autoRefresh()` | 程序化触发下拉刷新 |
| `autoLoadMore()` | 程序化触发上拉加载 |

### 3.3 SkyRefreshFlag

刷新容器的生命周期状态枚举。

```kotlin
enum class SkyRefreshFlag {
    IDLE,       // 空闲状态
    PULLING,    // 越界拉拽状态
    REFRESHING, // 异步执行状态
    FINISHING   // 闭幕回弹状态
}
```

### 3.4 SkyRefreshStyle

位移样式枚举，定义下拉刷新时三层结构的空间布局策略。

```kotlin
enum class SkyRefreshStyle {
    Translate,     // 默认：Header 推出，Content 跟随下移（微信式）
    FixedContent,  // 内容固定：Header 滑入覆盖（淘宝式）
    FixedFront     // 固定在前：Header 悬浮，仅驱动内部动画（美团式）
}
```

---

## 4. 内置指示器

### 4.1 Header 组件

| 组件 | 描述 | 适用场景 |
|------|------|----------|
| `SkyClassicsRefreshHeader` | 经典默认 Header：旋转图标 | 通用场景 |
| `SkyBallRefreshHeader` | 球状加载 Header：三球缩放动画 | 通用场景 |
| `SkyLottieRefreshHeader` | Lottie 动画 Header | 需要丰富动画效果 |
| `SkyCircleRefreshHeader` | 圆圈风格 Header（Material Design） | FixedContent 样式 |
| `SkyProgressRefreshHeader` | 辉光+进度条 Header | FixedFront 样式 |
| `SkyTimeRefreshHeader` | 时间文案 Header（ClassicsHeader 风格） | 需要显示最后更新时间 |
| `SkyTwoLevelRefreshHeader` | 二楼 Header | 下拉进入二楼场景 |

**使用示例：**

```kotlin
// 使用内置球状加载 Header
SkyRefreshLayout(
    state = state,
    header = { SkyBallRefreshHeader(flag = state.refreshFlag) },
    ...
)

// 使用圆圈风格 Header（FixedContent 样式）
SkyRefreshLayout(
    state = state,
    style = SkyRefreshStyle.FixedContent,
    header = { SkyCircleRefreshHeader(state) },
    ...
)
```

### 4.2 Footer 组件

| 组件 | 描述 |
|------|------|
| `SkyClassicsRefreshFooter` | 经典默认 Footer：旋转图标 + 文案，支持"无更多数据"终态停靠 |

**特性：**

- 加载中：旋转图标 + 文案（500ms 循环旋转）
- 数据穷尽：静态文案（可被 1:1 跟手拉出停靠查看）
- `noMoreText` 为 null 时终态整体不渲染（零尺寸）

---

## 5. 位移样式

### Translate（默认）

经典微信式：Header 从顶部推出，Content 整体跟随下移。

```
下拉前          下拉中
┌─────────┐    ┌─────────┐
│ Content │    │ Header  │  ← 滑入
│         │    ├─────────┤
│         │    │ Content │  ← 跟随下移
└─────────┘    └─────────┘
```

### FixedContent

类淘宝效果：Content 固定不动，Header 从顶部滑入覆盖。

```
下拉前          下拉中
┌─────────┐    ┌─────────┐
│ Content │    │ Header  │  ← 滑入覆盖
│         │    │ (覆盖)  │
│         │    │ Content │  ← 固定不动
└─────────┘    └─────────┘
```

### FixedFront

类美团效果：Header 始终悬浮在内容顶层原位，仅驱动内部动画。

```
下拉前          下拉中
┌─────────┐    ┌─────────┐
│ Header  │    │ Header  │  ← 位置不变，内部动画
│ (隐藏)  │    │ (显示)  │
├─────────┤    ├─────────┤
│ Content │    │ Content │  ← 固定不动
└─────────┘    └─────────┘
```

---

## 6. 资源定制

每个 Header/Footer 都有对应的 `@Stable` 状态类 + `rememberXxxState()` 工厂，所有可定制项通过 state 传递。

**对应关系：**

| 组件 | 状态类 | 可定制项 |
|------|--------|----------|
| `SkyClassicsRefreshHeader` | `SkyClassicsRefreshHeaderState` | `iconRes` |
| `SkyClassicsRefreshFooter` | `SkyClassicsRefreshFooterState` | `loadingText`, `loadingIconRes` |
| `SkyBallRefreshHeader` | `SkyBallRefreshHeaderState` | `refreshingColor`, `idleColor` |
| `SkyLottieRefreshHeader` | `SkyLottieRefreshHeaderState` | `rawRes`, `speed` |
| `SkyCircleRefreshHeader` | `SkyCircleRefreshHeaderState` | `backgroundColor`, `contentColor` |
| `SkyProgressRefreshHeader` | `SkyProgressRefreshHeaderState` | `color` |
| `SkyTimeRefreshHeader` | `SkyTimeRefreshHeaderState` | 4 文案 + `timeFormat` |
| `SkyTwoLevelRefreshHeader` | `SkyTwoLevelRefreshHeaderState` | 5 文案 |

**使用示例：**

```kotlin
// 定制球状加载 Header 颜色
header = {
    SkyBallRefreshHeader(
        flag = state.refreshFlag,
        headerState = rememberSkyBallRefreshHeaderState(
            refreshingColor = Color(0xFF33AAFF),
            idleColor = Color(0xFFEEEEEE)
        )
    )
}

// 定制时间文案 Header
header = {
    SkyTimeRefreshHeader(
        state,
        headerState = rememberSkyTimeRefreshHeaderState(
            pullingText = "Pull to refresh",
            releaseText = "Release to refresh",
            refreshingText = "Refreshing...",
            finishedText = "Refresh complete",
            timeFormat = "'Last update:' yyyy-MM-dd HH:mm:ss"
        )
    )
}
```

---

## 7. 使用指南

### 7.1 基础用法

```kotlin
@Composable
fun BasicRefreshScreen() {
    val scope = rememberCoroutineScope()
    val state = rememberSkyRefreshState()
    var items by remember { mutableStateOf(List(20) { "列表项 #${it + 1}" }) }

    SkyRefreshLayout(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onRefresh = {
            scope.launch {
                delay(1500)
                items = items.shuffled()
                state.finish()
            }
        },
        onLoadMore = {
            scope.launch {
                delay(1500)
                val appended = List(5) { "列表项 #${items.size + it + 1}" }
                items = items + appended
                state.finish(noMoreData = items.size >= 50)
            }
        },
        noMoreDataText = "已经到底啦 ~"
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it }) { text ->
                Text(text = text, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

### 7.2 仅下拉刷新

```kotlin
val state = rememberSkyRefreshState(enableLoadMore = false)

SkyRefreshLayout(
    state = state,
    onRefresh = { /* ... */ }
) { /* ... */ }
```

### 7.3 仅上拉加载

```kotlin
val state = rememberSkyRefreshState(enableRefresh = false)

SkyRefreshLayout(
    state = state,
    onLoadMore = { /* ... */ },
    noMoreDataText = "已经到底啦"
) { /* ... */ }
```

### 7.4 横向滑动

```kotlin
SkyRefreshLayout(
    state = state,
    orientation = Orientation.Horizontal,
    onRefresh = { /* ... */ },
    onLoadMore = { /* ... */ }
) {
    LazyRow { /* ... */ }
}
```

### 7.5 自定义指示器

```kotlin
SkyRefreshLayout(
    state = state,
    header = {
        // 自定义 Header：根据 flag 状态驱动 UI
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (state.refreshFlag) {
                    SkyRefreshFlag.IDLE -> "继续下拉"
                    SkyRefreshFlag.PULLING -> "松开立即刷新"
                    SkyRefreshFlag.REFRESHING -> "正在刷新…"
                    SkyRefreshFlag.FINISHING -> "刷新完成"
                }
            )
        }
    },
    footer = { /* 自定义 Footer */ }
) { /* ... */ }
```

### 7.6 程序化触发

```kotlin
// 按钮触发刷新
Button(onClick = { scope.launch { state.autoRefresh() } }) {
    Text("刷新")
}

// 按钮触发加载
Button(onClick = { scope.launch { state.autoLoadMore() } }) {
    Text("加载更多")
}

// 动态开关
Switch(
    checked = state.enableRefresh,
    onCheckedChange = { state.enableRefresh = it }
)
```

### 7.7 下拉进入二楼

```kotlin
var secondFloorOpen by remember { mutableStateOf(false) }

Box(modifier = Modifier.fillMaxSize()) {
    SkyRefreshLayout(
        state = state,
        secondFloorRate = 2f,  // 二级阈值 = headerBound × 2
        onSecondFloor = { secondFloorOpen = true },
        header = { SkyTwoLevelRefreshHeader(state, secondFloorRate = 2f) },
        onRefresh = { /* ... */ }
    ) { /* ... */ }

    // 二楼覆盖层
    AnimatedVisibility(visible = secondFloorOpen) {
        // 二楼内容
    }
}
```

---

## 8. 行为约定

| 约定 | 说明 |
|------|------|
| Footer 侧 1:1 线性跟手 | 上拉加载无阻尼，与拉出方向手感对称 |
| Header 下拉保留阻尼 | 下拉刷新使用 `stickinessLevel` 阻尼系数 |
| 刷新与加载互斥 | 不允许同时处于 REFRESHING 状态 |
| noMoreData 终态 | finish 后 Footer 平滑收起，不常驻视口底部 |
| noMoreDataText 不传 | 终态 UI 整体不渲染（零尺寸，footerBound 归零） |
| autoRefresh/autoLoadMore | 通过 State 内 internal refreshAction/loadMoreAction 派发 |

---

## 9. 构建与依赖

**Gradle 配置：**

```kotlin
// 根 build.gradle.kts
extra["skyBuild.enableCompose"] = true  // SkyBuildLogic 自动注入 Compose BOM + 核心依赖

// 模块 build.gradle.kts
dependencies {
    // Lottie 为 compileOnly 依赖，不传递给消费者
    compileOnly(libs.lottie.compose)
}
```

**本地开发模式：**

```properties
# local.properties
useLocalSkyWidgetCompose=true  # settings 才 include :SkyWidgetComposeLib
```

**Lottie 依赖约定：**

`SkyLottieRefreshHeader` 使用 Lottie 动画，但 Lottie 在库中为 **compileOnly** 依赖（完全不传递给消费者）。使用前需在宿主模块自行导包：

```kotlin
implementation("com.airbnb.android:lottie-compose:6.7.1")
```

组件内部有运行时探测，宿主未导包时抛出 `IllegalStateException` 并提示导包语句。

---

## 10. 资源清单

**字符串资源（`sky_rl_*` 前缀）：**

| 资源名 | 默认值 | 用途 |
|--------|--------|------|
| `sky_rl_loading` | 正在加载 | Footer 加载中文案 |
| `sky_rl_header_pulling` | 下拉开始刷新 | TimeHeader 下拉中文案 |
| `sky_rl_header_release` | 释放立即刷新 | TimeHeader 拉过阈值文案 |
| `sky_rl_header_refreshing` | 正在刷新… | TimeHeader 刷新中文案 |
| `sky_rl_header_finished` | 刷新完成 | TimeHeader 刷新完成文案 |
| `sky_rl_header_secondary` | 释放进入二楼 | TwoLevelHeader 二级阈值文案 |
| `sky_rl_header_time_format` | '最后更新：'M-d HH:mm | TimeHeader 时间格式 |

**颜色资源：**

| 资源名 | 值 | 用途 |
|--------|-----|------|
| `sky_rl_text_title` | #171E2C | 标题文字颜色 |
| `sky_rl_text_tertiary` | #9197A3 | 次要文字颜色 |

**图标资源：**

| 资源名 | 用途 |
|--------|------|
| `sky_icon_refresh_loading` | Header 刷新图标 |
| `sky_icon_load_more_loading` | Footer 加载图标 |

**Lottie 资源：**

| 资源名 | 用途 |
|--------|------|
| `sky_rl_lottie_refresh.json` | Lottie 默认动画 |

---

## 附录

### A. 版本历史

详见 [README_VERSION.md](./README_VERSION.md)

### B. 许可证

MIT License - 详见 [LICENSE](./LICENSE)

### C. 示例代码

完整示例请参考 `app` 模块中的 `RefreshDemoScreens.kt`，包含 13 个详细示例场景。
