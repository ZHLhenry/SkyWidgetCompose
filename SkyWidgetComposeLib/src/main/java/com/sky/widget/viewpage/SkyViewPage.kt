package com.sky.widget.viewpage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.sky.widget.pageIndicator.SkyPageIndexSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 可滑动分页容器。
 *
 * 与 Compose 官方 HorizontalPager 不同，[SkyViewPage] 通过手动测量 + 放置子项实现分页，
 * 支持水平/垂直方向、页面缓存、内容变换（如缩放）以及拖拽事件监听等能力。
 *
 * @param pageCount 总页数；小于等于 0 时不渲染任何内容。
 * @param modifier 作用于分页容器根节点的修饰符。
 * @param state 分页状态控制器，用于读取当前索引、代码翻页、监听偏移量等，见 [rememberSkyViewPageState]。
 * @param orientation 滑动方向，默认水平 [Orientation.Horizontal]。
 * @param userEnable 用户是否可手势滑动；为 false 时手势无效，但仍可通过 [state] 代码翻页。
 * @param pageCache 当前页左右（或上下）各缓存的页数，默认 1，不建议过大，最小应为 1。
 * @param scrollableInteractionSource 拖拽状态监听源，可用于监听用户开始/结束/取消滑动等事件。
 * @param pagerKey 为每一页提供一个稳定 key，用于减少重组、提升性能，效果等同 LazyColumn items 的 key。
 * @param clip 是否对内容区域做滚动裁剪，默认 true。
 * @param contentTransformation 页面内容变换策略，默认不变换，可用 [rememberScalePagerContentTransformation] 实现缩放效果。
 * @param pageAnimationSpec 翻页/回弹动画的时间曲线，默认 [spring]；
 *   消费者可传 tween / spring 等任意 [AnimationSpec] 自定义滑动动画效果。
 * @param content 每一页的 Compose 内容，接收 [SkyViewPageScope] 作为作用域，可通过其 index 拿到当前页索引。
 */
@Composable
fun SkyViewPage(
    pageCount: Int,
    modifier: Modifier = Modifier,
    state: SkyViewPageState = rememberSkyViewPageState(),
    orientation: Orientation = Orientation.Horizontal,
    userEnable: Boolean = true,
    pageCache: Int = 1,
    scrollableInteractionSource: SkyDragInteractionSource? = null,
    pagerKey: (index: Int) -> Any = { it },
    clip: Boolean = true,
    contentTransformation: SkyPagerContentTransformation = NoSkyPagerContentTransformation,
    pageAnimationSpec: AnimationSpec<Float> = spring(),
    content: @Composable SkyViewPageScope.() -> Unit
) {
    // 索引到 key 的映射函数。默认恒等映射；轮播（SkyBanner）会通过 CompositionLocal 注入
    // { it % pageCount }，把内部放大的原始索引映射回真实内容索引，从而实现循环复用。
    val indexToKey: (index: Int) -> Int = LocalIndexToKey.current

    // 已缓存的页面内容集合（key + content），随选中页变化而滚动复用。
    val contentList = remember { mutableStateListOf<SkyViewPageContentBean>() }

    // content 允许缓存的最大数量：当前页 + 左右各 pageCache 页。
    val maxContent by remember(pageCache) { mutableIntStateOf(pageCache * 2 + 1) }

    // 下一次需要被替换的缓存槽位索引；null 表示需要重新推导。
    var nextContentReplaceIndex by remember(pageCache, pageCount) { mutableStateOf<Int?>(null) }

    // 翻页方向标志位，用于驱动 content 缓存的增量更新，默认还原态。
    var isNextPage by remember { mutableStateOf<PageChangeAnimFlag>(PageChangeAnimFlag.Reduction) }

    val coroutineScope = rememberCoroutineScope()

    // 当 pageCount 变化后，校正当前选中索引，避免越界。
    remember(pageCount) {
        // 同步总页数到 state，供指示器等外部组件零配置读取。
        state.pageCountValue = pageCount
        if (pageCount <= 0) {
            // 无有效页面，无需处理。
        } else if (pageCount <= state.getCurrSelectIndex()) {
            // 当前索引超出了新的页数范围，回落到最后一页。
            state.currSelectIndex.value = pageCount - 1
            state.setPageIndex(pageCount - 1)
        }
        0
    }
    // 当前索引仍然越界则直接返回，不渲染。
    if (pageCount <= state.getCurrSelectIndex()) return

    // 首次进入或关键参数变化时，全量初始化 content 缓存。
    remember(pageCount, pageCache, state) {
        state.scope = coroutineScope
        initContentList(
            state = state,
            pageCache = pageCache,
            indexToKey = indexToKey,
            pagerKey = pagerKey,
            contentList = contentList,
            pageCount = pageCount,
            content = content
        )
        0
    }

    // 根据翻页方向增量维护 content 缓存：跳转时全量重建，单页翻动时只替换一个槽位。
    remember(pageCount, isNextPage, pageCache) {
        if (isNextPage is PageChangeAnimFlag.GoToPageNotAnim || isNextPage is PageChangeAnimFlag.GoToPageWithAnim) {
            // 代码跳转到任意页：无法复用相邻缓存，直接全量重建。
            initContentList(
                state = state,
                pageCache = pageCache,
                indexToKey = indexToKey,
                pagerKey = pagerKey,
                contentList = contentList,
                pageCount = pageCount,
                content = content
            )
            nextContentReplaceIndex = null
        }
        if (isNextPage == PageChangeAnimFlag.Next) {
            // 向后翻页：把新出现的“下一页”写入环形缓存的下一个槽位。
            val currIndex = nextContentReplaceIndex?.let {
                if (it >= maxContent - 1) 0 else it + 1
            } ?: 0
            val index = state.getCurrSelectIndex() + pageCache
            val key = indexToKey(index)
            contentList[currIndex] = SkyViewPageContentBean(
                key = getPagerKey(pagerKey, pageCount, key),
                paramModifier = Modifier.layoutId(index),
                paramScope = SkyViewPageScope(key, index)
            ) { mModifier, mScope ->
                // 越界页（缓存产生的负索引或超过 pageCount 的索引）渲染为空 Box 占位。
                if (key !in 0..<pageCount) {
                    Box(modifier = Modifier)
                } else {
                    Box(modifier = mModifier) { mScope.content() }
                }
            }
            nextContentReplaceIndex = currIndex
        } else if (isNextPage == PageChangeAnimFlag.Prev) {
            // 向前翻页：把新出现的“上一页”写入环形缓存的上一个槽位。
            val currIndex = nextContentReplaceIndex ?: (maxContent - 1)
            val index = state.getCurrSelectIndex() - pageCache
            val key = indexToKey(index)
            contentList[currIndex] = SkyViewPageContentBean(
                key = getPagerKey(pagerKey, pageCount, key),
                paramModifier = Modifier.layoutId(index),
                paramScope = SkyViewPageScope(key, index)
            ) { mModifier, mScope ->
                if (key !in 0..<pageCount) {
                    Box(modifier = Modifier)
                } else {
                    Box(modifier = mModifier) { mScope.content() }
                }
            }
            nextContentReplaceIndex = if (currIndex <= 0) maxContent - 1 else currIndex - 1
        }
        // 消费完当前标志位后回到还原态，避免重复触发。
        isNextPage = PageChangeAnimFlag.Reduction
        0
    }

    // 允许滑动到的最小偏移（对应下一页方向边界），用于限制手势拖拽范围。
    val minOffset = remember(state.mainAxisSize, state.currSelectIndex.value, pageCount) {
        val currIndex = state.currSelectIndex.value
        if (currIndex + 1 >= pageCount) {
            // 已是最后一页，不允许继续向后拖。
            currIndex * -state.mainAxisSize.toFloat()
        } else {
            (currIndex + 1) * -state.mainAxisSize.toFloat()
        }
    }
    // 允许滑动到的最大偏移（对应上一页方向边界）。
    val maxOffset = remember(state.mainAxisSize, state.currSelectIndex.value) {
        val currIndex = state.currSelectIndex.value
        if (currIndex <= 0) {
            // 已是第一页，不允许继续向前拖。
            0f
        } else {
            (currIndex - 1) * -state.mainAxisSize.toFloat()
        }
    }

    // 手势滚动状态：把用户拖拽的增量映射到 offset 动画值上，并限制在 [minOffset, maxOffset] 区间内。
    val scrollableState = rememberScrollableState {
        // userEnable 为 false 时直接吞掉增量（同时兼容部分平台的异常回调）。
        if (!userEnable) return@rememberScrollableState it
        // 用户开始手动拖拽，取消正在进行的翻页动画标志。
        state.pageChangeAnimFlag = null
        val lastOffset = state.offsetAnim.value
        val offset = midOf(minOffset, lastOffset + it, maxOffset)
        coroutineScope.launch { state.offsetAnim.snapTo(offset) }
        // 返回真正被消费的增量。
        offset - lastOffset
    }

    // 处理各类翻页动画：上一页/下一页/还原/无动画跳转/有动画跳转。
    LaunchedEffect(state.pageChangeAnimFlag) {
        val flag = state.pageChangeAnimFlag
        if (flag == null) {
            // 没有待处理的翻页请求，停掉残留动画即可。
            if (state.offsetAnim.isRunning) state.offsetAnim.stop()
            return@LaunchedEffect
        }
        // 用于“有动画跳转”时把一次长距离跳转拆成一次相邻翻页的续接标志。
        var returnPageChangeAnimFlag: PageChangeAnimFlag? = null
        try {
            val index = state.currSelectIndex.value
            when (flag) {
                PageChangeAnimFlag.Prev -> {
                    if (index <= 0) {
                        // 已在首页，回弹到原位。
                        state.offsetAnim.animateTo(-index * state.mainAxisSize.toFloat(), pageAnimationSpec)
                        return@LaunchedEffect
                    }
                    try {
                        state.offsetAnim.animateTo(-(index - 1) * state.mainAxisSize.toFloat(), pageAnimationSpec)
                    } finally {
                        // 动画结束（或被打断）后同步选中索引并触发缓存前移。
                        state.currSelectIndex.value = index - 1
                        isNextPage = PageChangeAnimFlag.Prev
                    }
                }

                PageChangeAnimFlag.Next -> {
                    if (index + 1 >= pageCount) {
                        // 已在末页，回弹到原位。
                        state.offsetAnim.animateTo(-index * state.mainAxisSize.toFloat(), pageAnimationSpec)
                        return@LaunchedEffect
                    }
                    try {
                        state.offsetAnim.animateTo(-(index + 1) * state.mainAxisSize.toFloat(), pageAnimationSpec)
                    } finally {
                        state.currSelectIndex.value = index + 1
                        isNextPage = PageChangeAnimFlag.Next
                    }
                }

                PageChangeAnimFlag.Reduction -> {
                    // 未超过翻页阈值，回弹到当前页。
                    state.offsetAnim.animateTo(-index * state.mainAxisSize.toFloat(), pageAnimationSpec)
                }

                is PageChangeAnimFlag.GoToPageNotAnim -> {
                    // 无动画直接跳到目标页。
                    state.currSelectIndex.value = flag.index
                    state.offsetAnim.snapTo(-flag.index * state.mainAxisSize.toFloat())
                    isNextPage = flag
                }

                is PageChangeAnimFlag.GoToPageWithAnim -> {
                    // 有动画跳转：先把索引移动到目标页的相邻页，再触发一次相邻翻页动画，
                    // 从而复用统一的翻页动画逻辑，避免跨多页的复杂插值。
                    val (goToIndex, nextFlag) = if (flag.index > state.currSelectIndex.value) {
                        flag.index - 1 to PageChangeAnimFlag.Next
                    } else {
                        flag.index + 1 to PageChangeAnimFlag.Prev
                    }
                    state.currSelectIndex.value = goToIndex
                    state.offsetAnim.snapTo(-goToIndex * state.mainAxisSize.toFloat())
                    returnPageChangeAnimFlag = nextFlag
                    isNextPage = flag
                }
            }
        } finally {
            // 若为有动画跳转，续接触发相邻翻页；否则清空标志。
            state.pageChangeAnimFlag = returnPageChangeAnimFlag
        }
    }

    // 测量并放置各页面子项。
    Layout(
        content = {
            contentList.forEach {
                // 以稳定 key 包裹，减少无关页面的重组。
                key(it.key) {
                    // 变换策略以 Modifier 扩展形式提供，需在 with 作用域内调用。
                    val transformedModifier = with(contentTransformation) {
                        it.paramModifier.transform(state, it.paramScope)
                    }
                    it.function(transformedModifier, it.paramScope)
                }
            }
        },
        modifier = modifier
            .scrollable(
                state = scrollableState,
                orientation = orientation,
                enabled = userEnable,
                interactionSource = scrollableInteractionSource,
                flingBehavior = remember<FlingBehavior>(orientation) {
                    object : FlingBehavior {
                        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                            // 松手时根据当前偏移 + 惯性速度判断应翻到上一页、下一页还是回弹。
                            val index = state.currSelectIndex.value
                            val half = state.mainAxisSize / 2
                            if (state.offsetAnim.value + initialVelocity > -(index * state.mainAxisSize - half)) {
                                state.pageChangeAnimFlag = PageChangeAnimFlag.Prev
                            } else if (state.offsetAnim.value + initialVelocity < -(index * state.mainAxisSize + half)) {
                                state.pageChangeAnimFlag = PageChangeAnimFlag.Next
                            } else {
                                state.pageChangeAnimFlag = PageChangeAnimFlag.Reduction
                            }
                            // 返回 0 表示速度已被完全消费，不再继续惯性滚动。
                            return 0f
                        }
                    }
                })
            .runIf(clip) { clipScrollableContainer(orientation) }
            .onSizeChanged {
                if (state.size != it) {
                    state.size = it
                    // 尺寸变化（如窗口大小改变）后，滚动回当前页的正确位置。
                    coroutineScope.launch { state.setOffset(0f) }
                }
            }
    ) { measurableList, constraints ->
        val selectIndex = state.currSelectIndex.value
        var width = 0
        var height = 0
        // 仅测量处于缓存范围内（|index - selectIndex| <= pageCache）的有效子项，并记录最大宽高。
        val placeableList = measurableList
            .filter {
                val id = it.layoutId
                id is Int && abs(id - selectIndex) <= pageCache
            }
            .map {
                val id = it.layoutId as Int
                val placeable = it.measure(constraints)
                width = maxOf(width, placeable.width)
                height = maxOf(height, placeable.height)
                id to placeable
            }

        // 主轴尺寸：水平方向取宽度，垂直方向取高度。
        state.mainAxisSize = if (orientation == Orientation.Horizontal) width else height

        layout(width, height) {
            // 当前动画偏移量（取整避免像素抖动）。
            val animValue = state.offsetAnim.value.roundToInt()
            placeableList.forEach { (index, placeable) ->
                // 每页在主轴上的位置 = 页索引 * 主轴尺寸 + 动画偏移。
                val offset = index * state.mainAxisSize + animValue
                if (orientation == Orientation.Horizontal) {
                    // placeRelative 可适配从右到左（RTL）布局。
                    placeable.placeRelative(x = offset, y = 0)
                } else {
                    placeable.placeRelative(x = 0, y = offset)
                }
            }
        }
    }
}

/**
 * 全量初始化 content 缓存列表。
 *
 * 以当前选中页为中心，生成 [selectIndex - pageCache, selectIndex + pageCache] 范围内的页面内容，
 * 越界页（负索引或超过 pageCount）会以空 Box 占位，保证滑动手感一致。
 */
private fun initContentList(
    state: SkyViewPageState,
    pageCache: Int,
    indexToKey: (index: Int) -> Int,
    pagerKey: (index: Int) -> Any,
    contentList: MutableList<SkyViewPageContentBean>,
    pageCount: Int,
    content: @Composable (SkyViewPageScope.() -> Unit)
) {
    contentList.clear()
    val selectIndex = state.currSelectIndex.value
    // 需要缓存的索引集合：key to index。
    val keyList = (selectIndex - pageCache).rangeTo(selectIndex + pageCache)
        .map { indexToKey(it) to it }

    keyList.forEach { (key, value) ->
        contentList.add(
            SkyViewPageContentBean(
                key = getPagerKey(pagerKey, pageCount, key),
                paramModifier = Modifier.layoutId(value),
                paramScope = SkyViewPageScope(key, value)
            ) { mModifier, mScope ->
                if (key !in 0..<pageCount) {
                    Box(modifier = Modifier)
                } else {
                    Box(modifier = mModifier) { mScope.content() }
                }
            }
        )
    }
}

/**
 * 由于存在 pageCache，会产生越界索引，此处针对越界索引直接以其自身作为 key，
 * 有效索引才走外部传入的 [pagerKey]。
 */
private fun getPagerKey(
    pagerKey: (index: Int) -> Any,
    pageCount: Int,
    index: Int,
): Any = if (index < 0 || index >= pageCount) index else pagerKey(index)

/**
 * 返回区间中值：把 [number] 限制在 [min, max] 之间。
 */
private fun midOf(min: Float, number: Float, max: Float): Float = maxOf(min, minOf(number, max))

/**
 * 条件执行扩展：[useBlock] 为 true 时返回 [block] 的结果，否则原样返回 this。
 */
@OptIn(ExperimentalContracts::class)
private inline fun <T> T.runIf(useBlock: Boolean, block: T.() -> T): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return if (useBlock) block(this) else this
}

/**
 * 索引到 key 的映射 CompositionLocal，默认恒等映射。
 *
 * 作为分页内容索引的扩展点：轮播组件 [com.sky.widget.banner.SkyBanner] 会将其覆盖为
 * `{ it % pageCount }`，从而把内部放大的原始索引映射回真实内容索引，实现无限循环轮播。
 * 声明为 internal 以便同模块内的轮播组件注入映射。
 */
internal val LocalIndexToKey = compositionLocalOf<(index: Int) -> Int> { { it } }

/**
 * [SkyViewPage] 每一页的 Compose 作用域。
 *
 * @property index 当前 content 所在的（映射后）索引，业务侧一般用它区分页面。
 * @property realIndex 真实索引，供内容变换等内部逻辑计算相对偏移使用。
 */
@Immutable
class SkyViewPageScope(
    val index: Int,
    internal val realIndex: Int,
)

/**
 * content 缓存条目，保存一页对应的 key、修饰符、作用域及内容构建函数。
 *
 * @property key Compose 作用域 key，用于稳定 remember 作用域、减少重组。
 * @property paramModifier 传递给内容根节点的修饰符（含 layoutId）。
 * @property paramScope 该页对应的 [SkyViewPageScope]。
 * @property function 内容构建函数，接收最终修饰符与作用域。
 */
@Immutable
internal data class SkyViewPageContentBean(
    val key: Any,
    val paramModifier: Modifier,
    val paramScope: SkyViewPageScope,
    val function: @Composable (Modifier, SkyViewPageScope) -> Unit
)

/**
 * 内部翻页标志位，驱动翻页动画与 content 缓存的增量维护。
 */
internal sealed class PageChangeAnimFlag {
    /** 翻到下一页。 */
    data object Next : PageChangeAnimFlag()

    /** 翻到上一页。 */
    data object Prev : PageChangeAnimFlag()

    /** 未达翻页阈值，回弹还原 offset。 */
    data object Reduction : PageChangeAnimFlag()

    /** 跳转到指定页，无动画。 */
    class GoToPageNotAnim(val index: Int) : PageChangeAnimFlag()

    /** 跳转到指定页，有动画。 */
    class GoToPageWithAnim(val index: Int) : PageChangeAnimFlag()
}

/**
 * 拖拽交互监听源。
 *
 * 实现 [MutableInteractionSource]，将滚动过程中产生的 [DragInteraction] 事件回调给 [dragInteractionChange]，
 * 可用于监听用户开始拖拽、拖拽结束、拖拽取消等时机。
 */
@Immutable
class SkyDragInteractionSource(
    private val dragInteractionChange: (DragInteraction) -> Unit
) : MutableInteractionSource {
    override val interactions: Flow<Interaction>
        get() = flowOf()

    override suspend fun emit(interaction: Interaction) {
        (interaction as? DragInteraction)?.let(dragInteractionChange)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        (interaction as? DragInteraction)?.let(dragInteractionChange)
        return true
    }
}

/**
 * [SkyViewPage] 的状态控制器。
 *
 * 持有当前选中索引、偏移动画、翻页标志、主轴尺寸等内部状态，并对外提供读取索引、
 * 代码翻页、设置偏移量、创建偏移量 Flow 等能力。
 */
@Stable
class SkyViewPageState : SkyPageIndexSource {

    /** 当前 [SkyViewPage] 选中的索引。 */
    internal val currSelectIndex: MutableState<Int> = mutableIntStateOf(0)

    /** 当前分页总页数，由 [SkyViewPage] 在组合时同步写入，供指示器等外部读取。 */
    internal var pageCountValue: Int = 0

    /** 实现 [SkyPageIndexSource]：总页数。 */
    override val pageCount: Int get() = pageCountValue

    /** 实现 [SkyPageIndexSource]：当前选中页索引的 [State]，随翻页自动更新。 */
    override val currentIndexState: State<Int> get() = currSelectIndex

    /** 拖拽/翻页偏移动画实现。 */
    internal val offsetAnim = Animatable(0f)

    /** 翻页标志位，null 表示无待处理的翻页请求。 */
    internal var pageChangeAnimFlag by mutableStateOf<PageChangeAnimFlag?>(null)

    /** 主轴方向的尺寸（水平为宽、垂直为高）。 */
    internal var mainAxisSize by mutableStateOf(0)

    /** 记录当前 size，尺寸变化后自动滚动到正确位置（适配可改变窗口大小的场景）。 */
    internal var size by mutableStateOf(IntSize.Zero)

    /** 关联的协程作用域，用于驱动异步翻页动画。 */
    internal var scope: CoroutineScope? = null

    /** 获取当前选中索引。 */
    fun getCurrSelectIndex(): Int = currSelectIndex.value

    /** 获取当前选中索引的 [State] 对象，便于在 Composable 中订阅。 */
    fun getCurrSelectIndexState(): State<Int> = currSelectIndex

    /** 创建当前选中索引的 [Flow]，便于在非 Composable 场景下监听索引变化。 */
    fun createCurrSelectIndexFlow(): Flow<Int> = snapshotFlow { currSelectIndex.value }

    /** 翻页动画是否正在执行。 */
    fun isAnimRunning(): Boolean = offsetAnim.isRunning

    /** 获取偏移量的 [State] 对象。 */
    fun getOffsetState(): State<Float> = offsetAnim.asState()

    /**
     * 创建子项偏移比例的 [Flow]。
     * 值为 -(偏移百分比 + 当前索引)，可用于联动指示器等外部动画。
     */
    fun createChildOffsetPercentFlow(): Flow<Float> = snapshotFlow {
        val size = mainAxisSize
        if (size == 0) {
            0f
        } else {
            val percent = offsetAnim.value / size
            0 - (percent + getCurrSelectIndex())
        }
    }

    /** 无动画切换到指定索引。 */
    fun setPageIndex(index: Int) {
        pageChangeAnimFlag = PageChangeAnimFlag.GoToPageNotAnim(index)
    }

    /** 有动画切换到指定索引。 */
    fun setPageIndexWithAnimate(index: Int) {
        pageChangeAnimFlag = null
        scope?.launch {
            // 等待一帧，确保上一次的标志位被消费后再写入新的翻页请求。
            withFrameNanos {}
            pageChangeAnimFlag = if (index == currSelectIndex.value) {
                PageChangeAnimFlag.Reduction
            } else {
                PageChangeAnimFlag.GoToPageWithAnim(index)
            }
        }
    }

    /** 设置偏移量（无动画），offset 为相对于整体内容的绝对偏移。 */
    suspend fun setOffset(offset: Float) {
        offsetAnim.snapTo(offset - mainAxisSize * getCurrSelectIndex())
    }

    /** 设置偏移量（有动画），offset 为相对于整体内容的绝对偏移。 */
    suspend fun setOffsetWithAnimate(offset: Float) {
        offsetAnim.animateTo(offset - mainAxisSize * getCurrSelectIndex())
    }

    /** 以主轴尺寸的百分比设置偏移量（无动画）。 */
    suspend fun setOffsetPercent(percent: Float) {
        setOffset(percent * mainAxisSize)
    }

    /** 获取当前索引的偏移百分比。 */
    fun getCurrentOffsetPercent(): Float = getOffsetPercent(getCurrSelectIndex())

    /** 获取指定索引的偏移百分比：0 表示正好对齐，非 0 表示相对该页的偏移量。 */
    fun getOffsetPercent(index: Int): Float {
        if (mainAxisSize == 0) return 0f
        return ((index * -mainAxisSize) - offsetAnim.value) / mainAxisSize
    }
}

/**
 * 创建并记住一个 [SkyViewPageState]。
 */
@Composable
fun rememberSkyViewPageState(): SkyViewPageState = remember { SkyViewPageState() }

/**
 * [SkyViewPage] 的页面内容变换策略。
 *
 * 通过当前页与目标页的相对偏移百分比，为每一页生成对应的 [Modifier]，
 * 常用于实现页面缩放、透明度渐变等视差效果。
 *
 * 变换方法均设计为 [Modifier] 的成员扩展函数，符合 Compose 对 Modifier 工厂的规范，
 * 调用时需在实现实例的 `with` 作用域内进行，例如：
 * `with(transformation) { modifier.transform(state, scope) }`。
 */
@Immutable
interface SkyPagerContentTransformation {

    /** 只对 ±n 以内的页面做变换，用于提升性能，默认 2。 */
    fun effectivePageNumber(): Int = 2

    /**
     * 结合 state 与 scope 计算变换后的 Modifier。
     * 默认只对 [effectivePageNumber] 范围内的页面做变换，超出范围的页面直接返回原 Modifier。
     */
    fun Modifier.transform(state: SkyViewPageState, scope: SkyViewPageScope): Modifier {
        if (abs(scope.realIndex - state.getCurrSelectIndex()) > effectivePageNumber()) return this
        return transform(state.getOffsetPercent(scope.realIndex))
    }

    /** 根据 content 距离当前位置的偏移百分比 [percent] 生成变换后的 Modifier。 */
    fun Modifier.transform(percent: Float): Modifier
}

/**
 * 不做任何变换的默认实现。
 */
internal object NoSkyPagerContentTransformation : SkyPagerContentTransformation {
    override fun Modifier.transform(state: SkyViewPageState, scope: SkyViewPageScope): Modifier = this

    override fun Modifier.transform(percent: Float): Modifier = this
}

/**
 * 缩放变换：距离当前页越远，缩放值越接近 [minScale]。
 *
 * @property maxScale 当前页（percent = 0）时的最大缩放值。
 * @property minScale 远离当前页时的最小缩放值。
 */
class ScalePagerContentTransformation(
    private val maxScale: Float,
    private val minScale: Float,
) : SkyPagerContentTransformation {
    override fun Modifier.transform(percent: Float): Modifier {
        // percent 为 0 时取 maxScale，随 |percent| 增大线性趋近 minScale。
        val scale = (minScale - maxScale) * abs(percent) + maxScale
        return this.scale(scale)
    }
}

/**
 * 创建并记住一个 [ScalePagerContentTransformation]。
 *
 * @param maxScale 当前页的最大缩放值。
 * @param minScale 远离当前页时的最小缩放值。
 */
@Composable
fun rememberScalePagerContentTransformation(maxScale: Float, minScale: Float): ScalePagerContentTransformation =
    remember(maxScale, minScale) { ScalePagerContentTransformation(maxScale, minScale) }
