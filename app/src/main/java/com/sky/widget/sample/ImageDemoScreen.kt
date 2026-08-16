package com.sky.widget.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.widget.image.SkyPercentBasics
import com.sky.widget.image.SkyPercentImage
import com.sky.widget.sample.ui.theme.SkyWidgetComposeTheme

/**
 * 按比例图片（SkyPercentImage）示例页。
 *
 * 演示：
 * - 以宽度为基准（Width）：不同 percent 下高度按宽度比例计算
 * - 以高度为基准（Height）：不同 percent 下宽度按高度比例计算
 * - 网络图片源：painter 支持任意 Painter（含 Coil 等网络库返回的 Painter）
 */
@Composable
fun ImageDemoScreen(onBack: () -> Unit) {
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
                text = "按比例图片示例",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 示例 1：以宽度为基准
            DemoCard(title = "以宽度为基准（basics = Width，高度 = 宽度 × percent）") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PercentSampleBox(
                            resId = R.drawable.ic_sample_blue,
                            basics = SkyPercentBasics.Width,
                            percent = 0.5f,
                            modifier = Modifier.weight(1f)
                        )
                        PercentSampleBox(
                            resId = R.drawable.ic_sample_green,
                            basics = SkyPercentBasics.Width,
                            percent = 1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PercentSampleBox(
                            resId = R.drawable.ic_sample_orange,
                            basics = SkyPercentBasics.Width,
                            percent = 0.25f,
                            modifier = Modifier.weight(1f)
                        )
                        PercentSampleBox(
                            resId = R.drawable.ic_sample_purple,
                            basics = SkyPercentBasics.Width,
                            percent = 1.5f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 示例 2：以高度为基准
            DemoCard(title = "以高度为基准（basics = Height，宽度 = 高度 × percent）") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PercentSampleBox(
                        resId = R.drawable.ic_sample_blue,
                        basics = SkyPercentBasics.Height,
                        percent = 0.5f,
                        modifier = Modifier.weight(1f)
                    )
                    PercentSampleBox(
                        resId = R.drawable.ic_sample_green,
                        basics = SkyPercentBasics.Height,
                        percent = 1f,
                        modifier = Modifier.weight(1f)
                    )
                    PercentSampleBox(
                        resId = R.drawable.ic_sample_orange,
                        basics = SkyPercentBasics.Height,
                        percent = 1.5f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 对照：SkyPercentImage(basics = Height) + modifier.fillMaxWidth
                // 父容器固定高度时，宽度 = 父高度 × percent
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "对照：SkyPercentImage(basics = Height) + fillMaxWidth，父高固定 80dp",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkyPercentImage(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.ic_sample_green),
                        basics = SkyPercentBasics.Height,
                        percent = 1f
                    )
                    SkyPercentImage(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(R.drawable.ic_sample_orange),
                        basics = SkyPercentBasics.Height,
                        percent = 0.6f
                    )
                }
            }

            // 示例 3：网络图片源说明
            DemoCard(title = "网络图片源（支持任意 Painter）") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SkyPercentImage 的 painter 支持任意 Painter，包括网络图片加载库（如 Coil）返回的 Painter。",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = buildString {
                            appendLine("// Coil 示例")
                            appendLine("val painter = rememberAsyncImagePainter(\"https://example.com/image.jpg\")")
                            appendLine("SkyPercentImage(")
                            appendLine("    modifier = Modifier.fillMaxWidth(),")
                            appendLine("    painter = painter,")
                            appendLine("    basics = SkyPercentBasics.Width,")
                            appendLine("    percent = 0.5f")
                            appendLine(")")
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF444444),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单个比例示例块：用真实图片资源演示比例效果。
 */
@Composable
private fun PercentSampleBox(
    resId: Int,
    basics: SkyPercentBasics,
    percent: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SkyPercentImage(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(resId),
            basics = basics,
            percent = percent
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "percent=${percent}",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ImageDemoScreenPreview() {
    SkyWidgetComposeTheme {
        ImageDemoScreen(onBack = {})
    }
}
