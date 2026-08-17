package com.sky.widget.qrcode

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.sky.widget.R
import com.sky.widget.qrcode.internal.DecodeFormatResolver
import com.sky.widget.qrcode.internal.SkyQRCodeAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 二维码扫描器。
 *
 * 基于 CameraX 与 ZXing 实现的声明式扫码组件。权限由消费者在外层声明并申请，
 * 本组件仅做权限检查；未授权时抛出 [SecurityException] 提示消费者处理。
 *
 * 扫描结果通过 [onResult] 回调返回。默认持续扫描：连续相同结果不重复上报，
 * 切换为不同内容的码立即上报；业务层可调用 [SkyQRCodeState.pause] 暂停、[SkyQRCodeState.resume] 继续。
 *
 * @param modifier 外部修饰符
 * @param state 扫码器状态，用于控制扫描模式、闪光灯、取景框尺寸等
 * @param onResult 识别结果回调
 * @param onError 相机初始化或绑定失败回调
 * @param viewfinder 取景框渲染器（BoxScope 接收者，可使用 align 等布局能力），默认 [SkyQRCodeViewfinder]
 * @param overlay 覆盖层渲染器（BoxScope 接收者），可用于放置标题、返回按钮、闪光灯开关等自定义 UI
 */
@Composable
fun SkyQRCodeScanner(
    modifier: Modifier = Modifier,
    state: SkyQRCodeState = rememberSkyQRCodeState(),
    onResult: (SkyQRCodeResult) -> Unit,
    onError: (Exception) -> Unit = {},
    viewfinder: @Composable BoxScope.(SkyQRCodeState) -> Unit = { SkyQRCodeViewfinder(state = it, modifier = Modifier.fillMaxSize()) },
    overlay: @Composable BoxScope.(SkyQRCodeState) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasPermission) {
        throw SecurityException(
            "Camera permission is required. " +
                "Please declare <uses-permission android:name=\"android.permission.CAMERA\"/> , <uses-permission android:name=\"android.permission.VIBRATE\"/>" +
                "in your AndroidManifest.xml and request it at runtime before using SkyQRCodeScanner."
        )
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    // 将生命周期前台状态桥接到 Compose snapshot：息屏/退后台时系统会释放相机并重置 torch，
    // 回前台后需要重新应用 flashEnabled，isResumed 作为 LaunchedEffect key 触发重新收集
    var isResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // 解码为 CPU 密集型任务，必须在后台线程执行，避免阻塞主线程导致 UI 与触摸输入卡顿
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }
    // 识别区域与视觉取景框保持一致（"所见即所扫"）：
    // 将 frameSize/frameMarginTop 换算为容器坐标像素矩形，分析器再按 FILL_CENTER 规则反映射到相机帧坐标
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    state.frameBounds = if (containerSize != IntSize.Zero) {
        val rect = computeFrameRect(
            containerSize.width.toFloat(),
            containerSize.height.toFloat(),
            state.frameSize,
            state.frameMarginTop,
            density
        )
        SkyQRCodeState.FrameBounds(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            containerWidth = containerSize.width,
            containerHeight = containerSize.height
        )
    } else {
        SkyQRCodeState.FrameBounds()
    }

    Box(modifier = modifier.onGloballyPositioned { containerSize = it.size }) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 为取景框与覆盖层提供全屏约束，避免自定义 viewfinder / overlay 未 fillMaxSize 时尺寸为 0
        Box(modifier = Modifier.fillMaxSize()) { viewfinder(state) }
        Box(modifier = Modifier.fillMaxSize()) { overlay(state) }

        LaunchedEffect(lifecycleOwner, state.mode) {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            // 去重状态：连续相同结果不重复上报；码离开视野（连续空帧）后重置，同码再次入镜可重新上报
            var lastReportedText: String? = null
            var emptyFrames = 0
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            try {
                val cameraProvider = cameraProviderFuture.await(mainExecutor)
                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            analysisExecutor,
                            SkyQRCodeAnalyzer(
                                formats = DecodeFormatResolver.formatsFor(state.mode),
                                frameBounds = { state.frameBounds },
                                onResult = { result ->
                                    // 解码在后台线程，回调切回主线程，保证消费者可安全执行 UI 操作
                                    mainExecutor.execute {
                                        if (!state.isScanning) return@execute
                                        emptyFrames = 0
                                        // 去重：切换到不同内容的码立即上报，保证换码可连续识别
                                        if (result.text != lastReportedText) {
                                            lastReportedText = result.text
                                            playBeepAndVibrate(context, state)
                                            onResult(
                                                SkyQRCodeResult.Success(
                                                    text = result.text,
                                                    barcode = null,
                                                    format = result.barcodeFormat?.name
                                                )
                                            )
                                        }
                                    }
                                },
                                onEmpty = {
                                    mainExecutor.execute {
                                        emptyFrames++
                                        if (emptyFrames >= EMPTY_FRAMES_TO_RESET_DEDUP) {
                                            lastReportedText = null
                                        }
                                    }
                                }
                            )
                        )
                    }

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                // 保持绑定，避免 LaunchedEffect 直接结束触发 finally unbind
                awaitCancellation()
            } catch (e: CancellationException) {
                // 协程取消（组合离开、key 变化等）属于正常生命周期行为，透明上抛，避免误报 onError
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                onError(e)
            } finally {
                camera = null
                try {
                    cameraProviderFuture.await(mainExecutor).unbindAll()
                } catch (e: Exception) {
                    Log.e(TAG, "Camera unbind failed", e)
                }
            }
        }

        // 通过 snapshotFlow 监听闪光灯状态，不依赖重组即可响应状态写入；
        // isResumed 作为 key：息屏回前台后重新收集并立即发射当前值，重新应用 torch
        // （息屏时系统释放相机并重置 torch，而 camera 实例不变不会触发重新应用）
        LaunchedEffect(camera, isResumed) {
            val currentCamera = camera ?: return@LaunchedEffect
            if (!isResumed) return@LaunchedEffect
            snapshotFlow { state.flashEnabled }.collect { enabled ->
                setTorchSafely(currentCamera, enabled)
            }
        }
    }
}

private fun setTorchSafely(camera: Camera?, enabled: Boolean) {
    Log.d(TAG, "setTorch: enabled=$enabled, cameraBound=${camera != null}")
    camera?.cameraControl?.let { control ->
        try {
            control.enableTorch(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Set torch failed: enabled=$enabled", e)
        }
    }
}

private suspend fun <T> ListenableFuture<T>.await(executor: Executor): T {
    return if (isDone) {
        try {
            get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    } else {
        suspendCancellableCoroutine { cont ->
            addListener(
                {
                    try {
                        cont.resume(get())
                    } catch (e: ExecutionException) {
                        cont.resumeWithException(e.cause ?: e)
                    } catch (e: Throwable) {
                        cont.resumeWithException(e)
                    }
                },
                executor
            )
        }
    }
}

@SuppressLint("MissingPermission") // VIBRATE 为普通权限，由消费者按需在 Manifest 声明
private fun playBeepAndVibrate(context: Context, state: SkyQRCodeState) {
    if (state.beepEnabled) {
        // 优先使用消费者自定义提示音；未设置（0）或资源无效时回退库内置提示音，
        // 避免依赖系统通知音（部分 ROM 上系统通知音静音或不可用）
        val player = if (state.beepResId != 0) {
            MediaPlayer.create(context, state.beepResId) ?: MediaPlayer.create(context, R.raw.sky_qrcode_beep)
        } else {
            MediaPlayer.create(context, R.raw.sky_qrcode_beep)
        }
        player?.apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
    }
    if (state.vibrateEnabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200L)
            }
        }
    }
}

private const val TAG = "SkyQRCodeScanner"

/** 连续未识别帧数达到该阈值后认为码已离开视野，重置去重状态。 */
private const val EMPTY_FRAMES_TO_RESET_DEDUP = 10
