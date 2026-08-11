package com.sky.widget.stateLayout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 页面状态容器：根据 [pageState] 自动在 加载 / 成功 / 空 / 错误 之间切换。
 *
 * 成功时渲染 [content]，其余状态渲染对应占位（均支持自定义与重试）。
 *
 * ## 基础用法
 *
 * 在 ViewModel 中维护一个 [SkyPageState]：
 *
 * ```kotlin
 * class HomeViewModel : ViewModel() {
 *     var pageState by mutableStateOf<SkyPageState>(SkyPageState.Loading)
 *         private set
 *
 *     var datas by mutableStateOf<List<Article>>(emptyList())
 *         private set
 *
 *     init { load() }
 *
 *     fun load() {
 *         pageState = SkyPageState.Loading
 *         viewModelScope.launch {
 *             runCatching { repository.fetchArticles() }
 *                 .onSuccess { list ->
 *                     datas = list
 *                     pageState = if (list.isEmpty()) {
 *                         SkyPageState.Empty()
 *                     } else {
 *                         SkyPageState.Success
 *                     }
 *                 }
 *                 .onFailure { e ->
 *                     pageState = SkyPageState.Error(e.message ?: "加载失败")
 *                 }
 *         }
 *     }
 * }
 * ```
 *
 * 在 Screen 中使用：
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
 *     val state by viewModel
 *
 *     SkyPageStateLayout(
 *         pageState = state.pageState,
 *         onEmptyRetry = { viewModel.load() },
 *         onErrorRetry = { viewModel.load() }
 *     ) {
 *         LazyColumn {
 *             items(state.datas) { article ->
 *                 ArticleItem(article)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## 自定义占位
 *
 * ```kotlin
 * SkyPageStateLayout(
 *     pageState = state.pageState,
 *     onEmptyRetry = { viewModel.load() },
 *     onErrorRetry = { viewModel.load() },
 *     empty = { message ->
 *         SkyEmptyWidget(
 *             image = {
 *                 Icon(
 *                     imageVector = Icons.Outlined.Inbox,
 *                     contentDescription = null,
 *                     modifier = Modifier.size(64.dp),
 *                     tint = MaterialTheme.colorScheme.outline
 *                 )
 *             },
 *             message = message.ifEmpty { "暂无数据" },
 *             onRetry = { viewModel.load() }
 *         )
 *     },
 *     error = { message ->
 *         SkyErrorWidget(
 *             message = message,
 *             buttonText = "重新加载",
 *             onRetry = { viewModel.load() }
 *         )
 *     }
 * ) {
 *     ArticleList(state.datas)
 * }
 * ```
 *
 * @param pageState 当前页面状态
 * @param modifier 外层修饰符
 * @param onEmptyRetry 空态占位按钮重试回调（默认空实现）
 * @param onErrorRetry 错误态占位按钮重试回调（默认空实现）
 * @param loading 加载态自定义内容，默认 [SkyLoadingWidget]
 * @param empty 空态自定义内容，默认 [SkyEmptyWidget]
 * @param error 错误态自定义内容，默认 [SkyErrorWidget]
 * @param content 成功态内容
 */
@Composable
fun SkyPageStateLayout(
    pageState: SkyPageState,
    modifier: Modifier = Modifier,
    onEmptyRetry: () -> Unit = {},
    onErrorRetry: () -> Unit = {},
    loading: @Composable () -> Unit = { SkyLoadingWidget() },
    empty: @Composable (message: String) -> Unit = {
        SkyEmptyWidget(message = it, onRetry = onEmptyRetry)
    },
    error: @Composable (message: String) -> Unit = {
        SkyErrorWidget(message = it, onRetry = onErrorRetry)
    },
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        when (pageState) {
            SkyPageState.Loading -> loading()
            is SkyPageState.Empty -> empty(pageState.message)
            is SkyPageState.Error -> error(pageState.message)
            SkyPageState.Success -> content()
        }
    }
}
