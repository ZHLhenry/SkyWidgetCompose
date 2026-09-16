package com.sky.widget.samplecp

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sky.mvi.core.compose.SkyMviScreen
import com.sky.widget.samplecp.signature.SignatureUiIntent
import com.sky.widget.samplecp.signature.SignatureViewModel
import com.sky.widget.samplecp.ui.theme.SkyWidgetComposeTheme
import com.sky.widget.signatureView.SkySignatureView
import com.sky.widget.signatureView.rememberSkySignatureViewState

/**
 * 签名板示例页（SkyMVI 改造版）。
 *
 * "是否有笔迹"由 [SignatureViewModel] 持有（通过 [SignatureUiIntent] 同步），
 * 笔迹本身与保存后的 Bitmap 预览属于瞬时 UI 数据，留在 UI 层由 SkySignatureViewState 管理。
 *
 * 演示：
 * - 手写签名
 * - 清空笔迹
 * - 保存为 Bitmap 并预览
 * - 切换笔触颜色
 */
@Composable
fun SignatureDemoScreen(onBack: () -> Unit) {
    SkyMviScreen(
        viewModel = hiltViewModel<SignatureViewModel>(),
    ) { _, intent ->
        SignatureDemoContent(intent = intent, onBack = onBack)
    }
}

@Composable
private fun SignatureDemoContent(
    intent: (SignatureUiIntent) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberSkySignatureViewState()
    var savedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val colors = listOf(
        "黑色" to Color.Black,
        "蓝色" to Color(0xFF2196F3),
        "红色" to Color(0xFFE53935)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = "签名板示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 颜色选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEachIndexed { index, (name, color) ->
                    Button(
                        onClick = { selectedColorIndex = index },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = if (selectedColorIndex == index) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 签名板
            SkySignatureView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = state,
                color = colors[selectedColorIndex].second,
                strokeWidth = 4.dp,
                backgroundColor = Color.White
            )

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        state.clear()
                        savedBitmap = null
                        intent(SignatureUiIntent.Clear)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isEmpty
                ) {
                    Text("清空")
                }
                Button(
                    onClick = {
                        savedBitmap = state.save(backgroundColor = Color.White)
                        intent(SignatureUiIntent.Saved(savedBitmap?.toString()))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isEmpty
                ) {
                    Text("保存并预览")
                }
            }

            // 保存结果预览
            if (savedBitmap != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "保存结果",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = savedBitmap!!.asImageBitmap(),
                        contentDescription = "签名预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.LightGray)
                    )
                }
            } else {
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SignatureDemoScreenPreview() {
    SkyWidgetComposeTheme {
        SignatureDemoScreen(onBack = {})
    }
}
