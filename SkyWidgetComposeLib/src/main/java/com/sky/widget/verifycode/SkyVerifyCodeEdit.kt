package com.sky.widget.verifycode
import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DEL
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "SkyVerifyCodeEdit"

/** 未输入位线条的默认透明度（对齐 Material 规范中禁用态内容的 38% 透明度） */
private const val NORMAL_LINE_ALPHA = 0.38f

/**
 * 验证码输入框组件：支持下划线 / 正方形两种格子样式，点击唤起数字键盘，输满自动回调。
 *
 * 基于 [Canvas] 绘制格子、数字与光标；通过 [PlatformTextInputModifierNode] 与输入法建立会话，
 * 接收提交的内容（仅接受数字，超长部分自动截断），删除键回退一位，光标在下一个待输入位闪烁。
 *
 * 使用示例：
 * ```kotlin
 * // 基本用法（默认 6 位下划线样式）
 * SkyVerifyCodeEdit(onComplete = { code -> /* 提交验证码 */ })
 *
 * // 正方形格子 + 自定义数量
 * SkyVerifyCodeEdit(
 *     count = 4,
 *     type = SkyVerifyType.Square,
 *     onComplete = { code -> /* 提交验证码 */ }
 * )
 * ```
 *
 * @param modifier 外层修饰符
 * @param count 验证码位数
 * @param space 相邻格子间距
 * @param size 单个格子尺寸（宽、高）
 * @param lineHeight 线条粗细（下划线高度 / 正方形边框宽度）
 * @param textFontSize 数字字号
 * @param activeLineColor 已输入位及当前输入位的线条颜色
 * @param textColor 数字颜色，默认跟随 [activeLineColor]
 * @param normalLineColor 未输入位的线条颜色，默认为 [activeLineColor] 的 [NORMAL_LINE_ALPHA] 透明度
 * @param cursorLineWidth 光标宽度
 * @param cursorLineHeight 光标高度；默认 [Dp.Unspecified]，按格子高度 × [cursorLineHeightRatio] 计算
 * @param cursorLineHeightRatio 光标高度占格子高度的比例，仅 [cursorLineHeight] 未指定时生效
 * @param cursorLineColor 光标颜色
 * @param type 格子样式，[SkyVerifyType.BottomLine] 下划线 / [SkyVerifyType.Square] 正方形
 * @param keyboardType 软键盘类型，默认 [KeyboardType.Number]；
 * 注意：传 [KeyboardType.NumberPassword] 会在华为等机型上触发系统安全键盘，
 * 安全键盘不遵循标准 InputConnection 协议（删除键不下发按键事件与编辑命令），会导致删除失效
 * @param onComplete 输满 [count] 位时的回调，参数为完整验证码
 */
@Composable
fun SkyVerifyCodeEdit(
    modifier: Modifier = Modifier,
    count: Int = 6,
    space: Dp = 10.dp,
    size: DpSize = DpSize(40.dp, 40.dp),
    lineHeight: Dp = 2.dp,
    textFontSize: TextUnit = 32.sp,
    activeLineColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = activeLineColor,
    normalLineColor: Color = activeLineColor.copy(alpha = NORMAL_LINE_ALPHA),
    cursorLineWidth: Dp = 2.dp,
    cursorLineHeight: Dp = Dp.Unspecified,
    cursorLineHeightRatio: Float = 0.4f,
    cursorLineColor: Color = Color.Black,
    type: SkyVerifyType = SkyVerifyType.BottomLine,
    keyboardType: KeyboardType = KeyboardType.Number,
    onComplete: (text: String) -> Unit,
) {
    val spacePx = with(LocalDensity.current) { space.toPx() }
    val lineHeightPx = with(LocalDensity.current) { lineHeight.toPx() }
    val cursorLineWidthPx = with(LocalDensity.current) { cursorLineWidth.toPx() }
    val textFontSizePx = with(LocalDensity.current) { textFontSize.toPx() }
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlphaState by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            repeatMode = RepeatMode.Reverse,
            animation = tween(easing = LinearEasing, delayMillis = 300)
        )
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textState = remember { mutableStateOf("") }
    var text by textState
    var hasFocus by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // 文字画笔：颜色或字号变化时重建，保证绘制参数实时生效
    val paint = remember(textColor, textFontSizePx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textSize = textFontSizePx
            textAlign = Paint.Align.CENTER
        }
    }
    val boxWidthPx = with(LocalDensity.current) { size.width.toPx() }
    val boxHeightPx = with(LocalDensity.current) { size.height.toPx() }
    val maxWidth = size.width * count + space * (count - 1)
    // 会话内回调可能晚于重组发生，通过 rememberUpdatedState 保证读取到最新的参数实例
    val currentOnCompleteState = rememberUpdatedState(onComplete)
    val currentCountState = rememberUpdatedState(count)
    val currentKeyboardTypeState = rememberUpdatedState(keyboardType)
    // 输入会话节点：与平台 IME 建立输入连接（以当前文本初始化，光标置于末尾），
    // 保证 IME 侧文本模型与组件状态一致 —— 若 IME 认为无内容可删，删除键将不下发任何事件
    val inputNode = remember {
        VerifyCodeInputNode(
            textProvider = { textState.value },
            keyboardTypeProvider = { currentKeyboardTypeState.value },
            onCommitText = { commit ->
                // 仅接受数字提交，超出 count 的部分截断；输满后回调 onComplete
                if (commit.isDigitsOnly() && textState.value.length < currentCountState.value) {
                    textState.value = (textState.value + commit).take(currentCountState.value)
                    if (textState.value.length == currentCountState.value) {
                        currentOnCompleteState.value(textState.value)
                    }
                }
            },
            onDeleteText = { length ->
                textState.value = textState.value.dropLast(length.coerceAtMost(textState.value.length))
            },
            onImeDone = { keyboardController?.hide() },
        )
    }
    // 键盘弹出时若组件持有焦点，自动滚动到可见区域，避免被 IME 遮挡
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && hasFocus) {
            bringIntoViewRequester.bringIntoView()
        }
    }
    Canvas(
        modifier = Modifier
            .width(maxWidth)
            .height(size.height)
            .focusRequester(focusRequester)
            .then(VerifyCodeInputElement(inputNode))
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { state ->
                hasFocus = state.isFocused
                if (state.isFocused) {
                    // 获取焦点后启动输入法会话：数字键盘 + Done 动作
                    inputNode.launchSession()
                }
            }
            .focusTarget()
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                // 监听删除键：回退一位并消费事件，避免继续向下传递
                val nativeKeyEvent = keyEvent.nativeKeyEvent
                if (nativeKeyEvent.action == ACTION_DOWN && nativeKeyEvent.keyCode == KEYCODE_DEL) {
                    text = text.dropLast(1)
                    return@onPreviewKeyEvent true
                }
                false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        focusRequester.requestFocus()
                        // 重启输入会话：收起键盘后系统侧输入连接可能已失效，
                        // 重新建立连接会以当前文本同步 IME 并重新唤起键盘，保证删除键可用
                        inputNode.launchSession()
                    })
            }
            .then(modifier)
    ) {
        // 光标高度：未显式指定时按格子高度 × cursorLineHeightRatio 计算
        val cursorLineHeightPx =
            if (cursorLineHeight == Dp.Unspecified) boxHeightPx * cursorLineHeightRatio else cursorLineHeight.toPx()
        // 绘制格子（下划线 / 正方形边框），已输入位与当前输入位用高亮色
        repeat(count) { index ->
            when (type) {
                SkyVerifyType.BottomLine -> {
                    drawLine(
                        color = if (index <= text.length) activeLineColor else normalLineColor,
                        start = Offset(
                            (boxWidthPx + spacePx) * index,
                            boxHeightPx - lineHeightPx
                        ),
                        end = Offset(
                            (boxWidthPx + spacePx) * index + boxWidthPx,
                            boxHeightPx - lineHeightPx
                        ),
                        strokeWidth = lineHeightPx
                    )

                }
                SkyVerifyType.Square -> {
                    drawRect(
                        color = if (index <= text.length) activeLineColor else normalLineColor,
                        topLeft = Offset(
                            (boxWidthPx + spacePx) * index,
                            0f,
                        ),
                        size = Size(boxWidthPx, boxHeightPx - lineHeightPx),
                        style = Stroke(width = lineHeightPx)
                    )
                }
            }
        }

        // 绘制已输入的数字：基线按字体度量换算，保证垂直居中
        repeat(text.length) { index ->
            drawIntoCanvas { canvas ->
                val textStr = text[index].toString()
                val rect = Rect(
                    ((boxWidthPx + spacePx) * index).toInt(), 0,
                    ((boxWidthPx + spacePx) * index + boxWidthPx).toInt(), boxHeightPx.toInt()
                )
                val metrics = paint.fontMetrics
                val distance = (metrics.descent - metrics.ascent) / 2 - metrics.descent
                val baseline = rect.centerY() + distance
                canvas.nativeCanvas.drawText(
                    textStr,
                    rect.centerX().toFloat(),
                    baseline,
                    paint
                )
            }
        }

        // 绘制光标：在下一个待输入位水平居中、垂直居中，透明度无限循环闪烁
        if (text.length < count) {
            val cursorX = (boxWidthPx + spacePx) * text.length + boxWidthPx / 2
            drawLine(
                color = cursorLineColor,
                start = Offset(cursorX, (boxHeightPx - cursorLineHeightPx) / 2),
                end = Offset(cursorX, (boxHeightPx + cursorLineHeightPx) / 2),
                strokeWidth = cursorLineWidthPx,
                alpha = cursorAlphaState
            )
        }

    }
}

/**
 * 验证码格子样式
 */
sealed interface SkyVerifyType {

    /** 下划线样式：每个格子底部一条横线 */
    data object BottomLine : SkyVerifyType

    /** 正方形样式：每个格子一个矩形边框 */
    data object Square : SkyVerifyType
}

/**
 * 验证码输入节点：通过 [PlatformTextInputModifierNode] 建立与平台 IME 的输入会话。
 *
 * [launchSession] 每次调用都会取消旧会话并重建输入连接（会话协程随节点 detach 自动取消），
 * 用于「获得焦点」与「点击重新唤起键盘」两个时机。
 */
private class VerifyCodeInputNode(
    private val textProvider: () -> String,
    private val keyboardTypeProvider: () -> KeyboardType,
    private val onCommitText: (String) -> Unit,
    private val onDeleteText: (Int) -> Unit,
    private val onImeDone: () -> Unit,
) : Modifier.Node(), PlatformTextInputModifierNode {

    private var sessionJob: Job? = null

    /** 启动（或重启）输入会话；startInputMethod 建立连接后会自动唤起键盘，无需手动 show */
    fun launchSession() {
        if (!isAttached) return
        sessionJob?.cancel()
        sessionJob = coroutineScope.launch {
            establishTextInputSession {
                val sessionView = view
                startInputMethod { outAttributes ->
                    outAttributes.inputType = keyboardTypeProvider().toEditorInputType()
                    outAttributes.imeOptions =
                        EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN
                    // 光标恒定在文本末尾，以当前文本初始化 IME 文本模型
                    outAttributes.initialSelStart = textProvider().length
                    outAttributes.initialSelEnd = textProvider().length
                    VerifyCodeInputConnection(
                        targetView = sessionView,
                        textProvider = textProvider,
                        onCommitText = onCommitText,
                        onDeleteText = onDeleteText,
                        onImeDone = onImeDone,
                    )
                }
            }
        }
    }
}

/** 持有并挂接 [VerifyCodeInputNode] 的 Modifier 元素 */
@SuppressLint("ModifierNodeInspectableProperties")
private class VerifyCodeInputElement(
    private val node: VerifyCodeInputNode,
) : ModifierNodeElement<VerifyCodeInputNode>() {
    override fun create(): VerifyCodeInputNode = node
    override fun update(node: VerifyCodeInputNode) {}
    override fun equals(other: Any?): Boolean =
        other is VerifyCodeInputElement && other.node === node
    override fun hashCode(): Int = node.hashCode()
}

/**
 * 验证码输入法的 [InputConnection]：把 IME 回调直接映射为「追加数字 / 删除末位 / Done」。
 *
 * 注意：
 * - [getTextBeforeCursor] 必须返回组件当前的实时文本，否则部分 IME 会认为"无内容可删"，
 *   收起键盘再次唤起后删除键不再下发任何事件；
 * - 华为等机型的系统安全键盘（[KeyboardType.NumberPassword] 触发）不遵循 InputConnection 协议，
 *   删除回调不会到达，属平台行为无法从组件侧兼容。
 */
private class VerifyCodeInputConnection(
    targetView: View,
    private val textProvider: () -> String,
    private val onCommitText: (String) -> Unit,
    private val onDeleteText: (Int) -> Unit,
    private val onImeDone: () -> Unit,
) : BaseInputConnection(targetView, false) {

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        Log.d(TAG, "commitText: $text")
        if (!text.isNullOrEmpty()) onCommitText(text.toString())
        return true
    }

    // 软键盘删除键：多数 IME 走 deleteSurroundingText 编辑命令，不发送按键事件
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        Log.d(TAG, "deleteSurroundingText: $beforeLength")
        if (beforeLength > 0) onDeleteText(beforeLength)
        return true
    }

    // 部分 IME / 硬件键盘的删除键走按键事件通道，与编辑命令通道互补
    override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (event.action == ACTION_DOWN && event.keyCode == KEYCODE_DEL) {
            Log.d(TAG, "sendKeyEvent: DEL")
            onDeleteText(1)
            return true
        }
        return super.sendKeyEvent(event)
    }

    override fun performEditorAction(actionId: Int): Boolean {
        Log.d(TAG, "performEditorAction: $actionId")
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            onImeDone()
            return true
        }
        return super.performEditorAction(actionId)
    }

    // 返回实时文本，保持 IME 侧文本模型与组件一致（光标恒在末尾）
    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = textProvider().takeLast(n)
}

/** [KeyboardType] 到 [EditorInfo.inputType] 的映射（对齐 Compose 旧版默认映射） */
private fun KeyboardType.toEditorInputType(): Int {
    val type = this
    return when {
        type == KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
        type == KeyboardType.Decimal ->
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        type == KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
        type == KeyboardType.NumberPassword ->
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        type == KeyboardType.Password ->
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        type == KeyboardType.Uri ->
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        type == KeyboardType.Email ->
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        else -> InputType.TYPE_CLASS_TEXT
    }
}