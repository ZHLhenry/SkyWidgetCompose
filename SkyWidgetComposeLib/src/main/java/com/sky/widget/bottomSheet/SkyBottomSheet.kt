package com.sky.widget.bottomSheet

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @Class: SkyBottomSheet
 * @Author: Henry
 * @Date: 2026/9/16 10:31
 * @Description: 底部弹窗，基于 Material3 ModalBottomSheet 实现（模态弹窗，带遮罩，
 * 支持下滑手势与返回键关闭），通过 [SkyBottomSheetState] 控制展示、隐藏与锚点切换
 *
 * 使用示例：
 * ```
 * val state = rememberBottomSheetState()
 * val scope = rememberCoroutineScope()
 * SkyBottomSheet(state = state) {
 *     // 弹窗内容
 * }
 * // 展示：scope.launch { state.show() }
 * // 隐藏：scope.launch { state.hide() }
 * ```
 *
 * @param modifier 弹窗内容容器修饰符
 * @param state 弹窗状态，通过 [rememberBottomSheetState] 创建
 * @param sheetBackgroundColor 弹窗容器背景色，默认透明（由内容自行绘制背景）
 * @param sheetCornerRadius 弹窗顶部圆角半径，默认 28.dp（Material3 默认值）；传 0.dp 为直角
 * @param content 弹窗内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyBottomSheet(
    modifier: Modifier = Modifier,
    state: SkyBottomSheetState = rememberBottomSheetState(),
    sheetBackgroundColor: Color = Color.Transparent,
    sheetCornerRadius: Dp = 28.dp,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    LaunchedEffect(state, sheetState) {
        state.sheetState = sheetState
    }
    // Material3 弹窗仅在展示时参与组合，隐藏后需从组合中移除
    if (state.visible) {
        ModalBottomSheet(
            onDismissRequest = { state.visible = false },
            modifier = modifier,
            sheetState = sheetState,
            containerColor = sheetBackgroundColor,
            shape = RoundedCornerShape(
                topStart = sheetCornerRadius,
                topEnd = sheetCornerRadius
            ),
            dragHandle = null,
        ) {
            content()
        }
        // ModalBottomSheet 进入组合时默认动画展开到半屏锚点，这里按 initialValue 校正展示位置
        LaunchedEffect(sheetState) {
            if (state.initialValue == SkyBottomSheetValue.Expanded) {
                sheetState.expand()
            }
        }
    }
}

/**
 * 创建并记住 [SkyBottomSheetState]，配置变化后自动恢复初始展示状态
 *
 * @param initialValue 初始展示状态，默认 [SkyBottomSheetValue.Hidden] 不展示
 * @return 可跨配置变化保存的 [SkyBottomSheetState] 实例
 */
@Composable
fun rememberBottomSheetState(
    initialValue: SkyBottomSheetValue = SkyBottomSheetValue.Hidden
): SkyBottomSheetState = rememberSaveable(saver = SkyBottomSheetState.SAVER) {
    SkyBottomSheetState(
        initialValue = initialValue,
    )
}

/**
 * 底部弹窗状态，控制 [SkyBottomSheet] 的展示、隐藏与锚点切换。
 *
 * 控制方法均为挂起函数（含动画），需在协程中调用，如 `rememberCoroutineScope().launch { ... }`
 *
 * @param initialValue 初始展示状态
 */
@OptIn(ExperimentalMaterial3Api::class)
open class SkyBottomSheetState(
    val initialValue: SkyBottomSheetValue,
) {

    internal var sheetState: SheetState? = null

    internal var visible by mutableStateOf(initialValue != SkyBottomSheetValue.Hidden)

    /**
     * 是否处于展示状态（含进入/退出动画过程）；下滑手势或返回键关闭后置为 false
     */
    val isVisible: Boolean
        get() = visible

    /**
     * 动画展开弹窗。
     *
     * 弹窗未展示时先加入组合再执行展开动画；已展示时动画回到默认锚点
     * （内容高度超过半屏时为 [SkyBottomSheetValue.HalfExpanded]，否则为 [SkyBottomSheetValue.Expanded]）；
     * 初始锚点为 [SkyBottomSheetValue.Expanded] 时展开到全展开
     */
    suspend fun show() {
        if (visible) {
            sheetState?.show()
        } else {
            visible = true
        }
    }

    /**
     * 动画切换到指定锚点
     *
     * @param targetValue 目标锚点。[SkyBottomSheetValue.Hidden] 等同 [hide]；
     * [SkyBottomSheetValue.HalfExpanded] 仅在弹窗内容高度超过半屏时生效，否则无动画
     */
    suspend fun animateTo(targetValue: SkyBottomSheetValue) {
        when (targetValue) {
            SkyBottomSheetValue.Hidden -> hide()
            SkyBottomSheetValue.HalfExpanded -> sheetState?.partialExpand()
            SkyBottomSheetValue.Expanded -> sheetState?.expand()
        }
    }

    /**
     * 动画隐藏弹窗，动画结束后将弹窗从组合中移除；弹窗未展示时调用无动画、直接移除
     */
    suspend fun hide() {
        val sheet = sheetState
        // 锚点未加载（弹窗尚未完成测量）时直接移除，避免无锚点动画异常
        if (sheet != null && sheet.hasExpandedState) {
            sheet.hide()
        }
        visible = false
    }

    companion object {
        /**
         * [rememberSaveable] 状态保存器，保存并恢复初始展示状态
         */
        val SAVER: Saver<SkyBottomSheetState, *> = Saver(
            save = {
                it.initialValue
            },
            restore = {
                SkyBottomSheetState(it)
            }
        )
    }
}

/**
 * 底部弹窗展示锚点
 */
enum class SkyBottomSheetValue {

    /** 隐藏 */
    Hidden,

    /** 全展开 */
    Expanded,

    /** 半屏展开 */
    HalfExpanded
}
